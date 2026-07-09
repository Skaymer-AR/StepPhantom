package com.stepphantom.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.stepphantom.config.ConfigRepository
import com.stepphantom.config.ModuleConfig
import com.stepphantom.config.WalkMode
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ConfigRepository(app.applicationContext)
    val config: StateFlow<ModuleConfig> = repo.config
    val logs: StateFlow<List<String>> = AppDiagnostics.lines

    init { AppDiagnostics.log("App abierta. Config cargada.") }

    fun setModuleEnabled(v: Boolean) = edit("Módulo ${if (v) "ACTIVADO" else "desactivado"}") { it.copy(moduleEnabled = v) }

    fun addPackage(pkg: String) {
        val clean = pkg.trim()
        if (clean.isEmpty()) return
        edit("Paquete agregado: $clean") {
            if (clean in it.targetPackages) it
            else it.copy(targetPackages = it.targetPackages + clean)
        }
    }

    fun removePackage(pkg: String) =
        edit("Paquete quitado: $pkg") { it.copy(targetPackages = it.targetPackages - pkg) }

    fun setSimStepCounter(v: Boolean) = edit("Sim STEP_COUNTER=$v") { it.copy(simStepCounter = v) }
    fun setSimStepDetector(v: Boolean) = edit("Sim STEP_DETECTOR=$v") { it.copy(simStepDetector = v) }
    fun setSimAccelerometer(v: Boolean) = edit("Sim ACCEL=$v") { it.copy(simAccelerometer = v) }
    fun setSimGyroscope(v: Boolean) = edit("Sim GYRO=$v") { it.copy(simGyroscope = v) }

    fun setStepsPerMinute(v: Int) = edit("Pasos/min=$v") { it.copy(stepsPerMinute = v.coerceIn(0, 100_000)) }
    fun setInitialSteps(v: Long) = edit("Pasos iniciales=$v") { it.copy(initialSteps = v.coerceAtLeast(0)) }
    fun setJitter(v: Float) = edit("Jitter=$v%") { it.copy(jitterPercent = v.coerceIn(0f, 100f)) }
    fun setWalkMode(m: WalkMode) = edit("Modo=$m") { it.copy(walkMode = m) }

    fun resetCounter() = edit("Reset de contador solicitado") {
        it.copy(resetToken = System.currentTimeMillis())
    }

    fun exportJson(): String = repo.exportJson()

    fun importJson(json: String): Boolean {
        val ok = repo.importJson(json)
        AppDiagnostics.log(if (ok) "Config importada desde JSON" else "JSON inválido: no se importó")
        return ok
    }

    private inline fun edit(logMsg: String, crossinline t: (ModuleConfig) -> ModuleConfig) {
        repo.update { t(it) }
        AppDiagnostics.log(logMsg)
    }
}
