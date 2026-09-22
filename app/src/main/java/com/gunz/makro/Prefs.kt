package com.gunz.makro

import android.content.Context

object Prefs {
    private const val NAME = "makro_by_gunz_prefs"

    private const val KEY_TARGET_PACKAGE = "target_package"
    private const val KEY_TARGET_LABEL = "target_label"
    private const val KEY_BUBBLE_X = "bubble_x"
    private const val KEY_BUBBLE_Y = "bubble_y"

    fun setTargetApp(context: Context, packageName: String, label: String) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_TARGET_PACKAGE, packageName)
            .putString(KEY_TARGET_LABEL, label)
            .apply()
    }

    fun getTargetPackage(context: Context): String? =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(KEY_TARGET_PACKAGE, null)

    fun getTargetLabel(context: Context): String? =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(KEY_TARGET_LABEL, null)

    fun saveBubblePosition(context: Context, x: Int, y: Int) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_BUBBLE_X, x)
            .putInt(KEY_BUBBLE_Y, y)
            .apply()
    }

    fun getBubbleX(context: Context, default: Int): Int =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getInt(KEY_BUBBLE_X, default)

    fun getBubbleY(context: Context, default: Int): Int =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getInt(KEY_BUBBLE_Y, default)
}
