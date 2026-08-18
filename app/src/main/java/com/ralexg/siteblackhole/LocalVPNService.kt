package com.ralexg.siteblackhole

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream

class LocalVpnService : VpnService(), Runnable {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    private lateinit var blocklistManager: BlocklistManager

    override fun onCreate() {
        super.onCreate()
        blocklistManager = BlocklistManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (vpnThread == null) {
            vpnThread = Thread(this, "LocalVpnThread").apply { start() }
        }
        return START_STICKY
    }

    override fun run() {
        try {
            val builder = Builder()
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1") // DNS padrão para tráfego normal

            vpnInterface = builder.setSession("SiteBlackholeVPN").establish()
            Log.d("BlackholeVPN", "VPN Ativada com sucesso.")

            // Mantém a interface ativa
            val input = FileInputStream(vpnInterface?.fileDescriptor)
            val output = FileOutputStream(vpnInterface?.fileDescriptor)
            val buffer = ByteArray(32767)

            while (!Thread.interrupted()) {
                val length = input.read(buffer)
                if (length > 0) {
                    // O tráfego passa pela interface virtual
                    // Em iterativos futuros, interceptamos consultas DNS diretamente aqui
                }
                Thread.sleep(10)
            }

        } catch (e: Exception) {
            Log.e("BlackholeVPN", "Erro na VPN: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        vpnThread?.interrupt()
        vpnInterface?.close()
    }
}