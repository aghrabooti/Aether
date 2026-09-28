package com.cluvex.aether

import android.app.*
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import hev.htproxy.TProxyService
import org.json.JSONObject
import java.io.File
import kotlin.concurrent.thread

class AetherVpnService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    private var coreJob = 0L
    @Volatile private var stopping = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> disconnect()
            else -> if (coreJob == 0L) connect()
        }
        return START_NOT_STICKY
    }

    private fun connect() {
        stopping = false
        createChannel()
        startForeground(NOTIFICATION_ID, notification("در حال اتصال…"))
        state("connecting")
        thread(name = "aether-vpn") {
            try {
                val identity = File(filesDir, "aether-masque.toml").absolutePath
                coreJob = AetherNative.start("[\"--masque\",\"--scan\",\"balanced\",\"--masque-config\",${JSONObject.quote(identity)}]")
                require(coreJob > 0) { "هستهٔ Aether شروع نشد" }

                // The SOCKS listener starts immediately; endpoint discovery continues behind it.
                Thread.sleep(1200)
                if (stopping) return@thread
                tun = Builder()
                    .setSession("Aether")
                    .setMtu(1280)
                    .addAddress("198.18.0.1", 32)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("1.1.1.1")
                    // Aether and tun2socks share our UID. Excluding it keeps Aether's own
                    // endpoint sockets outside the VPN and prevents a routing loop.
                    .addDisallowedApplication(packageName)
                    .establish() ?: error("ساخت رابط VPN ممکن نشد")

                val config = File(cacheDir, "tun2socks.yml")
                config.writeText("""
                    tunnel:
                      mtu: 1280
                      ipv4: 198.18.0.1
                    socks5:
                      address: 127.0.0.1
                      port: 1819
                      udp: 'tcp'
                    misc:
                      log-level: warn
                """.trimIndent())
                require(TProxyService.TProxyStartService(config.absolutePath, tun!!.fd)) { "راه‌اندازی مسیر VPN ناموفق بود" }
                updateNotification("متصل")
                state("connected")

                while (!stopping) {
                    val reply = JSONObject(AetherNative.poll(coreJob))
                    if (!reply.optBoolean("ok", false)) error(reply.optString("error", "خطای هستهٔ Aether"))
                    if (reply.optString("state") == "done") {
                        val result = reply.optJSONObject("result")
                        if (result != null && !result.optBoolean("ok", false)) {
                            error(result.optString("error", "تونل متوقف شد"))
                        }
                        break
                    }
                    Thread.sleep(1000)
                }
            } catch (e: Throwable) {
                if (!stopping) state("error", e.message ?: "خطای ناشناخته")
            } finally {
                cleanup()
            }
        }
    }

    private fun disconnect() { stopping = true; cleanup(); state("stopped"); stopSelf() }

    @Synchronized private fun cleanup() {
        runCatching { TProxyService.TProxyStopService() }
        tun?.close(); tun = null
        if (coreJob != 0L) runCatching { AetherNative.cancel(coreJob) }
        coreJob = 0
    }

    override fun onDestroy() { stopping = true; cleanup(); super.onDestroy() }
    override fun onRevoke() { disconnect() }

    private fun state(value: String, message: String? = null) = sendBroadcast(
        Intent(ACTION_STATE).setPackage(packageName).putExtra(EXTRA_STATE, value).putExtra("message", message)
    )

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL, "وضعیت اتصال", NotificationManager.IMPORTANCE_LOW))
    }

    private fun notification(text: String) = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(android.R.drawable.stat_sys_warning).setContentTitle("Aether VPN")
        .setContentText(text).setOngoing(true)
        .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
        .build()

    private fun updateNotification(text: String) = getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))

    companion object {
        const val ACTION_CONNECT = "com.cluvex.aether.CONNECT"
        const val ACTION_DISCONNECT = "com.cluvex.aether.DISCONNECT"
        const val ACTION_STATE = "com.cluvex.aether.STATE"
        const val EXTRA_STATE = "state"
        private const val CHANNEL = "aether_connection"
        private const val NOTIFICATION_ID = 1819
    }
}
