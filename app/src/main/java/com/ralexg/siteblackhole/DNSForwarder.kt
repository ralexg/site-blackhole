package com.ralexg.siteblackhole

import android.net.VpnService
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object DnsForwarder {

    fun forwardAndBuildResponse(requestPacket: ByteArray, requestLength: Int, vpnService: VpnService): ByteArray? {
        try {
            // 1. Entendendo o tamanho do cabeçalho IP e UDP original
            val ipHeaderLen = (requestPacket[0].toInt() and 0x0F) * 4
            val udpHeaderLen = 8
            val headerLen = ipHeaderLen + udpHeaderLen

            val dnsPayloadLen = requestLength - headerLen
            val dnsPayload = ByteArray(dnsPayloadLen)
            System.arraycopy(requestPacket, headerLen, dnsPayload, 0, dnsPayloadLen)

            // 2. Encaminhando para o DNS real (1.1.1.1)
            val socket = DatagramSocket()
            // CRÍTICO: Evita que a própria VPN capture esse pacote e crie um loop infinito
            vpnService.protect(socket)

            socket.soTimeout = 3000
            val serverAddress = InetAddress.getByName("1.1.1.1")
            val outPacket = DatagramPacket(dnsPayload, dnsPayload.size, serverAddress, 53)
            socket.send(outPacket)

            // 3. Recebendo a resposta do servidor real
            val receiveData = ByteArray(1024)
            val inPacket = DatagramPacket(receiveData, receiveData.size)
            socket.receive(inPacket)
            socket.close()

            val responsePayloadLen = inPacket.length
            val totalLen = headerLen + responsePayloadLen
            val responsePacket = ByteArray(totalLen)

            // 4. Montando o pacote de resposta falso (Copiando cabeçalhos originais e nova resposta)
            System.arraycopy(requestPacket, 0, responsePacket, 0, headerLen)
            System.arraycopy(receiveData, 0, responsePacket, headerLen, responsePayloadLen)

            // 5. Invertendo IP de Origem e Destino
            for (i in 0..3) {
                val temp = responsePacket[12 + i]
                responsePacket[12 + i] = responsePacket[16 + i]
                responsePacket[16 + i] = temp
            }

            // 6. Invertendo Portas de Origem e Destino
            for (i in 0..1) {
                val temp = responsePacket[ipHeaderLen + i]
                responsePacket[ipHeaderLen + i] = responsePacket[ipHeaderLen + 2 + i]
                responsePacket[ipHeaderLen + 2 + i] = temp
            }

            // 7. Atualizando tamanhos no cabeçalho
            responsePacket[2] = (totalLen shr 8).toByte()
            responsePacket[3] = (totalLen and 0xFF).toByte()
            val udpLen = udpHeaderLen + responsePayloadLen
            responsePacket[ipHeaderLen + 4] = (udpLen shr 8).toByte()
            responsePacket[ipHeaderLen + 5] = (udpLen and 0xFF).toByte()

            // 8. O truque do Checksum: No IPv4, Checksum UDP = 0 significa "ignorar"
            responsePacket[ipHeaderLen + 6] = 0
            responsePacket[ipHeaderLen + 7] = 0
            responsePacket[10] = 0
            responsePacket[11] = 0

            // Calculando apenas o Checksum do Cabeçalho IP
            var sum = 0
            for (i in 0 until ipHeaderLen step 2) {
                val word = ((responsePacket[i].toInt() and 0xFF) shl 8) or (responsePacket[i + 1].toInt() and 0xFF)
                sum += word
            }
            while ((sum shr 16) > 0) {
                sum = (sum and 0xFFFF) + (sum shr 16)
            }
            val checksum = sum.inv() and 0xFFFF
            responsePacket[10] = (checksum shr 8).toByte()
            responsePacket[11] = (checksum and 0xFF).toByte()

            return responsePacket
        } catch (e: Exception) {
            return null
        }
    }
}