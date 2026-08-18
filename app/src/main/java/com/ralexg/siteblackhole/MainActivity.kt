package com.ralexg.siteblackhole

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var blocklistManager: BlocklistManager
    private val VPN_REQUEST_CODE = 1001

    // Variável para a nossa lista visual
    private lateinit var listViewSites: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        blocklistManager = BlocklistManager(this)

        val inputDomain = findViewById<EditText>(R.id.edtDomain)
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val btnStartVpn = findViewById<Button>(R.id.btnStartVpn)
        listViewSites = findViewById(R.id.listViewSites)

        // Carrega a lista a primeira vez que o app abre
        atualizarListaDeSites()

        btnAdd.setOnClickListener {
            val domain = inputDomain.text.toString()
            if (domain.isNotBlank()) {
                blocklistManager.addDomain(domain)
                inputDomain.text.clear()
                Toast.makeText(this, "$domain bloqueado para sempre!", Toast.LENGTH_SHORT).show()

                // Atualiza a tela imediatamente após adicionar um novo site
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

    // Função que busca os sites e entrega para o Adapter exibir na tela
    private fun atualizarListaDeSites() {
        // Pega a lista do banco, transforma em List e ordena em ordem alfabética
        val sitesBloqueados = blocklistManager.getBlockedDomains().toList().sorted()

        // O ArrayAdapter é o "tradutor" que transforma os textos em linhas na tela
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1, // Layout padrão de lista do Android
            sitesBloqueados
        )

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