package com.stepphantom.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONObject

@Composable fun DiagnosticsScreen(vm:MainViewModel){val diag by vm.diagnostics.collectAsState();ScrollColumn{Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Text("Diagnóstico",Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall);TextButton(onClick={vm.clearDiagnostics()}){Text("Limpiar")}};Text("Lo que reporta el hook desde el proceso de cada app seleccionada. Revisá Scope y reiniciá su proceso.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);if(diag.isEmpty())SectionCard{Text("Sin diagnósticos todavía.")}else diag.forEach{(pkg,json)->DiagCard(pkg,json)};Spacer(Modifier.height(24.dp))}}
@Composable private fun DiagCard(pkg:String,json:String){val o=remember(json){runCatching{JSONObject(json)}.getOrNull()};SectionCard{Text(pkg,style=MaterialTheme.typography.titleMedium);if(o==null){Text("(diagnóstico ilegible)");return@SectionCard};Line("Vía detectada",o.optString("route","-"));Line("Último sensor",o.optString("lastSensor","-"));Line("Valor real",o.optLong("realValue",-1).let{if(it<0)"—" else it.toString()});Line("Valor simulado",o.optLong("fakeValue",-1).let{if(it<0)"—" else it.toString()});Line("UID",o.optInt("uid",-1).toString());Line("Proceso",o.optString("process","-"));Line("SDK","${o.optInt("sdkInt",0)} (ext ${o.optString("sdkExtensions","-")})");Line("HC Jetpack",o.optBoolean("hcJetpack").toString());Line("HC framework",o.optBoolean("hcFramework").toString());Line("Fit/Recording",o.optBoolean("fitOrRecording").toString());val err=o.optString("lastError","");if(err.isNotEmpty())Line("Último error",err)}}
@Composable private fun Line(k:String,v:String){Row{Text("$k: ",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(v,style=MaterialTheme.typography.bodySmall)}}
