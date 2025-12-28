package com.example.meintagebuch

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer

class PartnerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partner)

        // Toolbar einrichten
        val toolbar: Toolbar = findViewById(R.id.partnerToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.partner_title)

        val db = AppDatabase.getDatabase(this)

        val inviteButton: Button = findViewById(R.id.inviteButton)
        val inviteListText: TextView = findViewById(R.id.inviteListText)
        val partnerStatusText: TextView = findViewById(R.id.partnerStatusText)

        // Einladung erstellen und teilen
        inviteButton.setOnClickListener {
            val link = InviteManager.createInviteLink(this)
            shareInvite(link)
        }

        // Einladungen beobachten
        db.partnerInviteDao().getAllInvites().observe(this, Observer { invites ->
            // Partner-Status aktualisieren
            val acceptedPartners = invites.filter { it.accepted }
            if (acceptedPartners.isNotEmpty()) {
                val partnerNames = acceptedPartners.joinToString(", ") { it.partnerName }
                partnerStatusText.text = getString(R.string.partner_status_connected, partnerNames)
            } else {
                partnerStatusText.text = getString(R.string.partner_status_none)
            }

            // Einladungsliste aktualisieren
            if (invites.isEmpty()) {
                inviteListText.text = getString(R.string.partner_invites_empty)
            } else {
                inviteListText.text = invites.joinToString("\n") {
                    if (it.accepted)
                        "✅ ${it.partnerName}"
                    else
                        "⏳ ${it.partnerName} (${getString(R.string.partner_pending)})"
                }
            }
        })
    }

    private fun shareInvite(link: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.partner_invite_share, link))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.partner_invite_share_title)))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}