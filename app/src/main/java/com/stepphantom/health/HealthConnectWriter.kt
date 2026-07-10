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

object HealthConnectWriter {
    val permissions: Set<String> = setOf(
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )
    fun sdkStatus(context: Context): Int = HealthConnectClient.getSdkStatus(context)
    fun isAvailable(context: Context): Boolean = sdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    fun permissionContract() = PermissionController.createRequestPermissionResultContract()
    private fun client(context: Context): HealthConnectClient = HealthConnectClient.getOrCreate(context)
    suspend fun hasPermissions(context: Context): Boolean {
        if (!isAvailable(context)) return false
        return client(context).permissionController.getGrantedPermissions().containsAll(permissions)
    }
    suspend fun writeTestSteps(context: Context, count: Long, minutesWindow: Long = 10): Boolean {
        if (!isAvailable(context) || count <= 0) return false
        val end = Instant.now(); val start = end.minus(minutesWindow.coerceAtLeast(1), ChronoUnit.MINUTES); val zone = ZoneId.systemDefault()
        val record = StepsRecord(count = count, startTime = start, startZoneOffset = zone.rules.getOffset(start), endTime = end, endZoneOffset = zone.rules.getOffset(end), metadata = Metadata.manualEntry())
        client(context).insertRecords(listOf(record)); return true
    }
    suspend fun deleteOwnSteps(context: Context, sinceDays: Long = 30): Int {
        if (!isAvailable(context)) return 0
        val end = Instant.now(); val start = end.minus(sinceDays.coerceAtLeast(1), ChronoUnit.DAYS)
        val response = client(context).readRecords(ReadRecordsRequest(recordType = StepsRecord::class, timeRangeFilter = TimeRangeFilter.between(start, end), dataOriginFilter = setOf(DataOrigin(context.packageName))))
        val ids = response.records.map { it.metadata.id }.filter { it.isNotEmpty() }
        if (ids.isEmpty()) return 0
        client(context).deleteRecords(recordType = StepsRecord::class, recordIdsList = ids, clientRecordIdsList = emptyList())
        return ids.size
    }
}
