package com.example.meintagebuch

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class InviteAcceptActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inviteId = intent?.data?.getQueryParameter("code")
        if (inviteId == null) {
            Toast.makeText(this, getString(R.string.invite_invalid), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val input = EditText(this)
        input.hint = getString(R.string.invite_accept_hint)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.invite_accept_title))
            .setMessage(getString(R.string.invite_accept_message))
            .setView(input)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.invite_accept_button)) { _, _ ->
                val name = input.text.toString().ifBlank {
                    getString(R.string.invite_default_name)
                }
                acceptInvite(inviteId, name)
            }
            .setNegativeButton(getString(R.string.invite_cancel_button)) { _, _ ->
                finish()
            }
            .show()
    }

    private fun acceptInvite(inviteId: String, name: String) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@InviteAcceptActivity)
            val dao = db.partnerInviteDao()

            // Prüfen ob Einladung existiert
            val existingInvite = dao.getInviteById(inviteId)

            if (existingInvite != null) {
                // Einladung existiert bereits - Namen aktualisieren
                dao.updateNameAndAccept(inviteId, name)
                Toast.makeText(
                    this@InviteAcceptActivity,
                    getString(R.string.invite_accepted),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Neue Einladung erstellen (für den Fall, dass sie auf einem anderen Gerät erstellt wurde)
                dao.insert(
                    PartnerInvite(
                        inviteId = inviteId,
                        partnerName = name,
                        accepted = true
                    )
                )
                Toast.makeText(
                    this@InviteAcceptActivity,
                    getString(R.string.invite_accepted),
                    Toast.LENGTH_SHORT
                ).show()
            }

            finish()
        }
    }
}