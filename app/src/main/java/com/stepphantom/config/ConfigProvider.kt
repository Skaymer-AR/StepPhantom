package com.stepphantom.config

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * Puente entre procesos.
 *
 * PROBLEMA: el hook corre DENTRO del proceso de la app objetivo (otro UID).
 * No comparte las SharedPreferences de esta app. En Android moderno el viejo
 * truco de MODE_WORLD_READABLE está muerto (lanza SecurityException desde N).
 *
 * SOLUCIÓN elegida como principal: un ContentProvider exportado que devuelve
 * la config como JSON. Es independiente de la versión de Android y no depende
 * de detalles internos de LSPosed/Vector. El hook consulta
 *   content://com.stepphantom.config/config
 * y lee la columna "json".
 *
 * SEGURIDAD (razonable, no perfecta): está exported=true porque el proceso
 * consumidor es la app objetivo, que NO tiene ningún permiso custom nuestro,
 * así que no podríamos protegerlo con un permiso normal sin bloquearla. El dato
 * expuesto es sólo configuración de simulación de pasos (no hay secretos). Si
 * querés endurecerlo, mirá la nota al pie de este archivo.
 */
class ConfigProvider : ContentProvider() {

    companion object {
        const val PATH_CONFIG = "config"
        val CONFIG_URI: Uri =
            Uri.parse("content://${ModuleConfig.AUTHORITY}/$PATH_CONFIG")
        const val COLUMN_JSON = "json"
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val ctx: Context = context ?: return MatrixCursor(arrayOf(COLUMN_JSON))
        val prefs = ctx.getSharedPreferences(ModuleConfig.PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(ModuleConfig.PREFS_KEY_JSON, null)
            ?: ModuleConfig().toJsonString()

        return MatrixCursor(arrayOf(COLUMN_JSON)).apply {
            addRow(arrayOf<Any?>(json))
        }
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/vnd.stepphantom.config"

    // Sólo lectura: el hook nunca escribe config.
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, s: String?, a: Array<out String>?): Int = 0
    override fun delete(uri: Uri, s: String?, a: Array<out String>?): Int = 0
}

/*
 * NOTA de endurecimiento (opcional):
 * Si más adelante querés que sólo TUS paquetes objetivo puedan leer, podés:
 *   1) Definir un permiso <permission android:protectionLevel="signature"/> y
 *      firmar app + objetivos con la misma clave (poco práctico para apps de terceros).
 *   2) Verificar el UID llamante en query() con Binder.getCallingUid() y
 *      resolver su packageName contra una allowlist. Es defensa en profundidad,
 *      no una barrera fuerte, pero filtra curiosos.
 * Para testing local sobre apps propias, exported=true simple alcanza.
 */
