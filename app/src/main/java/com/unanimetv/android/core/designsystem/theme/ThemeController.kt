package com.unanimetv.android.core.designsystem.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ThemeStyle { DEFAULT, OCEAN, FOREST, SUNSET, MONO, SAKURA }

val ThemeStyle.label: String
    get() = when (this) {
        ThemeStyle.DEFAULT -> "Default"
        ThemeStyle.OCEAN -> "Ocean"
        ThemeStyle.FOREST -> "Forest"
        ThemeStyle.SUNSET -> "Sunset"
        ThemeStyle.MONO -> "Mono"
        ThemeStyle.SAKURA -> "Sakura"
    }

data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val style: ThemeStyle = ThemeStyle.DEFAULT,
    val amoled: Boolean = false,
    val materialYou: Boolean = false
)

object ThemeController {

    private const val PREFS_NAME = "unanime_theme"

    @Volatile private var prefs: SharedPreferences? = null

    private val _settings = MutableStateFlow(ThemeSettings())
    val settings: StateFlow<ThemeSettings> = _settings.asStateFlow()

    fun init(context: Context) {
        val store = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = store
        val storedStyle = store.getString(KEY_STYLE, null)
        _settings.value = ThemeSettings(
            mode = store.getString(KEY_MODE, null)
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            style = storedStyle
                ?.let { runCatching { ThemeStyle.valueOf(it) }.getOrNull() }
                ?: ThemeStyle.DEFAULT,
            amoled = storedStyle == "AMOLED" || store.getBoolean(KEY_AMOLED, false),
            materialYou = store.getBoolean(KEY_MATERIAL_YOU, false)
        )
    }

    fun setMode(mode: ThemeMode) {
        prefs?.edit()?.putString(KEY_MODE, mode.name)?.apply()
        _settings.update { it.copy(mode = mode) }
    }

    fun setStyle(style: ThemeStyle) {
        prefs?.edit()?.putString(KEY_STYLE, style.name)?.apply()
        _settings.update { it.copy(style = style) }
    }

    fun setAmoled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_AMOLED, enabled)?.apply()
        _settings.update { it.copy(amoled = enabled) }
    }

    fun setMaterialYou(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_MATERIAL_YOU, enabled)?.apply()
        _settings.update { it.copy(materialYou = enabled) }
    }

    private const val KEY_MODE = "mode"
    private const val KEY_STYLE = "style"
    private const val KEY_AMOLED = "amoled"
    private const val KEY_MATERIAL_YOU = "material_you"
}
