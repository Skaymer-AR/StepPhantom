package com.stepphantom.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
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
fun MainScreen(vm: MainViewModel, isConsolePreview: Boolean = false) {
    val s = LocalStrings.current
    val lang by vm.language.collectAsState()
    var languageMenuExpanded by rememberSaveable { mutableStateOf(false) }

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
                val v = saved as String
                when {
                    v == "apps" -> Dest.Apps; v == "health" -> Dest.Health
                    v == "diag" -> Dest.Diag; v == "adv" -> Dest.Advanced
                    v.startsWith("pkg:") -> Dest.PkgConfig(v.removePrefix("pkg:"))
                    else -> Dest.Home
                }
            }
        )
    ) { mutableStateOf<Dest>(Dest.Home) }

    Scaffold(
        topBar = {
            when (val d = dest) {
                is Dest.PkgConfig -> TopAppBar(
                    title = { Text(d.pkg, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = { dest = Dest.Apps }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, s.back)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                else -> CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.brand, style = MaterialTheme.typography.titleLarge)
                            if (isConsolePreview) {
                                Text(s.previewLabel, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    actions = {
                        Box {
                            FilledTonalButton(
                                onClick = { languageMenuExpanded = true },
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Rounded.Language, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(lang.name)
                            }
                            DropdownMenu(
                                expanded = languageMenuExpanded,
                                onDismissRequest = { languageMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(s.english) },
                                    onClick = {
                                        vm.setLanguage(Lang.EN)
                                        languageMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(s.spanish) },
                                    onClick = {
                                        vm.setLanguage(Lang.ES)
                                        languageMenuExpanded = false
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar {
                navItem(dest, Dest.Home, s.navHome, Icons.Rounded.Home) { dest = it }
                navItem(dest, Dest.Apps, s.navApps, Icons.Rounded.Apps) { dest = it }
                navItem(dest, Dest.Health, s.navHealth, Icons.Rounded.MonitorHeart) { dest = it }
                navItem(dest, Dest.Diag, s.navDiag, Icons.Rounded.Insights) { dest = it }
                navItem(dest, Dest.Advanced, s.navSettings, Icons.Rounded.Settings) { dest = it }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            AnimatedContent(
                targetState = dest,
                transitionSpec = {
                    (fadeIn(tween(240)) + slideInHorizontally(tween(240)) { it / 8 }) togetherWith
                        (fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it / 8 })
                },
                label = "screen"
            ) { d ->
                when (d) {
                    Dest.Home -> HomeScreen(vm, onOpenApps = { dest = Dest.Apps })
                    Dest.Apps -> AppsScreen(vm, onOpenConfig = { dest = Dest.PkgConfig(it) })
                    Dest.Health -> HealthConnectScreen(vm)
                    Dest.Diag -> DiagnosticsScreen(vm)
                    Dest.Advanced -> SettingsScreen(vm)
                    is Dest.PkgConfig -> PackageConfigScreen(vm, d.pkg)
                }
            }
        }
    }
}

@Composable
private fun RowScope.navItem(current: Dest, target: Dest, label: String, icon: ImageVector, set: (Dest) -> Unit) {
    val selected = current == target || (current is Dest.PkgConfig && target == Dest.Apps)
    NavigationBarItem(
        selected = selected,
        onClick = { set(target) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) }
    )
}

/* ------------------------------ Inicio ------------------------------ */

@Composable
fun HomeScreen(vm: MainViewModel, onOpenApps: () -> Unit) {
    val s = LocalStrings.current
    val cfg by vm.config.collectAsState()
    val profiles = cfg.packages.toSortedMap()
    val selected = profiles.size
    val enabled = profiles.values.count { it.enabled && !it.paused }
    val enabledRatio = if (selected == 0) 0f else enabled.toFloat() / selected

    ScrollColumn {
        Text(s.homeTitle, style = MaterialTheme.typography.headlineSmall)
        Text(s.homeSubtitle, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(s.homeProfilesTitle, style = MaterialTheme.typography.titleMedium)
                    Text("$enabled", style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary)
                    Text(s.homeProfilesCount(enabled, selected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { enabledRatio.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 9.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$selected", style = MaterialTheme.typography.titleLarge)
                        Text(s.navApps, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.homeTargetsTitle, Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium)
                FilledTonalButton(onClick = onOpenApps) {
                    Icon(Icons.Rounded.Apps, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s.homeConfigureApps)
                }
            }
            if (profiles.isEmpty()) {
                Text(s.homeNoTargets, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                profiles.entries.take(3).forEach { (packageName, packageConfig) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(packageName, style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1)
                            val status = when {
                                !packageConfig.enabled -> s.homeDisabledStatus
                                packageConfig.paused -> s.homePausedStatus
                                else -> s.homeEnabledStatus
                            }
                            Text(
                                status,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (packageConfig.enabled && !packageConfig.paused) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (packageConfig.enabled) Icons.Rounded.Insights else Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = if (packageConfig.enabled && !packageConfig.paused) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (selected > 3) {
                    Text(s.homeMoreTargets(selected - 3),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        WarnCard(s.scopeWarning)

        SectionCard {
            Text(s.howToActivate, style = MaterialTheme.typography.titleMedium)
            s.steps.forEach { Text(it) }
        }

        ToneCard(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer) {
            Text(s.presetTitle, style = MaterialTheme.typography.titleMedium)
            Text(s.presetBody, style = MaterialTheme.typography.bodyMedium)
        }

        SectionCard {
            Text(s.homeSelected(selected, enabled), style = MaterialTheme.typography.bodySmall)
            Text(s.homeRoute, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/* ------------------------------ Ajustes ------------------------------ */

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val s = LocalStrings.current
    val lang by vm.language.collectAsState()
    val dynamic by vm.dynamicColor.collectAsState()
    val logs by vm.logs.collectAsState()
    val clipboard = LocalClipboardManager.current
    var showExport by rememberSaveable { mutableStateOf(false) }
    var showImport by rememberSaveable { mutableStateOf(false) }

    ScrollColumn {
        Text(s.settingsTitle, style = MaterialTheme.typography.headlineSmall)

        SectionCard {
            Text(s.language, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = lang == Lang.ES, onClick = { vm.setLanguage(Lang.ES) },
                    label = { Text(s.spanish) })
                FilterChip(selected = lang == Lang.EN, onClick = { vm.setLanguage(Lang.EN) },
                    label = { Text(s.english) })
            }
        }

        SectionCard {
            Text(s.appearance, style = MaterialTheme.typography.titleMedium)
            SwitchRow(s.dynamicColor, dynamic) { vm.setDynamicColor(it) }
            Text(s.dynamicColorSub, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SectionCard {
            Text(s.jsonConfig, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showExport = true }, Modifier.weight(1f)) { Text(s.export) }
                OutlinedButton(onClick = { showImport = true }, Modifier.weight(1f)) { Text(s.importLabel) }
            }
        }

        SectionCard {
            Text(s.killSwitch, style = MaterialTheme.typography.titleMedium)
            Text(s.killSwitchBody, style = MaterialTheme.typography.bodyMedium)
            Text(s.create, style = MaterialTheme.typography.labelLarge)
            CodeLine("adb shell \"touch /data/local/tmp/stepphantom_disable\"")
            Text(s.delete, style = MaterialTheme.typography.labelLarge)
            CodeLine("adb shell \"rm /data/local/tmp/stepphantom_disable\"")
            TextButton(onClick = {
                clipboard.setText(AnnotatedString("touch /data/local/tmp/stepphantom_disable"))
            }) {
                Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp)); Text(s.copyCreateCmd)
            }
        }

        WarnCard(s.healthSeparateWarn)

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.configLog, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { AppDiagnostics.clear() }) { Text(s.clear) }
            }
            if (logs.isEmpty()) Text(s.noEvents, style = MaterialTheme.typography.bodySmall)
            else logs.take(40).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showExport) JsonDialog(s.exportTitle, vm.exportJson(), readOnly = true, confirmLabel = s.close,
        onConfirm = { showExport = false }, onDismiss = { showExport = false })
    if (showImport) {
        var err by remember { mutableStateOf(false) }
        JsonDialog(s.importTitle, "", readOnly = false, isError = err, confirmLabel = s.importLabel,
            onConfirm = { txt -> if (vm.importJson(txt)) showImport = false else err = true },
            onDismiss = { showImport = false })
    }
}

/* ------------------------------ helpers UI ------------------------------ */

@Composable
fun ScrollColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun HeroHeader(title: String, subtitle: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun StatPill(value: String, label: String, container: Color, onContainer: Color) {
    Surface(color = container, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = onContainer)
            Text(label, style = MaterialTheme.typography.labelMedium, color = onContainer)
        }
    }
}

@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
fun ToneCard(container: Color, onContainer: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = container, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        CompositionLocalProvider(LocalContentColor provides onContainer) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

@Composable
fun WarnCard(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CodeLine(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
        Text(text, Modifier.padding(10.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
    }
}

@Composable
fun JsonDialog(
    title: String,
    initial: String,
    readOnly: Boolean,
    isError: Boolean = false,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(confirmLabel) } },
        dismissButton = if (!readOnly) ({ TextButton(onClick = onDismiss) { Text(s.cancel) } }) else null,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, readOnly = readOnly, isError = isError,
                label = if (!readOnly) ({ Text(s.pasteJson) }) else null,
                modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp)
            )
            if (isError) Text(s.invalidJson, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
    )
}
