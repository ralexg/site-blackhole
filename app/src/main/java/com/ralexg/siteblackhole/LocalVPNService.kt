package com.ralexg.siteblackhole

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

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
            // Configura a VPN para interceptar APENAS o tráfego de DNS (1.1.1.1)
            val builder = Builder()
                .addAddress("10.0.0.2", 32)
                .addRoute("1.1.1.1", 32) // Roteia apenas este IP para a VPN
                .addDnsServer("1.1.1.1") // Força o celular a usar este DNS
                .setSession("SiteBlackholeVPN")

            vpnInterface = builder.establish()
            Log.d("BlackholeVPN", "VPN Ativada! Escutando DNS...")

            val input = FileInputStream(vpnInterface?.fileDescriptor)
            val buffer = ByteArray(32767)

            while (!Thread.interrupted()) {
                val length = input.read(buffer)
                if (length > 0) {
                    val domain = DnsPacketHandler.extractDomainFromPacket(buffer, length)

                    if (domain != null) {
                        Log.d("BlackholeVPN", "Requisição DNS para: $domain")

                        val blockedSites = blocklistManager.getBlockedDomains()
                        val isBlocked = blockedSites.any { domain.contains(it) }

                        if (isBlocked) {
                            Log.d("BlackholeVPN", "BLOCKED: $domain jogado no buraco negro!")
                            // Não fazemos nada. O pacote morre aqui.
                            continue
                        } else {
                            // Site permitido! Num cenário real e completo, aqui
                            // repassaríamos o pacote para o 1.1.1.1 real.
                            // Por ser um projeto introdutório, estamos focando primeiro
                            // em identificar a interceptação com sucesso!
                            Log.d("BlackholeVPN", "ALLOWED: $domain")
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