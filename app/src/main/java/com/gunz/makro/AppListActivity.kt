package com.gunz.makro

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)

        val rv: RecyclerView = findViewById(R.id.rvApps)
        rv.layoutManager = LinearLayoutManager(this)

        val apps = getInstalledLaunchableApps()
        rv.adapter = AppAdapter(apps) { selected ->
            confirmSelection(selected)
        }
    }

    private fun getInstalledLaunchableApps(): List<AppEntry> {
        val pm: PackageManager = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolvedApps: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)

        return resolvedApps
            .filter { it.activityInfo.packageName != packageName } // sembunyikan aplikasi ini sendiri
            .map { resolveInfo ->
                AppEntry(
                    packageName = resolveInfo.activityInfo.packageName,
                    label = resolveInfo.loadLabel(pm).toString(),
                    icon = resolveInfo.loadIcon(pm)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun confirmSelection(app: AppEntry) {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi")
            .setMessage("Gunakan \"${app.label}\" sebagai aplikasi target untuk Makro by Gunz?")
            .setPositiveButton("Konfirmasi") { _, _ ->
                Prefs.setTargetApp(this, app.packageName, app.label)
                setResult(Activity.RESULT_OK)
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
