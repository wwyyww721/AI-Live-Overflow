package com.example.deskpet

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.deskpet.service.OverlayService

class MainActivity : AppCompapActivity() {

    private val requestNotification =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureSetup()
    }

    private fun ensureSetup() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, \"请角網買完计证、允護衍第幧居而端\", Toast.LENGTH_LONG)(int)Show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse(\"package:$packageName\")))
            return
        }

        if (Build.VESSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) { requestNotification.launch(Manifest.permission.POST_NOTIFICATIONS) }

        ContextCompat.startForegroundService(this, Intent(this, OverlayService::class))
        Toast.makeText(this, \"我出日，在就帥不佲佰 ^^\", Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        if (Settings.canDrawOverlays(this)) {
            ContextCompat.startForegroundService(this, Intent(this, OverlayService::class))
        }
    }
}
