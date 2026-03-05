package io.qrx.scan.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("qrx_preferences", Context.MODE_PRIVATE)

    var copyWithoutLineBreaks: Boolean
        get() = prefs.getBoolean(KEY_COPY_WITHOUT_LINE_BREAKS, true)
        set(value) = prefs.edit().putBoolean(KEY_COPY_WITHOUT_LINE_BREAKS, value).apply()

    fun getCopySeparator(): String = "\n"

    companion object {
        private const val KEY_COPY_WITHOUT_LINE_BREAKS = "copy_without_line_breaks"
    }
}
