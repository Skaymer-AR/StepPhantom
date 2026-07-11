package com.stepphantom.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stepphantom.health.HealthConnectWriter

@Composable
fun HealthConnectScreen(vm: MainViewModel) {
    val s = LocalStrings.current
    val hc by vm.hc.collectAsState()
    var count by rememberSaveable { mutableStateOf("87") }

    val permLauncher = rememberLauncherForActivityResult(
        contract = HealthConnectWriter.permissionContract()
    ) { _ -> vm.refreshHc() }

    LaunchedEffect(Unit) { vm.refreshHc() }

    ScrollColumn {
        Text(s.healthTitle, style = MaterialTheme.typography.headlineSmall)

        WarnCard(s.healthWarn)

        SectionCard {
            Text(s.status, style = MaterialTheme.typography.titleMedium)
            Text("${s.hcAvailableLabel}: ${s.yesNo(hc.available)}")
            Text("${s.permsGrantedLabel}: ${s.yesNo(hc.hasPermission)}")
            if (!hc.available) {
                Text(s.hcInstallHint, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(enabled = hc.available, onClick = { permLauncher.launch(HealthConnectWriter.permissions) }) {
                Text(s.grantPerms)
            }
        }

        SectionCard {
            Text(s.writeTestTitle, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = count, onValueChange = { count = it },
                label = { Text(s.stepCount) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = hc.available && hc.hasPermission,
                    onClick = { count.toLongOrNull()?.let { vm.writeTestSteps(it) } },
                    modifier = Modifier.weight(1f)) { Text(s.write) }
                OutlinedButton(enabled = hc.available && hc.hasPermission,
                    onClick = { vm.deleteOwnSteps() },
                    modifier = Modifier.weight(1f)) { Text(s.deleteMine) }
            }
            if (hc.message.isNotEmpty()) Text(hc.message, style = MaterialTheme.typography.bodySmall)
        }

        ToneCard(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer) {
            Text(s.noteTitle, style = MaterialTheme.typography.titleMedium)
            Text(s.healthNote, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(16.dp))
    }
}
