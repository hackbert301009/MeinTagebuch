package com.example.meintagebuch

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer

class PartnerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partner)

        val db = AppDatabase.getDatabase(this)

        val inviteButton: Button = findViewById(R.id.inviteButton)
        val textView: TextView = findViewById(R.id.inviteListText)

        inviteButton.setOnClickListener {
            val link = InviteManager.createInviteLink(this)
            shareInvite(link)
        }

        db.partnerInviteDao().getAllInvites().observe(this, Observer { invites ->
            textView.text = invites.joinToString("\n") {
                if (it.accepted)
                    "✅ ${it.partnerName}"
                else
                    "⏳ ${it.partnerName}"
            }
        })
    }

    private fun shareInvite(link: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Tagebuch-Einladung: $link")
        }
        startActivity(Intent.createChooser(intent, "Einladung teilen"))
    }
}
