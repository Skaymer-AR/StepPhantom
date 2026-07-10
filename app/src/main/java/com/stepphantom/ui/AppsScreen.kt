package com.stepphantom.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.stepphantom.config.PackageConfig
import com.stepphantom.config.TransformMode

@Composable
fun AppsScreen(vm: MainViewModel, onOpenConfig: (String) -> Unit) {
    val cfg by vm.config.collectAsState()
    val apps by vm.apps.collectAsState()
    val loading by vm.loadingApps.collectAsState()
    val clipboard = LocalClipboardManager.current

    var includeSystem by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var manual by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(includeSystem) { vm.loadApps(includeSystem) }

    val selected = cfg.packages.keys
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("Aplicaciones", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(value=query,onValueChange={query=it},label={Text("Buscar por nombre o package")},singleLine=true,modifier=Modifier.fillMaxWidth().padding(top=8.dp))
        Row(verticalAlignment=Alignment.CenterVertically){Text("Mostrar apps del sistema",Modifier.weight(1f));Switch(checked=includeSystem,onCheckedChange={includeSystem=it})}
        Row(verticalAlignment=Alignment.CenterVertically){OutlinedTextField(value=manual,onValueChange={manual=it},label={Text("Agregar package manual")},singleLine=true,modifier=Modifier.weight(1f));Spacer(Modifier.width(8.dp));Button(onClick={vm.addPackage(manual);manual=""}){Text("Agregar")}}
        Row{TextButton(onClick={clipboard.setText(AnnotatedString(vm.selectedPackagesText()))}){Text("Copiar seleccionadas (${selected.size})")}}
        if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(4.dp)){
            val manualOnly=selected.filter{s->apps.none{it.packageName==s}}
            if(manualOnly.isNotEmpty()){item{Text("Seleccionadas no listadas",style=MaterialTheme.typography.titleSmall)};items(manualOnly){pkg->AppRow(pkg,pkg,false,null,true,{vm.toggleSelected(pkg,it)},{onOpenConfig(pkg)})};item{HorizontalDivider()}}
            items(filtered){app->val bmp=remember(app.packageName){runCatching{app.icon?.toBitmap(96,96)?.asImageBitmap()}.getOrNull()};AppRow(app.label,app.packageName,app.isSystem,bmp,app.packageName in selected,{vm.toggleSelected(app.packageName,it)},{onOpenConfig(app.packageName)})}
            item{Spacer(Modifier.height(24.dp))}
        }
    }
}

@Composable private fun AppRow(label:String,pkg:String,isSystem:Boolean,icon:androidx.compose.ui.graphics.ImageBitmap?,checked:Boolean,onCheck:(Boolean)->Unit,onOpen:()->Unit){ElevatedCard(Modifier.fillMaxWidth()){Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){if(icon!=null)Image(icon,null,Modifier.size(40.dp))else Spacer(Modifier.size(40.dp));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(label,maxLines=1,overflow=TextOverflow.Ellipsis);Text(pkg,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis);Text(if(isSystem)"sistema" else "usuario",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};TextButton(onClick=onOpen){Text("Config")};Checkbox(checked=checked,onCheckedChange=onCheck)}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PackageConfigScreen(vm:MainViewModel,pkg:String,onBack:()->Unit){val cfgRoot by vm.config.collectAsState();val cfg=cfgRoot.packages[pkg]?:PackageConfig();Scaffold(topBar={TopAppBar(title={Text(pkg,maxLines=1,overflow=TextOverflow.Ellipsis)},navigationIcon={IconButton(onClick=onBack){Text("←")}})}){pad->Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    SectionCard{SwitchRow("Módulo activo para esta app",cfg.enabled){vm.updatePackage(pkg){c->c.copy(enabled=it)}};SwitchRow("Hook SensorManager (ruta implementada)",cfg.hookSensorManager){vm.updatePackage(pkg){c->c.copy(hookSensorManager=it)}}}
    SectionCard{Text("Modo",style=MaterialTheme.typography.titleMedium);TransformMode.values().forEach{m->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(selected=cfg.mode==m,onClick={vm.updatePackage(pkg){c->c.copy(mode=m)}});Text(modeLabel(m))}};Button(onClick={vm.updatePackage(pkg){c->c.copy(mode=TransformMode.REEMPLAZAR,replaceValue=87L,enabled=true)}}){Text("Preset 4 → 87")}}
    SectionCard{Text("Parámetros",style=MaterialTheme.typography.titleMedium);NumberFieldLong("Offset (SUMAR)",cfg.offset){v->vm.updatePackage(pkg){it.copy(offset=v)}};NumberFieldFloat("Multiplicador (MULTIPLICAR)",cfg.multiplier){v->vm.updatePackage(pkg){it.copy(multiplier=v)}};NumberFieldLong("Valor objetivo (REEMPLAZAR)",cfg.replaceValue){v->vm.updatePackage(pkg){it.copy(replaceValue=v)}};NumberFieldInt("Pasos por minuto",cfg.stepsPerMinute){v->vm.updatePackage(pkg){it.copy(stepsPerMinute=v)}};NumberFieldLong("Pasos iniciales",cfg.initialSteps){v->vm.updatePackage(pkg){it.copy(initialSteps=v)}};Text("Jitter: ${cfg.jitterPercent.toInt()}%");Slider(value=cfg.jitterPercent,onValueChange={v->vm.updatePackage(pkg){it.copy(jitterPercent=v)}},valueRange=0f..50f);NumberFieldLong("Límite mínimo (PERSONALIZADO)",cfg.minLimit){v->vm.updatePackage(pkg){it.copy(minLimit=v)}};NumberFieldLong("Límite máximo (-1 = sin límite)",cfg.maxLimit){v->vm.updatePackage(pkg){it.copy(maxLimit=v)}}}
    SectionCard{SwitchRow("Pausa",cfg.paused){vm.updatePackage(pkg){c->c.copy(paused=it)}};OutlinedButton(onClick={vm.resetBaseline(pkg)}){Text("Reiniciar baseline / sesión")}}
    SectionCard{Text("Diagnóstico de vías (no reescribe)",style=MaterialTheme.typography.titleMedium);SwitchRow("Detectar Health Connect Jetpack",cfg.detectHcJetpack){vm.updatePackage(pkg){c->c.copy(detectHcJetpack=it)}};SwitchRow("Detectar Health Connect framework",cfg.detectHcFramework){vm.updatePackage(pkg){c->c.copy(detectHcFramework=it)}}}
    SectionCard{Text("Experimental (puede romper apps)",style=MaterialTheme.typography.titleMedium);SwitchRow("Simular acelerómetro",cfg.simAccelerometer){vm.updatePackage(pkg){c->c.copy(simAccelerometer=it)}};SwitchRow("Simular giroscopio",cfg.simGyroscope){vm.updatePackage(pkg){c->c.copy(simGyroscope=it)}}}
    Spacer(Modifier.height(24.dp))
}}}
@Composable fun SwitchRow(label:String,checked:Boolean,onChange:(Boolean)->Unit){Row(verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f));Switch(checked=checked,onCheckedChange=onChange)}}
@Composable fun NumberFieldLong(label:String,value:Long,onValue:(Long)->Unit){var text by remember(value){mutableStateOf(value.toString())};OutlinedTextField(value=text,onValueChange={text=it;it.toLongOrNull()?.let(onValue)},label={Text(label)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())}
@Composable fun NumberFieldInt(label:String,value:Int,onValue:(Int)->Unit){var text by remember(value){mutableStateOf(value.toString())};OutlinedTextField(value=text,onValueChange={text=it;it.toIntOrNull()?.let(onValue)},label={Text(label)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth())}
@Composable fun NumberFieldFloat(label:String,value:Float,onValue:(Float)->Unit){var text by remember(value){mutableStateOf(value.toString())};OutlinedTextField(value=text,onValueChange={text=it;it.toFloatOrNull()?.let(onValue)},label={Text(label)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.fillMaxWidth())}
private fun modeLabel(m:TransformMode)=when(m){TransformMode.ORIGINAL->"ORIGINAL (sin cambios)";TransformMode.SUMAR->"SUMAR (real + offset)";TransformMode.MULTIPLICAR->"MULTIPLICAR (real × mult)";TransformMode.REEMPLAZAR->"REEMPLAZAR (valor objetivo)";TransformMode.RITMO_SIMULADO->"RITMO_SIMULADO (por tiempo)";TransformMode.PERSONALIZADO->"PERSONALIZADO (combinado)"}
