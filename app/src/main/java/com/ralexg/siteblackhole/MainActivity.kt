package com.ralexg.siteblackhole

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {

    private lateinit var blocklistManager: BlocklistManager
    private val VPN_REQUEST_CODE = 1001
    private lateinit var listViewSites: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Carrega o tema salvo antes de desenhar a tela
        aplicarTemaSalvo()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        blocklistManager = BlocklistManager(this)

        val inputDomain = findViewById<EditText>(R.id.edtDomain)
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val btnStartVpn = findViewById<Button>(R.id.btnStartVpn)
        val btnTheme = findViewById<Button>(R.id.btnTheme)
        listViewSites = findViewById(R.id.listViewSites)

        atualizarListaDeSites()

        btnTheme.setOnClickListener {
            mostrarDialogoDeTema()
        }

        btnAdd.setOnClickListener {
            val domain = inputDomain.text.toString()
            if (domain.isNotBlank()) {
                blocklistManager.addDomain(domain)
                inputDomain.text.clear()
                Toast.makeText(this, "$domain bloqueado para sempre!", Toast.LENGTH_SHORT).show()
                atualizarListaDeSites()
            }
        }

        btnStartVpn.setOnClickListener {
            val intent = VpnService.prepare(this)
            if (intent != null) {
                startActivityForResult(intent, VPN_REQUEST_CODE)
            } else {
                startVpnService()
            }
        }
    }

    private fun aplicarTemaSalvo() {
        val prefs = getSharedPreferences("config_app", Context.MODE_PRIVATE)
        val temaSalvo = prefs.getInt("modo_tema", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(temaSalvo)
    }

    private fun mostrarDialogoDeTema() {
        val opcoes = arrayOf("Sistema", "Claro", "Escuro")
        val prefs = getSharedPreferences("config_app", Context.MODE_PRIVATE)
        val temaAtual = prefs.getInt("modo_tema", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        val itemSelecionado = when (temaAtual) {
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            else -> 0
        }

        AlertDialog.Builder(this)
            .setTitle("Escolher Tema")
            .setSingleChoiceItems(opcoes, itemSelecionado) { dialog, qual ->
                val modo = when (qual) {
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }

                // Salva a escolha e aplica na hora
                prefs.edit().putInt("modo_tema", modo).apply()
                AppCompatDelegate.setDefaultNightMode(modo)
                dialog.dismiss()
            }
            .show()
    }

    private fun atualizarListaDeSites() {
        val sitesBloqueados = blocklistManager.getBlockedDomains().toList().sorted()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, sitesBloqueados)
        listViewSites.adapter = adapter
    }

    private fun startVpnService() {
        val intent = Intent(this, LocalVpnService::class.java)
        startService(intent)
        Toast.makeText(this, "Bloqueador Ativado!", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startVpnService()
        }
    }
}