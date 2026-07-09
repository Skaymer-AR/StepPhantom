package com.stepphantom.xposed

import android.app.AndroidAppHelper
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.os.Handler
import android.os.SystemClock
import com.stepphantom.config.HookConfigSource
import com.stepphantom.config.ModuleConfig
import com.stepphantom.engine.StepSimulationEngine
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.util.concurrent.ConcurrentHashMap

/**
 * Punto de entrada del módulo (API clásica de Xposed, compatible con LSPosed
 * y con Vector — ver README para el porqué de esta elección de API).
 *
 * Declarado en assets/xposed_init como:
 *     com.stepphantom.xposed.SensorHook
 *
 * Instala los hooks SIEMPRE que el módulo se carga en un proceso (el filtrado
 * fino por paquete lo hacemos en tiempo de ejecución, cuando ya tenemos Context;
 * en handleLoadPackage todavía no lo tenemos con garantías). El scope grueso lo
 * define LSPosed/Vector (qué apps inyectar); el scope fino lo define la config.
 */
class SensorHook : IXposedHookLoadPackage {

    private val TAG = "[StepPhantom]"
    private val SUPPORTED = setOf(
        Sensor.TYPE_STEP_COUNTER,
        Sensor.TYPE_STEP_DETECTOR,
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GYROSCOPE
    )

    // Mapa listener original -> wrapper, por proceso.
    private val wrappers = ConcurrentHashMap<SensorEventListener, SensorListenerWrapper>()

    // Config cacheada con TTL para no consultar el provider en cada registro.
    @Volatile private var cachedConfig = ModuleConfig()
    @Volatile private var lastLoadMs = 0L
    @Volatile private var loadedOnce = false
    private val CONFIG_TTL_MS = 3000L

    // Un motor compartido por proceso (todos los listeners ven el mismo contador).
    @Volatile private var engine: StepSimulationEngine? = null
    @Volatile private var lastResetToken = Long.MIN_VALUE

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        try {
            installSensorHooks(lpparam)
        } catch (t: Throwable) {
            XposedBridge.log("$TAG No se pudieron instalar hooks en ${lpparam.packageName}: $t")
        }
    }

    // ---------------------------------------------------------------------
    // Instalación de hooks
    // ---------------------------------------------------------------------

    private fun installSensorHooks(lpparam: LoadPackageParam) {
        val cl = lpparam.classLoader
        val cls = "android.hardware.SensorManager"

        val onRegister = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = beforeRegister(param, lpparam)
        }
        val onUnregister = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) = beforeUnregister(param, lpparam)
        }

        val I = Int::class.javaPrimitiveType!!

        // --- registerListener overloads públicos (Android 16) ---
        hook(cls, cl, "registerListener", onRegister,
            SensorEventListener::class.java, Sensor::class.java, I)
        hook(cls, cl, "registerListener", onRegister,
            SensorEventListener::class.java, Sensor::class.java, I, Handler::class.java)
        hook(cls, cl, "registerListener", onRegister,
            SensorEventListener::class.java, Sensor::class.java, I, I)
        hook(cls, cl, "registerListener", onRegister,
            SensorEventListener::class.java, Sensor::class.java, I, I, Handler::class.java)

        // --- unregisterListener overloads públicos ---
        hook(cls, cl, "unregisterListener", onUnregister,
            SensorEventListener::class.java)
        hook(cls, cl, "unregisterListener", onUnregister,
            SensorEventListener::class.java, Sensor::class.java)

        XposedBridge.log("$TAG Hooks instalados en ${lpparam.packageName}")
    }

    /** Hookea una firma concreta; si no existe en esta ROM/versión, lo loguea y sigue. */
    private fun hook(
        className: String,
        cl: ClassLoader,
        method: String,
        callback: XC_MethodHook,
        vararg paramTypes: Any
    ) {
        try {
            val args = arrayOfNulls<Any>(paramTypes.size + 1)
            System.arraycopy(paramTypes, 0, args, 0, paramTypes.size)
            args[paramTypes.size] = callback
            XposedHelpers.findAndHookMethod(className, cl, method, *args)
        } catch (e: NoSuchMethodError) {
            XposedBridge.log("$TAG Overload ausente ($method / ${paramTypes.size} args) — se ignora")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG Error hookeando $method: $t")
        }
    }

    // ---------------------------------------------------------------------
    // Callbacks
    // ---------------------------------------------------------------------

    private fun beforeRegister(param: XC_MethodHook.MethodHookParam, lpparam: LoadPackageParam) {
        try {
            val ctx = extractContext(param.thisObject)
            val cfg = currentConfig(ctx)

            if (!cfg.moduleEnabled) return
            if (cfg.targetPackages.isNotEmpty() && lpparam.packageName !in cfg.targetPackages) return

            val listener = param.args.getOrNull(0) as? SensorEventListener ?: return
            if (listener is SensorListenerWrapper) return // ya es nuestro (delegación interna)
            val sensor = param.args.getOrNull(1) as? Sensor ?: return
            val type = sensor.type
            if (type !in SUPPORTED) return
            if (!shouldSimulate(cfg, type)) return

            val handler = param.args.firstOrNull { it is Handler } as? Handler
            val eng = engineFor(cfg)

            val wrapper = wrappers.getOrPut(listener) {
                SensorListenerWrapper(listener, eng, { currentConfig(extractContext(param.thisObject)) })
            }
            wrapper.onRegistered(sensor, handler)

            // Sustituir el listener por nuestro wrapper: el sistema registra el wrapper.
            param.args[0] = wrapper
            XposedBridge.log("$TAG Envuelto ${lpparam.packageName} sensor=${sensorName(type)}")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG beforeRegister error: $t") // no romper el registro real
        }
    }

    private fun beforeUnregister(param: XC_MethodHook.MethodHookParam, lpparam: LoadPackageParam) {
        try {
            val listener = param.args.getOrNull(0) as? SensorEventListener ?: return
            if (listener is SensorListenerWrapper) return
            val wrapper = wrappers[listener] ?: return

            // El sistema registró el wrapper, así que hay que desregistrar el wrapper.
            param.args[0] = wrapper

            val sensor = param.args.getOrNull(1) as? Sensor
            if (sensor == null) {
                // unregister total
                wrapper.stopAll()
                wrappers.remove(listener)
            } else {
                wrapper.onUnregistered(sensor.type)
                if (wrapper.isEmpty) {
                    wrapper.stopAll()
                    wrappers.remove(listener)
                }
            }
        } catch (t: Throwable) {
            XposedBridge.log("$TAG beforeUnregister error: $t")
        }
    }

    // ---------------------------------------------------------------------
    // Config / motor / contexto
    // ---------------------------------------------------------------------

    @Synchronized
    private fun currentConfig(ctx: Context?): ModuleConfig {
        val now = SystemClock.elapsedRealtime()
        if (loadedOnce && now - lastLoadMs < CONFIG_TTL_MS) return cachedConfig

        val loaded = HookConfigSource.load(ctx)
        lastLoadMs = now
        loadedOnce = true

        val eng = engine ?: StepSimulationEngine(loaded).also { engine = it }
        eng.updateConfig(loaded)
        if (loaded.resetToken != lastResetToken) {
            lastResetToken = loaded.resetToken
            eng.reset()
        }
        cachedConfig = loaded
        return loaded
    }

    private fun engineFor(cfg: ModuleConfig): StepSimulationEngine =
        engine ?: StepSimulationEngine(cfg).also { engine = it }

    /** Intenta sacar el Context del SystemSensorManager (campo mContext); si no, del Application. */
    private fun extractContext(sm: Any?): Context? {
        if (sm != null) {
            runCatching { XposedHelpers.getObjectField(sm, "mContext") as? Context }
                .getOrNull()?.let { return it }
        }
        return runCatching { AndroidAppHelper.currentApplication() as? Context }.getOrNull()
    }

    private fun shouldSimulate(cfg: ModuleConfig, type: Int): Boolean = when (type) {
        Sensor.TYPE_STEP_COUNTER -> cfg.simStepCounter
        Sensor.TYPE_STEP_DETECTOR -> cfg.simStepDetector
        Sensor.TYPE_ACCELEROMETER -> cfg.simAccelerometer
        Sensor.TYPE_GYROSCOPE -> cfg.simGyroscope
        else -> false
    }

    private fun sensorName(type: Int): String = when (type) {
        Sensor.TYPE_STEP_COUNTER -> "STEP_COUNTER"
        Sensor.TYPE_STEP_DETECTOR -> "STEP_DETECTOR"
        Sensor.TYPE_ACCELEROMETER -> "ACCELEROMETER"
        Sensor.TYPE_GYROSCOPE -> "GYROSCOPE"
        else -> "TYPE_$type"
    }
}
