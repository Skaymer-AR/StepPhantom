package com.stepphantom.config

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import android.os.Process

class ConfigProvider : ContentProvider() {
    companion object {
        const val PATH = "config"
        val URI: Uri = Uri.parse("content://${StepPhantomConfig.CONFIG_AUTHORITY}/$PATH")
        const val COL_JSON = "json"
    }

    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val ctx = context ?: return MatrixCursor(arrayOf(COL_JSON))
        val cursor = MatrixCursor(arrayOf(COL_JSON))
        val json = ctx.getSharedPreferences(StepPhantomConfig.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(StepPhantomConfig.PREFS_KEY_JSON, null) ?: return cursor
        val uid = Binder.getCallingUid()
        if (uid != Process.myUid() && !callerIsSelected(ctx, uid, json)) return cursor
        cursor.addRow(arrayOf<Any?>(json))
        return cursor
    }

    private fun callerIsSelected(ctx: Context, uid: Int, configJson: String): Boolean {
        val pkgs = runCatching { ctx.packageManager.getPackagesForUid(uid) }.getOrNull() ?: return false
        val selected = StepPhantomConfig.fromJsonString(configJson).packages.keys
        return pkgs.any { it in selected }
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/vnd.stepphantom.config"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?): Int = 0
    override fun delete(uri: Uri, s: String?, a: Array<out String>?): Int = 0
}
