package com.stepphantom.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Log de CONFIGURACIÓN in-app (proceso propio). Distinto del runtime del hook. */
object AppDiagnostics {
    private const val MAX = 150
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines

    fun log(msg: String) {
        _lines.value = (listOf("${fmt.format(Date())}  $msg") + _lines.value).take(MAX)
    }

    fun clear() { _lines.value = emptyList() }
}
