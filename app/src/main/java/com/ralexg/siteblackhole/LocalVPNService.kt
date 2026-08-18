package com.ralexg.siteblackhole

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.Executors

class LocalVpnService : VpnService(), Runnable {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    private lateinit var blocklistManager: BlocklistManager
    
    // Cria um "pool" de trabalhadores para resolver os DNS em paralelo
    private val executorService = Executors.newCachedThreadPool()

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
                            Log.d("BlackholeVPN", "BLOCKED: $domain - Retornando 127.0.0.1")
                            val fakeResponse = DnsBlockResponder.createBlockResponse(buffer, length)
                            output.write(fakeResponse)
                        } else {
                            Log.d("BlackholeVPN", "ALLOWED: $domain - Buscando resposta assíncrona...")
                            
                            // CRÍTICO: Copiamos o pacote atual porque a variável 'buffer' 
                            // será sobrescrita pela próxima requisição instantaneamente!
                            val requestCopy = buffer.copyOfRange(0, length)
                            
                            // Manda o pacote para um trabalhador em segundo plano
                            executorService.execute {
                                val response = DnsForwarder.forwardAndBuildResponse(requestCopy, length, this@LocalVpnService)
                                if (response != null) {
                                    // Bloqueia o output rapidinho só para garantir que dois 
                                    // trabalhadores não tentem escrever ao mesmo tempo e embaralhem os bytes
                                    synchronized(output) {
                                        output.write(response)
                                    }
                                }
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
        executorService.shutdownNow() // Desliga os trabalhadores ao fechar
        vpnInterface?.close()
    }
}
