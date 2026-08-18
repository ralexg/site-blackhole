package com.ralexg.siteblackhole

object DnsBlockResponder {

    fun createBlockResponse(requestPacket: ByteArray, requestLength: Int): ByteArray {
        val ipHeaderLen = (requestPacket[0].toInt() and 0x0F) * 4
        val udpHeaderLen = 8
        val dnsOffset = ipHeaderLen + udpHeaderLen
        
        // A resposta DNS que aponta para 127.0.0.1 tem exatos 16 bytes a mais que a pergunta
        val answerBytes = byteArrayOf(
            0xC0.toByte(), 0x0C.toByte(), // Ponteiro para o nome (domínio) perguntado
            0x00, 0x01,                   // Tipo A (IPv4)
            0x00, 0x01,                   // Classe IN (Internet)
            0x00, 0x00, 0x00, 0x3C,       // TTL (Tempo de vida) = 60 segundos
            0x00, 0x04,                   // Tamanho do IP (4 bytes)
            127, 0, 0, 1                  // O IP Falso: 127.0.0.1
        )

        val totalLen = requestLength + answerBytes.size
        val responsePacket = ByteArray(totalLen)
        
        // 1. Copia o pacote original inteiro
        System.arraycopy(requestPacket, 0, responsePacket, 0, requestLength)
        
        // 2. Anexa a nossa resposta falsa no final
        System.arraycopy(answerBytes, 0, responsePacket, requestLength, answerBytes.size)

        // 3. Inverte IP e Porta (Origem vira Destino e vice-versa)
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

        // 4. Modifica o Cabeçalho DNS para dizer "Sou uma Resposta Válida"
        responsePacket[dnsOffset + 2] = 0x81.toByte() // Flags: Resposta Padrão
        responsePacket[dnsOffset + 3] = 0x80.toByte() // Flags: Sem erros
        responsePacket[dnsOffset + 6] = 0x00.toByte() // Quantidade de respostas: 1
        responsePacket[dnsOffset + 7] = 0x01.toByte() 

        // 5. Atualiza os tamanhos no cabeçalho IP e UDP
        responsePacket[2] = (totalLen shr 8).toByte()
        responsePacket[3] = (totalLen and 0xFF).toByte()
        val udpLen = udpHeaderLen + (requestLength - dnsOffset) + answerBytes.size
        responsePacket[ipHeaderLen + 4] = (udpLen shr 8).toByte()
        responsePacket[ipHeaderLen + 5] = (udpLen and 0xFF).toByte()

        // 6. Recalcula o Checksum do IP (Truque de redes)
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
    }
}
