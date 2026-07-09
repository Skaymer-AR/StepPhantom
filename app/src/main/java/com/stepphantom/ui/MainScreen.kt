package com.stepphantom.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stepphantom.config.ModuleConfig
import com.stepphantom.config.WalkMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val cfg by vm.config.collectAsState()
    val logs by vm.logs.collectAsState()

    var showExport by rememberSaveable { mutableStateOf(false) }
    var showImport by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StepPhantom") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatusCard(cfg, vm::setModuleEnabled)
            PackagesCard(cfg, vm::addPackage, vm::removePackage)
            SensorsCard(cfg, vm)
            ParamsCard(cfg, vm)
            ModeCard(cfg, vm::setWalkMode)
            ActionsCard(
                onReset = vm::resetCounter,
                onExport = { showExport = true },
                onImport = { showImport = true }
            )
            DiagnosticsCard(cfg, logs, onClear = { AppDiagnostics.clear() })
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showExport) {
        ExportDialog(json = vm.exportJson(), onDismiss = { showExport = false })
    }
    if (showImport) {
        ImportDialog(
            onImport = { json -> vm.importJson(json).also { if (it) showImport = false } },
            onDismiss = { showImport = false }
        )
    }
}

/* --------------------------------------------------------------------- */

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StatusCard(cfg: ModuleConfig, onToggle: (Boolean) -> Unit) {
    SectionCard("Estado del módulo") {
        SwitchRow(
            label = if (cfg.moduleEnabled) "Módulo ACTIVADO" else "Módulo desactivado",
            checked = cfg.moduleEnabled,
            subtitle = "El estado real del hook se ve en el log de LSPosed/Vector o en logcat (filtrá \"[StepPhantom]\").",
            onChange = onToggle
        )
    }
}

@Composable
private fun PackagesCard(
    cfg: ModuleConfig,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    SectionCard("Paquetes objetivo") {
        var input by rememberSaveable { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("com.ejemplo.app") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onAdd(input); input = "" }) { Text("Agregar") }
        }
        if (cfg.targetPackages.isEmpty()) {
            Text("Sin paquetes: el módulo no simulará nada (además del scope de LSPosed).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            cfg.targetPackages.forEach { pkg ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pkg, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = { onRemove(pkg) }) { Text("Quitar") }
                }
            }
        }
    }
}

@Composable
private fun SensorsCard(cfg: ModuleConfig, vm: MainViewModel) {
    SectionCard("Sensores a simular") {
        SwitchRow("TYPE_STEP_COUNTER", cfg.simStepCounter, onChange = vm::setSimStepCounter)
        SwitchRow("TYPE_STEP_DETECTOR", cfg.simStepDetector, onChange = vm::setSimStepDetector)
        SwitchRow("Acelerómetro", cfg.simAccelerometer,
            subtitle = "⚠ Puede romper apps que dependen del acelerómetro real.",
            onChange = vm::setSimAccelerometer)
        SwitchRow("Giroscopio", cfg.simGyroscope,
            subtitle = "⚠ Igual que arriba: activá sólo si sabés qué esperás.",
            onChange = vm::setSimGyroscope)
    }
}

@Composable
private fun ParamsCard(cfg: ModuleConfig, vm: MainViewModel) {
    SectionCard("Parámetros del motor") {
        NumberField("Pasos por minuto", cfg.stepsPerMinute.toString()) {
            it.toIntOrNull()?.let(vm::setStepsPerMinute)
        }
        NumberField("Pasos iniciales (total)", cfg.initialSteps.toString()) {
            it.toLongOrNull()?.let(vm::setInitialSteps)
        }
        Text("Variación aleatoria (jitter): ${cfg.jitterPercent.toInt()}%")
        Slider(
            value = cfg.jitterPercent,
            onValueChange = vm::setJitter,
            valueRange = 0f..50f
        )
    }
}

@Composable
private fun NumberField(label: String, value: String, onValue: (String) -> Unit) {
    // Estado local para no pelear con el cursor; se re-sincroniza si cambia afuera.
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onValue(it) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ModeCard(cfg: ModuleConfig, onMode: (WalkMode) -> Unit) {
    SectionCard("Modo de caminata") {
        val modes = listOf(
            WalkMode.SOFT to "Suave",
            WalkMode.CONSTANT to "Constante",
            WalkMode.PAUSE to "Pausa"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modes.forEach { (mode, label) ->
                FilterChip(
                    selected = cfg.walkMode == mode,
                    onClick = { onMode(mode) },
                    label = { Text(label) }
                )
            }
        }
        val hint = when (cfg.walkMode) {
            WalkMode.SOFT -> "Caminata con pequeña variación suave alrededor del ritmo."
            WalkMode.CONSTANT -> "Ritmo parejo y predecible."
            WalkMode.PAUSE -> "Congela el contador: el tiempo en pausa no suma pasos."
        }
        Text(hint, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActionsCard(onReset: () -> Unit, onExport: () -> Unit, onImport: () -> Unit) {
    SectionCard("Acciones") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("Reset contador") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text("Exportar JSON") }
            OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Importar JSON") }
        }
    }
}

@Composable
private fun DiagnosticsCard(cfg: ModuleConfig, logs: List<String>, onClear: () -> Unit) {
    SectionCard("Diagnóstico") {
        val sensores = buildList {
            if (cfg.simStepCounter) add("STEP_COUNTER")
            if (cfg.simStepDetector) add("STEP_DETECTOR")
            if (cfg.simAccelerometer) add("ACCEL")
            if (cfg.simGyroscope) add("GYRO")
        }.joinToString(", ").ifEmpty { "ninguno" }

        Text("Módulo (según config): ${if (cfg.moduleEnabled) "activo" else "inactivo"}")
        Text("Paquetes: ${cfg.targetPackages.size}")
        Text("Sensores simulados: $sensores")
        Text("Pasos/min: ${cfg.stepsPerMinute}   ·   Iniciales: ${cfg.initialSteps}")
        Text("Jitter: ${cfg.jitterPercent.toInt()}%   ·   Modo: ${cfg.walkMode}")

        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Log de configuración", modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = onClear) { Text("Limpiar") }
        }
        Text(
            "Runtime del hook (último paquete/sensor/errores) -> log de LSPosed/Vector o logcat.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (logs.isEmpty()) {
            Text("Sin eventos todavía.", style = MaterialTheme.typography.bodySmall)
        } else {
            logs.take(40).forEach { line ->
                Text(line, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ExportDialog(json: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = { Text("Configuración (JSON)") },
        text = {
            OutlinedTextField(
                value = json,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
            )
        }
    )
}

@Composable
private fun ImportDialog(onImport: (String) -> Boolean, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { error = !onImport(text) }) { Text("Importar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Importar JSON") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = false },
                    label = { Text("Pegá el JSON acá") },
                    isError = error,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
                )
                if (error) Text("JSON inválido.", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    )
}
