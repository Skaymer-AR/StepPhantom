package com.stepphantom.ui

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Preferencias de UI (idioma, color dinámico). Separadas de la config del módulo. */
class UiPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("stepphantom_ui", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(loadLang())
    val language: StateFlow<Lang> = _language

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC, true))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor

    fun setLanguage(lang: Lang) {
        prefs.edit().putString(KEY_LANG, lang.name).apply()
        _language.value = lang
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC, enabled).apply()
        _dynamicColor.value = enabled
    }

    private fun loadLang(): Lang =
        runCatching { Lang.valueOf(prefs.getString(KEY_LANG, Lang.ES.name)!!) }.getOrDefault(Lang.ES)

    companion object {
        private const val KEY_LANG = "lang"
        private const val KEY_DYNAMIC = "dynamic_color"
    }
}
