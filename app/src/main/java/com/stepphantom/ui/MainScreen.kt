package com.stepphantom.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

sealed interface Dest {
    data object Home : Dest
    data object Apps : Dest
    data object Health : Dest
    data object Diag : Dest
    data object Advanced : Dest
    data class PkgConfig(val pkg: String) : Dest
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    var dest by rememberSaveable(
        stateSaver = androidx.compose.runtime.saveable.Saver(
            save = {
                when (it) {
                    is Dest.PkgConfig -> "pkg:${it.pkg}"
                    Dest.Apps -> "apps"; Dest.Health -> "health"
                    Dest.Diag -> "diag"; Dest.Advanced -> "adv"; else -> "home"
                }
            },
            restore = { saved ->
                val s = saved as String
                when {
                    s == "apps" -> Dest.Apps; s == "health" -> Dest.Health
                    s == "diag" -> Dest.Diag; s == "adv" -> Dest.Advanced
                    s.startsWith("pkg:") -> Dest.PkgConfig(s.removePrefix("pkg:"))
                    else -> Dest.Home
                }
            }
        )
    ) { mutableStateOf<Dest>(Dest.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItem(dest, Dest.Home, "Inicio", "\u2302") { dest = it }        // ⌂
                navItem(dest, Dest.Apps, "Apps", "\u2637") { dest = it }          // ☷
                navItem(dest, Dest.Health, "Health", "\u2665") { dest = it }      // ♥
                navItem(dest, Dest.Diag, "Diag", "\u24D8") { dest = it }          // ⓘ
                navItem(dest, Dest.Advanced, "Ajustes", "\u2699") { dest = it }   // ⚙
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (val d = dest) {
                Dest.Home -> HomeScreen(vm)
                Dest.Apps -> AppsScreen(vm, onOpenConfig = { dest = Dest.PkgConfig(it) })
                Dest.Health -> HealthConnectScreen(vm)
                Dest.Diag -> DiagnosticsScreen(vm)
                Dest.Advanced -> AdvancedScreen(vm)
                is Dest.PkgConfig -> PackageConfigScreen(vm, d.pkg, onBack = { dest = Dest.Apps })
            }
        }
    }
}

@Composable
private fun RowScope.navItem(current: Dest, target: Dest, label: String, glyph: String, set: (Dest) -> Unit) {
    val selected = current == target || (current is Dest.PkgConfig && target == Dest.Apps)
    NavigationBarItem(
        selected = selected,
        onClick = { set(target) },
        icon = { Text(glyph, style = MaterialTheme.typography.titleMedium) },
        label = { Text(label) }
    )
}

/* ---------------------------- Inicio ---------------------------- */

@Composable
fun HomeScreen(vm: MainViewModel) {
    val cfg by vm.config.collectAsState()
    val selected = cfg.packages.size
    val enabled = cfg.packages.values.count { it.enabled }

    ScrollColumn {
        Text("StepPhantom", style = MaterialTheme.typography.headlineSmall)
        Text("Módulo LSPosed / Vector · Android 16 · experimental",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        SectionCard {
            Text("Apps seleccionadas: $selected  ·  con módulo activo: $enabled")
            Text("Ruta implementada: SensorManager. Health Connect: sólo diagnóstico (no se reescribe).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        WarnCard(
            "También tenés que seleccionar estas apps en el Scope de LSPosed / Vector. " +
            "StepPhantom NO puede cambiar el Scope automáticamente con la API clásica."
        )

        SectionCard {
            Text("Cómo activar", style = MaterialTheme.typography.titleMedium)
            listOf(
                "1. Seleccioná la app en la pestaña Apps y configurala.",
                "2. Abrí LSPosed / Vector.",
                "3. Activá StepPhantom en Módulos.",
                "4. Entrá en Scope del módulo.",
                "5. Marcá la MISMA app.",
                "6. Forzá detención o reiniciá el proceso de la app objetivo."
            ).forEach { Text(it) }
        }

        SectionCard {
            Text("Preset de demo", style = MaterialTheme.typography.titleMedium)
            Text("En la config de una app tenés el botón \"4 → 87\": pone modo REEMPLAZAR con " +
                 "valor 87 (sólo demostración por SensorManager). También podés escribir cualquier valor.")
        }
    }
}

/* --------------------------- Ajustes --------------------------- */

@Composable
fun AdvancedScreen(vm: MainViewModel) {
    val logs by vm.logs.collectAsState()
    var showExport by rememberSaveable { mutableStateOf(false) }
    var showImport by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    ScrollColumn {
        Text("Ajustes avanzados", style = MaterialTheme.typography.headlineSmall)

        SectionCard {
            Text("Configuración JSON", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showExport = true }, Modifier.weight(1f)) { Text("Exportar") }
                OutlinedButton(onClick = { showImport = true }, Modifier.weight(1f)) { Text("Importar") }
            }
        }

        SectionCard {
            Text("Kill switch de emergencia", style = MaterialTheme.typography.titleMedium)
            Text("Si el módulo causa problemas, creá este archivo por ADB/root y reiniciá el " +
                 "proceso objetivo. Con el archivo presente, el módulo NO instala ningún hook.")
            Text("Crear:", style = MaterialTheme.typography.labelLarge)
            CodeLine("adb shell \"touch /data/local/tmp/stepphantom_disable\"")
            Text("Borrar:", style = MaterialTheme.typography.labelLarge)
            CodeLine("adb shell \"rm /data/local/tmp/stepphantom_disable\"")
            TextButton(onClick = {
                clipboard.setText(AnnotatedString("touch /data/local/tmp/stepphantom_disable"))
            }) { Text("Copiar comando crear") }
        }

        WarnCard("La escritura oficial en Health Connect vive en la pestaña Health, está apagada por " +
                 "defecto y NO es equivalente al módulo: agrega datos reales, no altera lo que ve una app.")

        SectionCard {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Log de configuración", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { AppDiagnostics.clear() }) { Text("Limpiar") }
            }
            if (logs.isEmpty()) Text("Sin eventos.", style = MaterialTheme.typography.bodySmall)
            else logs.take(40).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }

    if (showExport) JsonDialog("Configuración (JSON)", vm.exportJson(), readOnly = true,
        onConfirm = { showExport = false }, onDismiss = { showExport = false })
    if (showImport) {
        var err by remember { mutableStateOf(false) }
        JsonDialog("Importar JSON", "", readOnly = false, isError = err,
            confirmLabel = "Importar",
            onConfirm = { txt -> if (vm.importJson(txt)) showImport = false else err = true },
            onDismiss = { showImport = false })
    }
}

/* --------------------------- helpers UI --------------------------- */

@Composable
fun ScrollColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
fun WarnCard(text: String) {
    ElevatedCard(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(text, Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CodeLine(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
        Text(text, Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
    }
}

@Composable
fun JsonDialog(
    title: String,
    initial: String,
    readOnly: Boolean,
    isError: Boolean = false,
    confirmLabel: String = "Cerrar",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(confirmLabel) } },
        dismissButton = if (!readOnly) ({ TextButton(onClick = onDismiss) { Text("Cancelar") } }) else null,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, readOnly = readOnly, isError = isError,
                modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp)
            )
        }
    )
}
