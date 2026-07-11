package com.stepphantom.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONObject

@Composable
fun DiagnosticsScreen(vm: MainViewModel) {
    val s = LocalStrings.current
    val diag by vm.diagnostics.collectAsState()

    ScrollColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.diagTitle, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { vm.clearDiagnostics() }) { Text(s.clear) }
        }
        Text(s.diagIntro, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (diag.isEmpty()) {
            SectionCard { Text(s.diagEmpty) }
        } else {
            diag.forEach { (pkg, json) -> DiagCard(s, pkg, json) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DiagCard(s: Strings, pkg: String, json: String) {
    val o = remember(json) { runCatching { JSONObject(json) }.getOrNull() }
    SectionCard {
        Text(pkg, style = MaterialTheme.typography.titleMedium)
        if (o == null) { Text("—"); return@SectionCard }
        Line(s.dRoute, o.optString("route", "-"))
        Line(s.dLastSensor, o.optString("lastSensor", "-"))
        Line(s.dRealValue, o.optLong("realValue", -1).let { if (it < 0) "—" else it.toString() })
        Line(s.dFakeValue, o.optLong("fakeValue", -1).let { if (it < 0) "—" else it.toString() })
        Line(s.dUid, o.optInt("uid", -1).toString())
        Line(s.dProcess, o.optString("process", "-"))
        Line(s.dSdk, "${o.optInt("sdkInt", 0)} (ext ${o.optString("sdkExtensions", "-")})")
        Line(s.dHcJetpack, s.yesNo(o.optBoolean("hcJetpack")))
        Line(s.dHcFramework, s.yesNo(o.optBoolean("hcFramework")))
        Line(s.dFitRecording, s.yesNo(o.optBoolean("fitOrRecording")))
        val classes = o.optJSONArray("classesFound")
        if (classes != null && classes.length() > 0) {
            Line(s.dClasses, (0 until classes.length()).joinToString(", ") { classes.optString(it) })
        }
        val err = o.optString("lastError", "")
        if (err.isNotEmpty()) Line(s.dLastError, err)
        Line(s.dUpdated, o.optLong("timestamp", 0).let {
            if (it == 0L) "-" else java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(it))
        })
    }
}

@Composable
private fun Line(k: String, v: String) {
    Row {
        Text("$k: ", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(v, style = MaterialTheme.typography.bodySmall)
    }
}
