package com.stepphantom.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * ESCRITURA OFICIAL de datos de prueba en Health Connect. Es una función APARTE
 * del módulo LSPosed y NO es equivalente:
 *
 *   - El módulo LSPosed altera lo que VE una app seleccionada (intercepta sensores).
 *   - Esto AGREGA registros reales al repositorio de Health Connect, atribuidos a
 *     StepPhantom, usando la API oficial y WRITE_STEPS. No oculta el origen.
 *
 * Apagada por defecto. Requiere que el usuario conceda permisos explícitos en
 * Health Connect. Sólo puede borrar los registros que ESTA app escribió.
 *
 * NOTA de versión: pineado contra connect-client:1.1.0-alpha07. Si subís la
 * versión, la API de Metadata puede cambiar (p.ej. Metadata.manualEntry(...)).
 */
object HealthConnectWriter {

    /** Permisos que pide (lectura para poder borrar sólo lo nuestro). */
    val permissions: Set<String> = setOf(
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    fun sdkStatus(context: Context): Int = HealthConnectClient.getSdkStatus(context)

    fun isAvailable(context: Context): Boolean =
        sdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    /** Contrato para pedir permisos desde una Activity/Compose. */
    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    private fun client(context: Context): HealthConnectClient =
        HealthConnectClient.getOrCreate(context)

    suspend fun hasPermissions(context: Context): Boolean {
        if (!isAvailable(context)) return false
        val granted = client(context).permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    /**
     * Escribe [count] pasos en una ventana que termina ahora y empieza
     * [minutesWindow] minutos antes. Devuelve true si insertó.
     */
    suspend fun writeTestSteps(context: Context, count: Long, minutesWindow: Long = 10): Boolean {
        if (!isAvailable(context) || count <= 0) return false
        val end = Instant.now()
        val start = end.minus(minutesWindow.coerceAtLeast(1), ChronoUnit.MINUTES)
        val zone = ZoneId.systemDefault()
        val record = StepsRecord(
            count = count,
            startTime = start,
            startZoneOffset = zone.rules.getOffset(start),
            endTime = end,
            endZoneOffset = zone.rules.getOffset(end),
            metadata = Metadata()
        )
        client(context).insertRecords(listOf(record))
        return true
    }

    /**
     * Borra SOLAMENTE los StepsRecord escritos por esta app (filtra por DataOrigin
     * = nuestro package). Devuelve cuántos borró.
     */
    suspend fun deleteOwnSteps(context: Context, sinceDays: Long = 30): Int {
        if (!isAvailable(context)) return 0
        val end = Instant.now()
        val start = end.minus(sinceDays.coerceAtLeast(1), ChronoUnit.DAYS)
        val response = client(context).readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                dataOriginFilter = setOf(DataOrigin(context.packageName))
            )
        )
        val ids = response.records.map { it.metadata.id }.filter { it.isNotEmpty() }
        if (ids.isEmpty()) return 0
        client(context).deleteRecords(
            recordType = StepsRecord::class,
            recordIdsList = ids,
            clientRecordIdsList = emptyList()
        )
        return ids.size
    }
}
