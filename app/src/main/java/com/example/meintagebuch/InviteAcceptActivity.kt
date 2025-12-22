package com.example.meintagebuch

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import  android.widget.EditText

class InviteAcceptActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Invite-Code aus Link auslesen
        val inviteId = intent?.data?.getQueryParameter("code")

        if (inviteId != null) {
            // Optional: Benutzername abfragen
            val input = EditText(this)
            AlertDialog.Builder(this)
                .setTitle("Name eingeben")
                .setMessage("Wie soll dein Name angezeigt werden?")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    val name = input.text.toString().ifBlank { "Partner" }
                    acceptInvite(inviteId, name)
                }
                .setNegativeButton("Abbrechen") { _, _ ->
                    acceptInvite(inviteId, "Partner")
                }
                .show()
        } else {
            finish() // Kein Code → schließen
        }
    }

    private fun acceptInvite(inviteId: String, name: String) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@InviteAcceptActivity)
            val existing = db.partnerInviteDao().getInviteById(inviteId)
            if (existing != null) {
                db.partnerInviteDao().markAccepted(inviteId)
            } else {
                db.partnerInviteDao().insert(
                    PartnerInvite(inviteId, partnerName = name, accepted = true)
                )
            }
            finish() // Activity schließen nach akzeptieren
        }
    }
}
