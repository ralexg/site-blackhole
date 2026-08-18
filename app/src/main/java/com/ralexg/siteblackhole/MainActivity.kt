package com.ralexg.siteblackhole

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var blocklistManager: BlocklistManager
    private val VPN_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        blocklistManager = BlocklistManager(this)

        val inputDomain = findViewById<EditText>(R.id.edtDomain)
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val btnStartVpn = findViewById<Button>(R.id.btnStartVpn)

        btnAdd.setOnClickListener {
            val domain = inputDomain.text.toString()
            if (domain.isNotBlank()) {
                blocklistManager.addDomain(domain)
                inputDomain.text.clear()
                Toast.makeText(this, "$domain bloqueado para sempre!", Toast.LENGTH_SHORT).show()
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