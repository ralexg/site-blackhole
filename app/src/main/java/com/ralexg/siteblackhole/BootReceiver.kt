package com.ralexg.siteblackhole

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.d("BlackholeVPN", "Celular reiniciado. Tentando ligar a VPN...")

            // VpnService.prepare retorna nulo se o usuário já deu a permissão antes
            if (VpnService.prepare(context) == null) {
                val vpnIntent = Intent(context, LocalVpnService::class.java)
                
                // Em versões mais novas do Android, serviços iniciados 
                // em segundo plano exigem um tratamento especial
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(vpnIntent)
                } else {
                    context.startService(vpnIntent)
                }
            } else {
                Log.e("BlackholeVPN", "Permissão da VPN foi revogada!")
            }
        }
    }
}
