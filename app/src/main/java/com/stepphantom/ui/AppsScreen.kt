package com.stepphantom.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
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
    val s = LocalStrings.current
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
        Spacer(Modifier.height(8.dp))
        Text(s.appsTitle, style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text(s.search) }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.showSystem, Modifier.weight(1f))
            Switch(checked = includeSystem, onCheckedChange = { includeSystem = it })
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = manual, onValueChange = { manual = it },
                label = { Text(s.addManual) }, singleLine = true, modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = { vm.addPackage(manual); manual = "" }) {
                Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(s.add)
            }
        }

        TextButton(onClick = { clipboard.setText(AnnotatedString(vm.selectedPackagesText())) }) {
            Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp)); Text(s.copySelected(selected.size))
        }

        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())

        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val manualOnly = selected.filter { sel -> apps.none { it.packageName == sel } }
            if (manualOnly.isNotEmpty()) {
                item { Text(s.selectedNotListed, style = MaterialTheme.typography.titleSmall) }
                items(manualOnly) { pkg ->
                    AppRow(pkg, pkg, isSystem = false, icon = null, checked = true,
                        onCheck = { vm.toggleSelected(pkg, it) }, onOpen = { onOpenConfig(pkg) })
                }
                item { HorizontalDivider() }
            }
            items(filtered) { app ->
                val bmp = remember(app.packageName) {
                    runCatching { app.icon?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()
                }
                AppRow(app.label, app.packageName, app.isSystem, bmp,
                    checked = app.packageName in selected,
                    onCheck = { vm.toggleSelected(app.packageName, it) },
                    onOpen = { onOpenConfig(app.packageName) })
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AppRow(
    label: String, pkg: String, isSystem: Boolean, icon: ImageBitmap?,
    checked: Boolean, onCheck: (Boolean) -> Unit, onOpen: () -> Unit
) {
    val s = LocalStrings.current
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Image(icon, null, Modifier.size(42.dp)) else Spacer(Modifier.size(42.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium)
                Text(pkg, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (isSystem) s.systemLabel else s.userLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onOpen) { Text(s.config) }
            Checkbox(checked = checked, onCheckedChange = onCheck)
        }
    }
}

/* --------------------- Config por aplicación (content-only) --------------------- */

@Composable
fun PackageConfigScreen(vm: MainViewModel, pkg: String) {
    val s = LocalStrings.current
    val cfgRoot by vm.config.collectAsState()
    val cfg = cfgRoot.packages[pkg] ?: PackageConfig()

    ScrollColumn {
        SectionCard {
            SwitchRow(s.moduleActive, cfg.enabled) { vm.updatePackage(pkg) { c -> c.copy(enabled = it) } }
            SwitchRow(s.hookSensorManager, cfg.hookSensorManager) { vm.updatePackage(pkg) { c -> c.copy(hookSensorManager = it) } }
        }

        SectionCard {
            Text(s.mode, style = MaterialTheme.typography.titleMedium)
            TransformMode.values().forEach { m ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = cfg.mode == m, onClick = { vm.updatePackage(pkg) { c -> c.copy(mode = m) } })
                    Text(modeLabel(s, m))
                }
            }
            FilledTonalButton(onClick = {
                vm.updatePackage(pkg) { c -> c.copy(mode = TransformMode.REEMPLAZAR, replaceValue = 87L, enabled = true) }
            }) { Text(s.preset487) }
        }

        SectionCard {
            Text(s.params, style = MaterialTheme.typography.titleMedium)
            NumberFieldLong(s.offset, cfg.offset) { v -> vm.updatePackage(pkg) { it.copy(offset = v) } }
            NumberFieldFloat(s.multiplier, cfg.multiplier) { v -> vm.updatePackage(pkg) { it.copy(multiplier = v) } }
            NumberFieldLong(s.replaceValue, cfg.replaceValue) { v -> vm.updatePackage(pkg) { it.copy(replaceValue = v) } }
            NumberFieldInt(s.stepsPerMinute, cfg.stepsPerMinute) { v -> vm.updatePackage(pkg) { it.copy(stepsPerMinute = v) } }
            NumberFieldLong(s.initialSteps, cfg.initialSteps) { v -> vm.updatePackage(pkg) { it.copy(initialSteps = v) } }
            Text(s.jitter(cfg.jitterPercent.toInt()))
            Slider(value = cfg.jitterPercent, onValueChange = { v -> vm.updatePackage(pkg) { it.copy(jitterPercent = v) } }, valueRange = 0f..50f)
            NumberFieldLong(s.minLimit, cfg.minLimit) { v -> vm.updatePackage(pkg) { it.copy(minLimit = v) } }
            NumberFieldLong(s.maxLimit, cfg.maxLimit) { v -> vm.updatePackage(pkg) { it.copy(maxLimit = v) } }
        }

        SectionCard {
            SwitchRow(s.pause, cfg.paused) { vm.updatePackage(pkg) { c -> c.copy(paused = it) } }
            OutlinedButton(onClick = { vm.resetBaseline(pkg) }) {
                Icon(Icons.Rounded.RestartAlt, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp)); Text(s.resetBaseline)
            }
        }

        SectionCard {
            Text(s.routeDiagTitle, style = MaterialTheme.typography.titleMedium)
            SwitchRow(s.detectHcJetpack, cfg.detectHcJetpack) { vm.updatePackage(pkg) { c -> c.copy(detectHcJetpack = it) } }
            SwitchRow(s.detectHcFramework, cfg.detectHcFramework) { vm.updatePackage(pkg) { c -> c.copy(detectHcFramework = it) } }
        }

        SectionCard {
            Text(s.experimentalTitle, style = MaterialTheme.typography.titleMedium)
            SwitchRow(s.simAccel, cfg.simAccelerometer) { vm.updatePackage(pkg) { c -> c.copy(simAccelerometer = it) } }
            SwitchRow(s.simGyro, cfg.simGyroscope) { vm.updatePackage(pkg) { c -> c.copy(simGyroscope = it) } }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/* ------------------------------ helpers ------------------------------ */

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun NumberFieldLong(label: String, value: Long, onValue: (Long) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text, onValueChange = { text = it; it.toLongOrNull()?.let(onValue) },
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun NumberFieldInt(label: String, value: Int, onValue: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text, onValueChange = { text = it; it.toIntOrNull()?.let(onValue) },
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun NumberFieldFloat(label: String, value: Float, onValue: (Float) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text, onValueChange = { text = it; it.toFloatOrNull()?.let(onValue) },
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun modeLabel(s: Strings, m: TransformMode): String = when (m) {
    TransformMode.ORIGINAL -> s.modeOriginal
    TransformMode.SUMAR -> s.modeAdd
    TransformMode.MULTIPLICAR -> s.modeMultiply
    TransformMode.REEMPLAZAR -> s.modeReplace
    TransformMode.RITMO_SIMULADO -> s.modeRhythm
    TransformMode.PERSONALIZADO -> s.modeCustom
}
