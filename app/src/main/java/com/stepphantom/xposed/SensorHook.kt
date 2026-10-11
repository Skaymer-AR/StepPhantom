package com.stepphantom.xposed

import android.app.AndroidAppHelper
import android.content.ContentValues
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.stepphantom.config.ConfigProvider
import com.stepphantom.config.DiagnosticsProvider
import com.stepphantom.config.HookConfigSource
import com.stepphantom.config.PackageConfig
import com.stepphantom.config.StepPhantomConfig
import com.stepphantom.engine.StepTransformationEngine
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Entry point del módulo (API clásica Xposed 82, compatible con LSPosed y Vector).
 * Declarado en assets/xposed_init como com.stepphantom.xposed.SensorHook.
 *
 * Capa 3, Ruta A (SensorManager). Las rutas de Health Connect NO se reescriben en
 * esta versión: RouteDetector sólo informa qué vía usa cada app.
 */
class SensorHook : IXposedHookLoadPackage {

    private val TAG = "[StepPhantom]"
    private val KILL_SWITCH = "/data/local/tmp/stepphantom_disable"
    private val SUPPORTED = setOf(
        Sensor.TYPE_STEP_COUNTER, Sensor.TYPE_STEP_DETECTOR,
        Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE
    )

    private val engine = StepTransformationEngine()
    private val registry = ListenerRegistry()

    @Volatile private var cachedConfig = StepPhantomConfig()
    @Volatile private var lastLoadMs = 0L
    @Volatile private var loadedOnce = false
    private val CONFIG_TTL_MS = 3000L

    @Volatile private var route: RouteDetector.Result? = null
    @Volatile private var lastDiagPushMs = 0L
    private val DIAG_TTL_MS = 2000L
    @Volatile private var lastSensorLabel = "-"
    @Volatile private var lastReal = -1L
    @Volatile private var lastFake = -1L
    @Volatile private var lastError = ""
    @Volatile private var simulationStatus = "sin eventos sintéticos entregados"

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // Kill switch de emergencia por archivo.
        if (killSwitchPresent()) {
            XposedBridge.log("$TAG Desactivado por kill switch ($KILL_SWITCH) en ${lpparam.packageName}")
            return
        }
        try {
            route = RouteDetector.detect(lpparam.classLoader)
            XposedBridge.log("$TAG ${lpparam.packageName} -> ${route?.primaryRouteLabel()}")
            installSensorHooks(lpparam)
            // Empujar diagnóstico inicial (best-effort; puede no haber Context aún).
            runCatching { pushDiag(extractContext(null), lpparam.packageName, force = true) }
        } catch (t: Throwable) {
            lastError = t.toString()
            XposedBridge.log("$TAG Fallo instalando hooks en ${lpparam.packageName}: $t")
        }
    }

    private fun killSwitchPresent(): Boolean = runCatching { File(KILL_SWITCH).exists() }.getOrDefault(false)

    // ------------------------------------------------------------------
    // Hooks de SensorManager
    // ------------------------------------------------------------------

    private fun installSensorHooks(lpparam: LoadPackageParam) {
        val cls = "android.hardware.SensorManager"
        val cl = lpparam.classLoader
        val I = Integer.TYPE

        val onRegister = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = beforeRegister(param, lpparam)
            override fun afterHookedMethod(param: MethodHookParam) = afterRegister(param)
        }
        val onUnregister = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = beforeUnregister(param)
            override fun afterHookedMethod(param: MethodHookParam) = afterUnregister(param)
        }

        hook(cls, cl, "registerListener", onRegister,
            SensorEventListener::class.java, Sensor::class.java, I)
        hook(cls, cl, "registerListener", onRegister,
            SensorEventListener::class.java, Sensor::class.java, I, Handler::class.java)
        hook(cls, cl, "registerListener", onRegister,
            SensorEventListener::class.java, Sensor::class.java, I, I)
        hook(cls, cl, "registerListener", onRegister,
            SensorEventListener::class.java, Sensor::class.java, I, I, Handler::class.java)

        hook(cls, cl, "unregisterListener", onUnregister, SensorEventListener::class.java)
        hook(cls, cl, "unregisterListener", onUnregister,
            SensorEventListener::class.java, Sensor::class.java)

        XposedBridge.log("$TAG Hooks de SensorManager instalados en ${lpparam.packageName}")
    }

    private fun hook(
        className: String, cl: ClassLoader, method: String,
        callback: XC_MethodHook, vararg paramTypes: Any
    ) {
        try {
            val args = arrayOfNulls<Any>(paramTypes.size + 1)
            System.arraycopy(paramTypes, 0, args, 0, paramTypes.size)
            args[paramTypes.size] = callback
            XposedHelpers.findAndHookMethod(className, cl, method, *args)
        } catch (e: NoSuchMethodError) {
            XposedBridge.log("$TAG Overload ausente ($method/${paramTypes.size}); se ignora")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG Error hookeando $method: $t")
        }
    }

    // ------------------------------------------------------------------
    // Callbacks
    // ------------------------------------------------------------------

    private fun beforeRegister(param: XC_MethodHook.MethodHookParam, lpparam: LoadPackageParam) {
        try {
            val ctx = extractContext(param.thisObject)
            val cfg = currentConfig(ctx).forPackage(lpparam.packageName) ?: return
            if (!cfg.enabled || !cfg.hookSensorManager) return

            val listener = param.args.getOrNull(0) as? SensorEventListener ?: return
            if (registry.isWrapper(listener)) return
            val sensor = param.args.getOrNull(1) as? Sensor ?: return
            if (sensor.type !in SUPPORTED) return

            val handler = (param.args.firstOrNull { it is Handler } as? Handler)
                ?: runCatching { Handler(Looper.myLooper() ?: Looper.getMainLooper()) }.getOrNull()
            val pkg = lpparam.packageName
            val wrapper = registry.getOrCreate(listener) {
                SensorListenerWrapper(
                    original = listener,
                    engine = engine,
                    pkg = pkg,
                    cfgProvider = { currentConfig(extractContext(param.thisObject)).forPackage(pkg) },
                    onCounterObserved = { real, fake ->
                        lastReal = real; lastFake = fake
                        lastSensorLabel = "STEP_COUNTER"
                        simulationStatus = "callback completado sin excepción; consumo por la app no confirmado"
                        pushDiag(ctx, pkg, force = false)
                    },
                    onSimulationError = { message ->
                        simulationStatus = message
                        lastError = message
                        pushDiag(ctx, pkg, force = false)
                    }
                )
            }
            param.setObjectExtra("stepphantom.original.listener", listener)
            param.setObjectExtra("stepphantom.sensor", sensor)
            param.setObjectExtra("stepphantom.handler", handler)
            param.setObjectExtra("stepphantom.package", pkg)
            param.args[0] = wrapper
            if (sensor.type == Sensor.TYPE_STEP_COUNTER) lastSensorLabel = "STEP_COUNTER"
            if (sensor.type == Sensor.TYPE_STEP_DETECTOR) lastSensorLabel = "STEP_DETECTOR"
            XposedBridge.log("$TAG Envuelto $pkg sensor=${sensorName(sensor.type)}")
        } catch (t: Throwable) {
            lastError = t.toString()
            XposedBridge.log("$TAG beforeRegister error: $t")
        }
    }

    private fun afterRegister(param: XC_MethodHook.MethodHookParam) {
        try {
            val wrapper = param.args.getOrNull(0) as? SensorListenerWrapper ?: return
            val listener = param.getObjectExtra("stepphantom.original.listener") as? SensorEventListener ?: return
            val sensor = param.getObjectExtra("stepphantom.sensor") as? Sensor ?: return
            val pkg = param.getObjectExtra("stepphantom.package") as? String ?: return
            if (param.hasThrowable() || param.result != true) {
                wrapper.onUnregistered(sensor.type)
                if (wrapper.isEmpty) registry.remove(listener)
                simulationStatus = "Android rechazó el registro de ${sensorName(sensor.type)}"
                lastError = simulationStatus
                pushDiag(extractContext(param.thisObject), pkg, force = false)
                return
            }
            val handler = param.getObjectExtra("stepphantom.handler") as? Handler
            wrapper.onRegistered(sensor, handler)
            simulationStatus = "esperando el primer evento sintético"
            pushDiag(extractContext(param.thisObject), pkg, force = false)
        } catch (t: Throwable) {
            XposedBridge.log("$TAG afterRegister error: $t")
        }
    }

    private fun beforeUnregister(param: XC_MethodHook.MethodHookParam) {
        try {
            val listener = param.args.getOrNull(0) as? SensorEventListener ?: return
            if (registry.isWrapper(listener)) return
            val wrapper = registry.get(listener) ?: return
            param.setObjectExtra("stepphantom.original.listener", listener)
            param.args[0] = wrapper
        } catch (t: Throwable) {
            XposedBridge.log("$TAG beforeUnregister error: $t")
        }
    }

    private fun afterUnregister(param: XC_MethodHook.MethodHookParam) {
        try {
            if (param.hasThrowable()) return
            val listener = param.getObjectExtra("stepphantom.original.listener") as? SensorEventListener ?: return
            val wrapper = registry.get(listener) ?: return
            val sensor = param.args.getOrNull(1) as? Sensor
            if (sensor == null) {
                wrapper.stopAll()
                registry.remove(listener)
            } else {
                wrapper.onUnregistered(sensor.type)
                if (wrapper.isEmpty) {
                    wrapper.stopAll()
                    registry.remove(listener)
                }
            }
        } catch (t: Throwable) {
            XposedBridge.log("$TAG afterUnregister error: $t")
        }
    }

    // ------------------------------------------------------------------
    // Config / contexto
    // ------------------------------------------------------------------

    @Synchronized
    private fun currentConfig(ctx: Context?): StepPhantomConfig {
        val now = SystemClock.elapsedRealtime()
        if (loadedOnce && now - lastLoadMs < CONFIG_TTL_MS) return cachedConfig
        cachedConfig = HookConfigSource.load(ctx)
        lastLoadMs = now
        loadedOnce = true
        return cachedConfig
    }

    private fun extractContext(sm: Any?): Context? {
        if (sm != null) {
            runCatching { XposedHelpers.getObjectField(sm, "mContext") as? Context }
                .getOrNull()?.let { return it }
        }
        return runCatching { AndroidAppHelper.currentApplication() as? Context }.getOrNull()
    }

    // ------------------------------------------------------------------
    // Diagnóstico (canal inverso hacia la app)
    // ------------------------------------------------------------------

    private fun pushDiag(ctx: Context?, pkg: String, force: Boolean) {
        val context = ctx ?: return
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastDiagPushMs < DIAG_TTL_MS) return
        lastDiagPushMs = now
        try {
            val r = route
            val json = JSONObject().apply {
                put("package", pkg)
                put("uid", android.os.Process.myUid())
                put("process", runCatching { AndroidAppHelper.currentProcessName() }.getOrDefault("-"))
                put("sdkInt", Build.VERSION.SDK_INT)
                put("sdkExtensions", sdkExtensions())
                put("route", when {
                    lastSensorLabel != "-" -> "SensorManager: $lastSensorLabel solicitado por la app; Health Connect disponible no confirma uso"
                    else -> r?.primaryRouteLabel() ?: "desconocida"
                })
                put("sensorRequested", lastSensorLabel != "-")
                put("simulationStatus", simulationStatus)
                put("hcJetpack", r?.hcJetpack ?: false)
                put("hcFramework", r?.hcFramework ?: false)
                put("fitOrRecording", r?.fitOrRecording ?: false)
                put("classesFound", JSONArray(r?.classesFound ?: emptyList<String>()))
                put("lastSensor", lastSensorLabel)
                put("realValue", lastReal)
                put("fakeValue", lastFake)
                put("lastError", lastError)
                put("timestamp", System.currentTimeMillis())
            }.toString()

            val values = ContentValues().apply {
                put(DiagnosticsProvider.COL_PACKAGE, pkg)
                put(DiagnosticsProvider.COL_JSON, json)
            }
            context.contentResolver.insert(DiagnosticsProvider.URI, values)
        } catch (t: Throwable) {
            XposedBridge.log("$TAG No se pudo empujar diagnóstico: $t")
        }
    }

    private fun sdkExtensions(): String {
        if (Build.VERSION.SDK_INT < 30) return "n/a"
        return try {
            val sb = StringBuilder()
            val codes = intArrayOf(30, 31, 33, 34) // R, S, T, U
            for (c in codes) {
                val v = runCatching { android.os.ext.SdkExtensions.getExtensionVersion(c) }.getOrDefault(-1)
                if (v >= 0) sb.append("$c=$v ")
            }
            sb.toString().trim().ifEmpty { "0" }
        } catch (_: Throwable) { "n/a" }
    }

    private fun sensorName(type: Int): String = when (type) {
        Sensor.TYPE_STEP_COUNTER -> "STEP_COUNTER"
        Sensor.TYPE_STEP_DETECTOR -> "STEP_DETECTOR"
        Sensor.TYPE_ACCELEROMETER -> "ACCELEROMETER"
        Sensor.TYPE_GYROSCOPE -> "GYROSCOPE"
        else -> "TYPE_$type"
    }
}
