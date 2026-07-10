package com.stepphantom.config

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object DiagnosticsStore {
    private const val KEY_PREFIX = "diag_"
    private var prefs: android.content.SharedPreferences? = null
    private val _snapshots = MutableStateFlow<Map<String, String>>(emptyMap())
    val snapshots: StateFlow<Map<String, String>> = _snapshots

    @Synchronized fun ensure(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("stepphantom_diag", Context.MODE_PRIVATE)
        reload()
    }

    @Synchronized fun put(pkg: String, json: String) { prefs?.edit()?.putString(KEY_PREFIX + pkg, json)?.apply(); reload() }
    @Synchronized fun clear() { prefs?.edit()?.clear()?.apply(); _snapshots.value = emptyMap() }

    private fun reload() {
        val p = prefs ?: return
        val out = mutableMapOf<String, String>()
        p.all.forEach { (k, v) -> if (k.startsWith(KEY_PREFIX) && v is String) out[k.removePrefix(KEY_PREFIX)] = v }
        _snapshots.value = out
    }
}
