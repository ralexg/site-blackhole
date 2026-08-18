package com.ralexg.siteblackhole

import android.net.VpnService
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object DnsForwarder {

    fun forwardAndBuildResponse(requestPacket: ByteArray, requestLength: Int, vpnService: VpnService): ByteArray? {
        try {
            val ipHeaderLen = (requestPacket[0].toInt() and 0x0F) * 4
            val udpHeaderLen = 8
            val headerLen = ipHeaderLen + udpHeaderLen
            
            val dnsPayloadLen = requestLength - headerLen
            val dnsPayload = ByteArray(dnsPayloadLen)
            System.arraycopy(requestPacket, headerLen, dnsPayload, 0, dnsPayloadLen)

            val receiveData = ByteArray(1024)
            var responsePayloadLen = 0

            // O bloco '.use' garante o socket.close() automaticamente no final ou em caso de erro!
            DatagramSocket().use { socket ->
                vpnService.protect(socket) 
                socket.soTimeout = 3000
                
                val serverAddress = InetAddress.getByName("1.1.1.1")
                val outPacket = DatagramPacket(dnsPayload, dnsPayload.size, serverAddress, 53)
                socket.send(outPacket)

                val inPacket = DatagramPacket(receiveData, receiveData.size)
                socket.receive(inPacket)
                responsePayloadLen = inPacket.length
            }

            val totalLen = headerLen + responsePayloadLen
            val responsePacket = ByteArray(totalLen)
            
            System.arraycopy(requestPacket, 0, responsePacket, 0, headerLen)
            System.arraycopy(receiveData, 0, responsePacket, headerLen, responsePayloadLen)
            
            for (i in 0..3) {
                val temp = responsePacket[12 + i]
                responsePacket[12 + i] = responsePacket[16 + i]
                responsePacket[16 + i] = temp
            }
            
            for (i in 0..1) {
                val temp = responsePacket[ipHeaderLen + i]
                responsePacket[ipHeaderLen + i] = responsePacket[ipHeaderLen + 2 + i]
                responsePacket[ipHeaderLen + 2 + i] = temp
            }
            
            responsePacket[2] = (totalLen shr 8).toByte()
            responsePacket[3] = (totalLen and 0xFF).toByte()
            val udpLen = udpHeaderLen + responsePayloadLen
            responsePacket[ipHeaderLen + 4] = (udpLen shr 8).toByte()
            responsePacket[ipHeaderLen + 5] = (udpLen and 0xFF).toByte()
            
            responsePacket[ipHeaderLen + 6] = 0
            responsePacket[ipHeaderLen + 7] = 0
            responsePacket[10] = 0
            responsePacket[11] = 0
            
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
