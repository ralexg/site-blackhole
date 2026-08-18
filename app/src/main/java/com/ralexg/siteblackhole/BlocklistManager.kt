package com.ralexg.siteblackhole

import android.content.Context
import android.content.SharedPreferences

class BlocklistManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("blackhole_prefs", Context.MODE_PRIVATE)

    // Recupera a lista de domínios bloqueados
    fun getBlockedDomains(): Set<String> {
        return prefs.getStringSet("blocked_sites", emptySet()) ?: emptySet()
    }

    // Adiciona um novo domínio (NÃO EXISTE método para remover)
    fun addDomain(domain: String) {
        val cleanDomain = domain.trim().lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")

        if (cleanDomain.isNotEmpty()) {
            val currentList = getBlockedDomains().toMutableSet()
            currentList.add(cleanDomain)
            prefs.edit().putStringSet("blocked_sites", currentList).apply()
        }
    }
}