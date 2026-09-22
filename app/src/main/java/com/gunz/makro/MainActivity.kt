package com.gunz.makro

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: SelectedAppAdapter
    private val selectedApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rvSelectedApps = findViewById<RecyclerView>(R.id.rvSelectedApps)
        rvSelectedApps.layoutManager = LinearLayoutManager(this)
        
        adapter = SelectedAppAdapter(
            apps = selectedApps,
            onAddClick = {
                val intent = Intent(this, AppListActivity::class.java)
                startActivity(intent)
            },
            onRemoveClick = { appInfo ->
                Prefs.removeApp(this, appInfo.packageName)
                loadSelectedApps()
            }
        )
        rvSelectedApps.adapter = adapter

        findViewById<MaterialButton>(R.id.btnStartService).setOnClickListener {
            if (checkOverlayPermission()) {
                startFloatingService()
            } else {
                requestOverlayPermission()
            }
        }

        findViewById<MaterialButton>(R.id.btnSpeedSettings).setOnClickListener {
            startActivity(Intent(this, SpeedSettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadSelectedApps()
    }

    private fun loadSelectedApps() {
        selectedApps.clear()
        val savedPackages = Prefs.getSelectedApps(this)
        val pm = packageManager

        for (pkg in savedPackages) {
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val appName = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                selectedApps.add(AppInfo(appName, pkg, icon))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 1234)
        }
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Service Dimulai", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1234) {
            if (checkOverlayPermission()) {
                startFloatingService()
            } else {
                Toast.makeText(this, "Izin Overlay Diperlukan!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
