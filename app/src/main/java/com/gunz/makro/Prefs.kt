package com.gunz.makro

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class TargetApp(val packageName: String, val label: String)

object Prefs {
    private const val NAME = "makro_by_gunz_prefs"

    private const val KEY_TARGET_APPS = "target_apps_json"
    private const val KEY_BUBBLE_X = "bubble_x"
    private const val KEY_BUBBLE_Y = "bubble_y"
    private const val KEY_TAP_INTERVAL_MS = "tap_interval_ms"

    // Kecepatan default: 150ms antar ketukan
    private const val DEFAULT_INTERVAL_MS = 150

    // ---------- Daftar aplikasi target (bisa lebih dari satu) ----------

    fun getTargetApps(context: Context): List<TargetApp> {
        val raw = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_TARGET_APPS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                TargetApp(obj.getString("pkg"), obj.getString("label"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveTargetApps(context: Context, apps: List<TargetApp>) {
        val arr = JSONArray()
        apps.forEach { app ->
            val obj = JSONObject()
            obj.put("pkg", app.packageName)
            obj.put("label", app.label)
            arr.put(obj)
        }
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_TARGET_APPS, arr.toString())
            .apply()
    }

    fun addTargetApp(context: Context, packageName: String, label: String): Boolean {
        val current = getTargetApps(context).toMutableList()
        if (current.any { it.packageName == packageName }) return false
        current.add(TargetApp(packageName, label))
        saveTargetApps(context, current)
        return true
    }

    fun replaceTargetApp(context: Context, oldPackage: String, newPackage: String, newLabel: String) {
        val current = getTargetApps(context).toMutableList()
        val index = current.indexOfFirst { it.packageName == oldPackage }
        if (index >= 0) {
            current[index] = TargetApp(newPackage, newLabel)
            saveTargetApps(context, current)
        }
    }

    fun removeTargetApp(context: Context, packageName: String) {
        val current = getTargetApps(context).filter { it.packageName != packageName }
        saveTargetApps(context, current)
    }

    fun reorderTargetApps(context: Context, newOrder: List<TargetApp>) {
        saveTargetApps(context, newOrder)
    }

    // ---------- Posisi bubble mengambang ----------

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

    // ---------- Kecepatan ketuk otomatis ----------
    // progress slider 0..100 -> interval ms (100 = paling cepat/20ms, 0 = paling lambat/1000ms)

    fun sliderProgressToIntervalMs(progress: Int): Int {
        val clamped = progress.coerceIn(0, 100)
        val maxMs = 1000
        val minMs = 20
        return maxMs - ((maxMs - minMs) * clamped / 100)
    }

    fun intervalMsToSliderProgress(intervalMs: Int): Int {
        val maxMs = 1000
        val minMs = 20
        val clamped = intervalMs.coerceIn(minMs, maxMs)
        return 100 - ((clamped - minMs) * 100 / (maxMs - minMs))
    }

    fun setTapIntervalMs(context: Context, intervalMs: Int) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_TAP_INTERVAL_MS, intervalMs)
            .apply()
    }

    fun getTapIntervalMs(context: Context): Int =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getInt(KEY_TAP_INTERVAL_MS, DEFAULT_INTERVAL_MS)
}
