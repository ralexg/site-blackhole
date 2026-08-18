package com.ralexg.siteblackhole

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.compareTo

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
                .addRoute("1.1.1.1", 32)
                .addDnsServer("1.1.1.1")
                .setSession("SiteBlackholeVPN")

            vpnInterface = builder.establish()
            Log.d("BlackholeVPN", "VPN Ativada! Escutando DNS...")

            val input = FileInputStream(vpnInterface?.fileDescriptor)
            // ADICIONAMOS O OUTPUT AQUI:
            val output = FileOutputStream(vpnInterface?.fileDescriptor)
            val buffer = ByteArray(32767)

            while (!Thread.interrupted()) {
                val length = input.read(buffer)
                if (length > 0) {
                    val domain = DnsPacketHandler.extractDomainFromPacket(buffer, length)

                    if (domain != null) {
                        val blockedSites = blocklistManager.getBlockedDomains()
                        val isBlocked = blockedSites.any { domain.contains(it) }

                        if (isBlocked) {
                            Log.d("BlackholeVPN", "BLOCKED: $domain jogado no buraco negro!")
                            // Fica no buraco negro (não faz nada)
                            continue
                        } else {
                            Log.d("BlackholeVPN", "ALLOWED: $domain - Buscando resposta...")
                            // Envia para o DNS real, pega a resposta e escreve de volta no celular
                            val response = DnsForwarder.forwardAndBuildResponse(buffer, length, this)
                            if (response != null) {
                                output.write(response)
                            }
                        }
                    }
                }
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
