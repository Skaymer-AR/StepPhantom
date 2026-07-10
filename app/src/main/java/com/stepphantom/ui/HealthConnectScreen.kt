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
    val hc by vm.hc.collectAsState()
    var count by rememberSaveable { mutableStateOf("87") }

    val permLauncher = rememberLauncherForActivityResult(
        contract = HealthConnectWriter.permissionContract()
    ) { _ -> vm.refreshHc() }

    LaunchedEffect(Unit) { vm.refreshHc() }

    ScrollColumn {
        Text("Health Connect", style = MaterialTheme.typography.headlineSmall)

        WarnCard(
            "Esto NO es el módulo. El módulo LSPosed altera lo que VE una app seleccionada " +
            "(intercepta sensores). Esta pantalla ESCRIBE datos reales de prueba en el repositorio " +
            "de Health Connect usando la API oficial, atribuidos a StepPhantom, sin ocultar el origen. " +
            "No son equivalentes."
        )

        SectionCard {
            Text("Estado", style = MaterialTheme.typography.titleMedium)
            Text("Health Connect disponible: ${if (hc.available) "sí" else "no"}")
            Text("Permisos de pasos concedidos: ${if (hc.hasPermission) "sí" else "no"}")
            if (!hc.available) {
                Text("Instalá/activá Health Connect (en Android 14+ viene en el sistema) y volvé a esta pantalla.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                enabled = hc.available,
                onClick = { permLauncher.launch(HealthConnectWriter.permissions) }
            ) { Text("Conceder permisos (WRITE/READ pasos)") }
        }

        SectionCard {
            Text("Escribir datos de prueba", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = count, onValueChange = { count = it },
                label = { Text("Cantidad de pasos") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = hc.available && hc.hasPermission,
                    onClick = { count.toLongOrNull()?.let { vm.writeTestSteps(it) } },
                    modifier = Modifier.weight(1f)
                ) { Text("Escribir") }
                OutlinedButton(
                    enabled = hc.available && hc.hasPermission,
                    onClick = { vm.deleteOwnSteps() },
                    modifier = Modifier.weight(1f)
                ) { Text("Borrar lo mío") }
            }
            if (hc.message.isNotEmpty()) {
                Text(hc.message, style = MaterialTheme.typography.bodySmall)
            }
        }

        SectionCard {
            Text("Nota", style = MaterialTheme.typography.titleMedium)
            Text("El borrado sólo elimina StepsRecords cuyo DataOrigin es StepPhantom. No toca datos " +
                 "de otras fuentes ni intenta ocultar nada. Apagado por defecto: no se escribe nada sin tu acción.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
