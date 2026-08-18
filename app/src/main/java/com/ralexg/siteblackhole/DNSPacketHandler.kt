package com.ralexg.siteblackhole

import java.nio.ByteBuffer

object DnsPacketHandler {

    // Extrai o domínio de um pacote DNS bruto (IP + UDP + DNS Payload)
    fun extractDomainFromPacket(packet: ByteArray, length: Int): String? {
        try {
            // Verifica se é um pacote IPv4
            if (length < 28 || (packet[0].toInt() shr 4) != 4) return null

            // O cabeçalho IPv4 tem tamanho variável, geralmente 20 bytes
            val ipHeaderLen = (packet[0].toInt() and 0x0F) * 4
            // O cabeçalho UDP tem 8 bytes
            val udpHeaderLen = 8
            val dnsPayloadOffset = ipHeaderLen + udpHeaderLen

            if (length <= dnsPayloadOffset + 12) return null // Muito curto para ser DNS

            // Lê o domínio do DNS (QNAME)
            var offset = dnsPayloadOffset + 12
            val domainBuilder = StringBuilder()

            while (offset < length) {
                val labelLen = packet[offset].toInt() and 0xFF
                if (labelLen == 0) break // Fim do nome

                // Pula ponteiros DNS (compressão)
                if ((labelLen and 0xC0) == 0xC0) break

                offset++
                for (i in 0 until labelLen) {
                    if (offset < length) {
                        domainBuilder.append(packet[offset].toInt().toChar())
                        offset++
                    }
                }
                domainBuilder.append(".")
            }

            return domainBuilder.toString().trimEnd('.').lowercase()
        } catch (e: Exception) {
            return null
        }
    }
}