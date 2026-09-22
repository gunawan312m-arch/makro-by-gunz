package com.gunz.makro

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnAktivasi: Button
    private lateinit var btnPilihApp: Button
    private lateinit var btnMulai: Button
    private lateinit var tvStatusAksesibilitas: TextView
    private lateinit var tvAppTerpilih: TextView

    companion object {
        const val REQ_APP_PICKER = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnAktivasi = findViewById(R.id.btnAktivasi)
        btnPilihApp = findViewById(R.id.btnPilihApp)
        btnMulai = findViewById(R.id.btnMulai)
        tvStatusAksesibilitas = findViewById(R.id.tvStatusAksesibilitas)
        tvAppTerpilih = findViewById(R.id.tvAppTerpilih)

        btnAktivasi.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnPilihApp.setOnClickListener {
            startActivityForResult(Intent(this, AppListActivity::class.java), REQ_APP_PICKER)
        }

        btnMulai.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Aktifkan dulu layanan aksesibilitas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                Toast.makeText(this, "Izinkan tampil di atas aplikasi lain, lalu tekan Mulai lagi", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            startService(Intent(this, FloatingService::class.java))
            Toast.makeText(this, "Mode mengambang aktif", Toast.LENGTH_SHORT).show()
        }

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_APP_PICKER) {
            refreshUi()
        }
    }

    private fun refreshUi() {
        val aksesibilitasAktif = isAccessibilityServiceEnabled()
        tvStatusAksesibilitas.text = if (aksesibilitasAktif) "Status: aktif" else "Status: belum aktif"

        val label = Prefs.getTargetLabel(this)
        tvAppTerpilih.text = if (label != null) "Aplikasi target: $label" else "Belum ada aplikasi dipilih"

        btnMulai.isEnabled = aksesibilitasAktif && label != null
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = ComponentName(this, MacroAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            val componentName = ComponentName.unflattenFromString(splitter.next())
            if (componentName != null && componentName == expectedComponent) {
                return true
            }
        }
        return false
    }
}
