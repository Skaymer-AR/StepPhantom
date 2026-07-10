package com.stepphantom.xposed

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import com.stepphantom.config.PackageConfig
import com.stepphantom.config.TransformMode
import com.stepphantom.engine.StepTransformationEngine
import de.robv.android.xposed.XposedBridge
import java.util.concurrent.ConcurrentHashMap

class SensorListenerWrapper(
    private val original: SensorEventListener,
    private val engine: StepTransformationEngine,
    private val pkg: String,
    private val cfgProvider: () -> PackageConfig?,
    private val onCounterObserved: (real: Long, fake: Long) -> Unit
) : SensorEventListener {
    private val liveSensors=ConcurrentHashMap.newKeySet<Int>()
    private val handlersByType=ConcurrentHashMap<Int,Handler>()
    private val synthActive=ConcurrentHashMap.newKeySet<Int>()
    private val seedEvents=ConcurrentHashMap<Int,SensorEvent>()
    private val thread:HandlerThread by lazy{HandlerThread("StepPhantom-sim").also{it.start()}}
    private val scheduler:Handler by lazy{Handler(thread.looper)}
    val isEmpty:Boolean get()=liveSensors.isEmpty()
    fun onRegistered(sensor:Sensor,handler:Handler?){val type=sensor.type;liveSensors.add(type);if(handler!=null)handlersByType[type]=handler;if(type==Sensor.TYPE_STEP_COUNTER||type==Sensor.TYPE_STEP_DETECTOR)startSynthetic(sensor)}
    fun onUnregistered(type:Int){liveSensors.remove(type);synthActive.remove(type);handlersByType.remove(type)}
    fun stopAll(){liveSensors.clear();synthActive.clear();handlersByType.clear();runCatching{scheduler.removeCallbacksAndMessages(null)};runCatching{thread.quitSafely()}}
    override fun onSensorChanged(event:SensorEvent){try{val cfg=cfgProvider();if(cfg==null||!cfg.enabled||!cfg.hookSensorManager){original.onSensorChanged(event);return};when(event.sensor.type){Sensor.TYPE_STEP_COUNTER->{seedEvents.putIfAbsent(Sensor.TYPE_STEP_COUNTER,event);if(cfg.mode==TransformMode.ORIGINAL)original.onSensorChanged(event)else{val real=event.values[0].toLong();val fake=engine.transformCounter(pkg,cfg,real);event.values[0]=fake.toFloat();onCounterObserved(real,fake);original.onSensorChanged(event)}};Sensor.TYPE_STEP_DETECTOR->{seedEvents.putIfAbsent(Sensor.TYPE_STEP_DETECTOR,event);original.onSensorChanged(event)};Sensor.TYPE_ACCELEROMETER->{if(cfg.simAccelerometer)engine.fillAccelerometer(pkg,cfg,event.values);original.onSensorChanged(event)};Sensor.TYPE_GYROSCOPE->{if(cfg.simGyroscope)engine.fillGyroscope(pkg,cfg,event.values);original.onSensorChanged(event)};else->original.onSensorChanged(event)}}catch(t:Throwable){XposedBridge.log("[StepPhantom] onSensorChanged error ($pkg): $t");runCatching{original.onSensorChanged(event)}}}
    override fun onAccuracyChanged(sensor:Sensor,accuracy:Int){runCatching{original.onAccuracyChanged(sensor,accuracy)}}
    private fun startSynthetic(sensor:Sensor){if(!synthActive.add(sensor.type))return;scheduleNext(sensor)}
    private fun scheduleNext(sensor:Sensor){val type=sensor.type;if(!synthActive.contains(type))return;val cfg=cfgProvider();val delay=when(type){Sensor.TYPE_STEP_COUNTER->1000L;Sensor.TYPE_STEP_DETECTOR->if(cfg!=null)engine.detectorIntervalMs(cfg)else 1000L;else->return}.coerceIn(50L,60_000L);scheduler.postDelayed({deliverSynthetic(sensor);scheduleNext(sensor)},delay)}
    private fun deliverSynthetic(sensor:Sensor){try{val cfg=cfgProvider()?:return;if(!cfg.enabled||!cfg.hookSensorManager)return;when(sensor.type){Sensor.TYPE_STEP_COUNTER->{if(!engine.needsScheduler(cfg))return;val fake=engine.syntheticCounter(pkg,cfg);pushEvent(sensor,fake.toFloat());onCounterObserved(-1L,fake)};Sensor.TYPE_STEP_DETECTOR->{if(cfg.mode!=TransformMode.RITMO_SIMULADO||cfg.paused)return;pushEvent(sensor,1.0f)}}}catch(t:Throwable){XposedBridge.log("[StepPhantom] deliverSynthetic error ($pkg): $t")}}
    private fun pushEvent(sensor:Sensor,value0:Float){val ev=buildEvent(sensor,value0)?:return;val h=handlersByType[sensor.type];if(h!=null)h.post{safeDeliver(ev)}else safeDeliver(ev)}
    private fun safeDeliver(ev:SensorEvent){runCatching{original.onSensorChanged(ev)}.onFailure{XposedBridge.log("[StepPhantom] listener lanzó ($pkg): $it")}}
    private fun buildEvent(sensor:Sensor,value0:Float):SensorEvent?{SensorEventFactory.tryCreate(1)?.let{fresh->fresh.values[0]=value0;fresh.sensor=sensor;fresh.accuracy=SensorManager.SENSOR_STATUS_ACCURACY_HIGH;fresh.timestamp=SystemClock.elapsedRealtimeNanos();return fresh};seedEvents[sensor.type]?.let{seed->return runCatching{seed.values[0]=value0;seed.timestamp=SystemClock.elapsedRealtimeNanos();seed}.getOrNull()};return null}
}
