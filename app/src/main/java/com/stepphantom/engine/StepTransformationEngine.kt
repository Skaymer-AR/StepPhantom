package com.stepphantom.engine

import android.os.SystemClock
import com.stepphantom.config.PackageConfig
import com.stepphantom.config.TransformMode
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong
import kotlin.random.Random

class StepTransformationEngine {
    private class Session {
        var realBaseline: Long = -1L
        var lastEmitted: Long = -1L
        var startNanos: Long = SystemClock.elapsedRealtimeNanos()
        var pausedAccumNanos: Long = 0L
        var pauseStartNanos: Long = -1L
        var seed: Long = Random.nextLong()
        var lastResetToken: Long = Long.MIN_VALUE
    }
    private val sessions = ConcurrentHashMap<String, Session>()
    private fun session(pkg: String): Session = sessions.getOrPut(pkg) { Session() }
    private fun maybeReset(s: Session, cfg: PackageConfig) {
        if (cfg.resetToken != s.lastResetToken) {
            s.lastResetToken = cfg.resetToken; s.realBaseline = -1L; s.lastEmitted = -1L
            s.startNanos = SystemClock.elapsedRealtimeNanos(); s.pausedAccumNanos = 0L; s.pauseStartNanos = -1L
        }
    }
    private fun activeNanos(s: Session, cfg: PackageConfig, now: Long): Long {
        var paused = s.pausedAccumNanos
        if (cfg.paused) { if (s.pauseStartNanos < 0) s.pauseStartNanos = now; paused += now - s.pauseStartNanos }
        else if (s.pauseStartNanos >= 0) { s.pausedAccumNanos += now - s.pauseStartNanos; s.pauseStartNanos = -1L; paused = s.pausedAccumNanos }
        return ((now - s.startNanos) - paused).coerceAtLeast(0L)
    }
    private fun applyLimitsAndJitter(cfg: PackageConfig, s: Session, value: Long): Long {
        var v = value
        if (cfg.jitterPercent > 0f) {
            val rnd = Random(s.seed xor (v * 2654435761L))
            v += (v * (cfg.jitterPercent / 100.0) * (rnd.nextDouble() - 0.5) * 2).roundToLong()
        }
        if (cfg.minLimit > 0 && v < cfg.minLimit) v = cfg.minLimit
        if (cfg.maxLimit >= 0 && v > cfg.maxLimit) v = cfg.maxLimit
        return v.coerceAtLeast(0L)
    }
    private fun enforceMonotonic(s: Session, value: Long): Long {
        var v = value.coerceAtLeast(0L); if (s.lastEmitted >= 0 && v < s.lastEmitted) v = s.lastEmitted; s.lastEmitted = v; return v
    }
    @Synchronized fun transformCounter(pkg: String, cfg: PackageConfig, realValue: Long): Long {
        val s=session(pkg); maybeReset(s,cfg); if(s.realBaseline<0)s.realBaseline=realValue
        val sessionSteps=(realValue-s.realBaseline).coerceAtLeast(0); val now=SystemClock.elapsedRealtimeNanos()
        val raw=when(cfg.mode){
            TransformMode.ORIGINAL->realValue
            TransformMode.SUMAR->safeAdd(realValue,cfg.offset)
            TransformMode.MULTIPLICAR->safeMul(realValue,cfg.multiplier)
            TransformMode.REEMPLAZAR->cfg.replaceValue
            TransformMode.RITMO_SIMULADO->rhythmValue(cfg,s,now)
            TransformMode.PERSONALIZADO->{ val ritmo=if(cfg.stepsPerMinute>0)rhythmDelta(cfg,s,now)else 0L; val base=safeAdd(cfg.initialSteps,safeMul(sessionSteps,cfg.multiplier)); applyLimitsAndJitter(cfg,s,safeAdd(safeAdd(base,cfg.offset),ritmo)) }
        }
        return enforceMonotonic(s,raw)
    }
    @Synchronized fun syntheticCounter(pkg:String,cfg:PackageConfig):Long{ val s=session(pkg); maybeReset(s,cfg); val raw=when(cfg.mode){TransformMode.REEMPLAZAR->cfg.replaceValue;TransformMode.RITMO_SIMULADO->rhythmValue(cfg,s,SystemClock.elapsedRealtimeNanos());else->s.lastEmitted.coerceAtLeast(0)};return enforceMonotonic(s,raw)}
    fun needsScheduler(cfg:PackageConfig)=cfg.mode==TransformMode.REEMPLAZAR||cfg.mode==TransformMode.RITMO_SIMULADO
    fun detectorIntervalMs(cfg:PackageConfig):Long{if(cfg.paused||cfg.stepsPerMinute<=0)return Long.MAX_VALUE/4;return(60_000.0/cfg.stepsPerMinute).toLong().coerceAtLeast(50)}
    fun fillAccelerometer(pkg:String,cfg:PackageConfig,out:FloatArray){val s=session(pkg);val now=SystemClock.elapsedRealtimeNanos();val g=9.81f;if(cfg.paused||cfg.stepsPerMinute<=0){out[0]=0f;out[1]=0f;out[2]=g;return};val freq=(cfg.stepsPerMinute/60.0)*2.0;val t=activeNanos(s,cfg,now)/1_000_000_000.0;val phase=2.0*Math.PI*freq*t;out[0]=(0.9*Math.sin(phase+0.5)).toFloat();out[1]=(0.6*Math.sin(phase/2.0)).toFloat();out[2]=g+(1.4*Math.sin(phase)).toFloat()}
    fun fillGyroscope(pkg:String,cfg:PackageConfig,out:FloatArray){val s=session(pkg);val now=SystemClock.elapsedRealtimeNanos();if(cfg.paused){out.fill(0f);return};val freq=(cfg.stepsPerMinute/60.0).coerceAtLeast(0.5);val t=activeNanos(s,cfg,now)/1_000_000_000.0;val phase=2.0*Math.PI*freq*t;out[0]=(0.04*Math.sin(phase)).toFloat();out[1]=(0.03*Math.sin(phase+1.0)).toFloat();out[2]=(0.02*Math.sin(phase+2.0)).toFloat()}
    private fun rhythmDelta(cfg:PackageConfig,s:Session,now:Long)=(activeNanos(s,cfg,now)/60_000_000_000.0*cfg.stepsPerMinute.coerceAtLeast(0)).toLong().coerceAtLeast(0)
    private fun rhythmValue(cfg:PackageConfig,s:Session,now:Long)=applyLimitsAndJitter(cfg,s,safeAdd(cfg.initialSteps,rhythmDelta(cfg,s,now)))
    private fun safeAdd(a:Long,b:Long):Long{val r=a+b;return if(((a xor r)and(b xor r))<0){if(b>0)Long.MAX_VALUE else 0L}else r}
    private fun safeMul(a:Long,m:Float):Long{if(m<=0f)return 0L;val r=a.toDouble()*m;return when{r>=Long.MAX_VALUE.toDouble()->Long.MAX_VALUE;r<=0.0->0L;else->r.roundToLong()}}
}
