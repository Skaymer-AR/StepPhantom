package com.stepphantom.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stepphantom.config.ConfigRepository
import com.stepphantom.config.DiagnosticsStore
import com.stepphantom.config.PackageConfig
import com.stepphantom.config.StepPhantomConfig
import com.stepphantom.health.HealthConnectWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HcUiState(val available:Boolean=false,val hasPermission:Boolean=false,val message:String="")
class MainViewModel(app:Application):AndroidViewModel(app){
    private val repo=ConfigRepository(app.applicationContext)
    val config:StateFlow<StepPhantomConfig> = repo.config
    val logs:StateFlow<List<String>> = AppDiagnostics.lines
    val diagnostics:StateFlow<Map<String,String>> = DiagnosticsStore.snapshots
    private val _apps=MutableStateFlow<List<AppInfo>>(emptyList());val apps:StateFlow<List<AppInfo>> = _apps
    private val _loadingApps=MutableStateFlow(false);val loadingApps:StateFlow<Boolean> = _loadingApps
    private val _hc=MutableStateFlow(HcUiState());val hc:StateFlow<HcUiState> = _hc
    init{DiagnosticsStore.ensure(app.applicationContext);AppDiagnostics.log("App abierta.");refreshHc()}
    fun addPackage(pkg:String){val clean=pkg.trim();if(clean.isEmpty())return;if(!AppEnumerator.exists(getApplication(),clean))AppDiagnostics.log("Paquete inexistente (se agrega igual): $clean");repo.addPackage(clean);AppDiagnostics.log("Paquete agregado: $clean")}
    fun removePackage(pkg:String){repo.removePackage(pkg);AppDiagnostics.log("Paquete quitado: $pkg")}
    fun toggleSelected(pkg:String,selected:Boolean){if(selected)addPackage(pkg)else removePackage(pkg)}
    fun updatePackage(pkg:String,transform:(PackageConfig)->PackageConfig)=repo.updatePackage(pkg,transform)
    fun packageConfig(pkg:String):PackageConfig=repo.packageConfig(pkg)
    fun resetBaseline(pkg:String){repo.updatePackage(pkg){it.copy(resetToken=System.currentTimeMillis())};AppDiagnostics.log("Reset de baseline: $pkg")}
    fun selectedPackagesText():String=config.value.packages.keys.joinToString("\n")
    fun loadApps(includeSystem:Boolean){_loadingApps.value=true;viewModelScope.launch{_apps.value=withContext(Dispatchers.Default){AppEnumerator.list(getApplication(),includeSystem)};_loadingApps.value=false}}
    fun exportJson():String=repo.exportJson()
    fun importJson(json:String):Boolean{val ok=repo.importJson(json);AppDiagnostics.log(if(ok)"Config importada" else "JSON inválido");return ok}
    fun clearDiagnostics(){DiagnosticsStore.clear();AppDiagnostics.log("Diagnósticos borrados")}
    fun refreshHc(){viewModelScope.launch{val ctx=getApplication<Application>();val available=HealthConnectWriter.isAvailable(ctx);val hasPerm=if(available)runCatching{HealthConnectWriter.hasPermissions(ctx)}.getOrDefault(false)else false;_hc.value=_hc.value.copy(available=available,hasPermission=hasPerm)}}
    fun writeTestSteps(count:Long){viewModelScope.launch{val ctx=getApplication<Application>();val msg=runCatching{if(HealthConnectWriter.writeTestSteps(ctx,count))"Escrito: $count pasos de prueba" else "No se pudo escribir (HC no disponible o valor inválido)"}.getOrElse{"Error al escribir: $it"};_hc.value=_hc.value.copy(message=msg);AppDiagnostics.log("HC write: $msg")}}
    fun deleteOwnSteps(){viewModelScope.launch{val ctx=getApplication<Application>();val msg=runCatching{"Borrados ${HealthConnectWriter.deleteOwnSteps(ctx)} registros escritos por StepPhantom"}.getOrElse{"Error al borrar: $it"};_hc.value=_hc.value.copy(message=msg);AppDiagnostics.log("HC delete: $msg")}}
}
