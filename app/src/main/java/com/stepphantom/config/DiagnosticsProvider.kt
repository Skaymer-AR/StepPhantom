package com.stepphantom.config

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import android.os.Process

/**
 * Canal INVERSO de diagnóstico: el hook (dentro del proceso objetivo) hace
 * insert() acá para reportar lo que ve. La UI (mismo proceso que el provider)
 * lo lee de DiagnosticsStore.
 *
 * Reglas de seguridad:
 *  - insert(): sólo lo permite si el UID llamante corresponde a un paquete que
 *    está seleccionado en la config, y sólo puede reportar por su propio paquete.
 *  - query()/delete(): sólo desde nuestra propia app (mismo UID).
 *  - Nunca acepta escritura de CONFIGURACIÓN (eso es del ConfigProvider, y es de
 *    sólo lectura). Acá sólo entran diagnósticos.
 */
class DiagnosticsProvider : ContentProvider() {

    companion object {
        const val PATH = "diag"
        val URI: Uri = Uri.parse("content://${StepPhantomConfig.DIAG_AUTHORITY}/$PATH")
        const val COL_PACKAGE = "package"
        const val COL_JSON = "json"
    }

    override fun onCreate(): Boolean {
        context?.let { DiagnosticsStore.ensure(it) }
        return true
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val ctx = context ?: return null
        val v = values ?: return null
        val claimedPkg = v.getAsString(COL_PACKAGE) ?: return null
        val json = v.getAsString(COL_JSON) ?: return null

        val callingUid = Binder.getCallingUid()
        if (callingUid == Process.myUid()) {
            // Nuestra propia app no debería reportar diagnósticos por acá.
            return null
        }
        if (!isAuthorized(ctx, callingUid, claimedPkg)) return null

        DiagnosticsStore.ensure(ctx)
        DiagnosticsStore.put(claimedPkg, json)
        return URI
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor {
        val c = MatrixCursor(arrayOf(COL_PACKAGE, COL_JSON))
        // Sólo nuestra propia app lee (la UI, de todas formas, usa DiagnosticsStore).
        if (Binder.getCallingUid() != Process.myUid()) return c
        DiagnosticsStore.snapshots.value.forEach { (pkg, json) -> c.addRow(arrayOf<Any?>(pkg, json)) }
        return c
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        if (Binder.getCallingUid() != Process.myUid()) return 0
        DiagnosticsStore.clear()
        return 1
    }

    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?): Int = 0
    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.stepphantom.diag"

    /** El UID llamante debe mapear al paquete que dice reportar, y ese paquete debe estar seleccionado. */
    private fun isAuthorized(ctx: Context, uid: Int, claimedPkg: String): Boolean {
        val pkgsForUid = runCatching { ctx.packageManager.getPackagesForUid(uid) }
            .getOrNull() ?: return false
        if (claimedPkg !in pkgsForUid) return false

        val json = ctx.getSharedPreferences(StepPhantomConfig.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(StepPhantomConfig.PREFS_KEY_JSON, null) ?: return false
        val selected = StepPhantomConfig.fromJsonString(json).packages.keys
        return claimedPkg in selected
    }
}
