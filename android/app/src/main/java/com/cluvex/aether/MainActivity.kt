package com.cluvex.aether

import android.Manifest
import android.app.Activity
import android.content.*
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var button: Button
    private var connected = false

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) startTunnel()
        else setState(false, "اجازهٔ VPN داده نشد")
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getStringExtra(AetherVpnService.EXTRA_STATE) ?: return
            when (state) {
                "connected" -> setState(true, "متصل شد")
                "connecting" -> setState(false, "در حال پیدا کردن مسیر امن…", false)
                "stopped" -> setState(false, "آمادهٔ اتصال")
                "error" -> setState(false, intent.getStringExtra("message") ?: "اتصال ناموفق بود")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        button = findViewById(R.id.connect)
        button.setOnClickListener { if (connected) stopTunnel() else requestVpn() }
        if (android.os.Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(receiver, IntentFilter(AetherVpnService.ACTION_STATE), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() { unregisterReceiver(receiver); super.onStop() }

    private fun requestVpn() {
        val intent = VpnService.prepare(this)
        if (intent == null) startTunnel() else vpnPermission.launch(intent)
    }

    private fun startTunnel() {
        setState(false, "در حال اتصال…", false)
        ContextCompat.startForegroundService(this, Intent(this, AetherVpnService::class.java).setAction(AetherVpnService.ACTION_CONNECT))
    }

    private fun stopTunnel() {
        startService(Intent(this, AetherVpnService::class.java).setAction(AetherVpnService.ACTION_DISCONNECT))
    }

    private fun setState(isConnected: Boolean, text: String, enabled: Boolean = true) {
        connected = isConnected
        status.text = text
        button.text = if (isConnected) "قطع اتصال" else "اتصال"
        button.isEnabled = enabled
    }
}
