package com.stepphantom.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Log de CONFIGURACIÓN visible en la app (proceso propio).
 *
 * Ojo con la expectativa: esto NO es el log del hook. El estado en runtime del
 * hook (último paquete hookeado, último sensor interceptado, errores del hook)
 * vive en el proceso de la app OBJETIVO, no acá, y se lee por el log de
 * LSPosed/Vector o por logcat filtrando "[StepPhantom]". Un proceso normal no
 * puede leer el logcat de otro en Android moderno sin permisos privilegiados,
 * así que no intentamos fingir que sí.
 */
object AppDiagnostics {
    private const val MAX = 120
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines

    fun log(msg: String) {
        val line = "${fmt.format(Date())}  $msg"
        _lines.value = (listOf(line) + _lines.value).take(MAX)
    }

    fun clear() { _lines.value = emptyList() }
}
