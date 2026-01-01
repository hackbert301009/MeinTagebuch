package com.example.meintagebuch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class PartnerActivity : AppCompatActivity() {

    private val TAG = "PartnerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partner)

        val toolbar: Toolbar = findViewById(R.id.partnerToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.partner_title)

        val db = AppDatabase.getDatabase(this)

        val inviteButton: Button = findViewById(R.id.inviteButton)
        val inviteListText: TextView = findViewById(R.id.inviteListText)
        val partnerStatusText: TextView = findViewById(R.id.partnerStatusText)

        inviteButton.setOnClickListener {
            // Namen abfragen falls noch nicht vorhanden
            if (!PartnerNameHelper.hasMyName(this)) {
                showMyNameDialog()
            } else {
                val link = InviteManager.createInviteLink(this)
                shareInvite(link)
            }
        }

        // Firebase beobachten
        lifecycleScope.launch {
            try {
                FirebaseManager.observeInvites().collect { firebaseInvites ->
                    Log.d(TAG, "📥 Firebase invites: ${firebaseInvites.size}")

                    firebaseInvites.forEach { invite ->
                        try {
                            val localInvite = db.partnerInviteDao().getInviteById(invite.inviteId)
                            if (localInvite == null) {
                                Log.d(TAG, "📝 New invite from Firebase")
                                db.partnerInviteDao().insert(invite)
                            } else if (localInvite.accepted != invite.accepted ||
                                       localInvite.acceptorName != invite.acceptorName) {
                                Log.d(TAG, "🔄 Updating invite")
                                db.partnerInviteDao().updateAccept(
                                    invite.inviteId,
                                    invite.acceptorName,
                                    invite.accepted
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing invite", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing Firebase", e)
            }
        }

        // Lokale Invites beobachten
        db.partnerInviteDao().getAllInvites().observe(this) { invites ->
            Log.d(TAG, "📊 Local invites: ${invites.size}")

            val myName = PartnerNameHelper.getMyName(this)

            // Partner-Status berechnen
            val acceptedInvites = invites.filter { it.accepted }

            if (acceptedInvites.isNotEmpty()) {
                val partnerNames = acceptedInvites.mapNotNull { invite ->
                    // Bin ich der Creator oder Acceptor?
                    if (invite.creatorName == myName && invite.acceptorName != "Unknown") {
                        invite.acceptorName  // Ich habe eingeladen, zeige Acceptor
                    } else if (invite.acceptorName == myName) {
                        invite.creatorName   // Ich habe angenommen, zeige Creator
                    } else null
                }.filter { it.isNotBlank() && it != "Unknown" }

                if (partnerNames.isNotEmpty()) {
                    val names = partnerNames.joinToString(", ")
                    partnerStatusText.text = getString(R.string.partner_status_connected, names)
                    Log.d(TAG, "✅ Connected with: $names")
                } else {
                    partnerStatusText.text = getString(R.string.partner_status_none)
                }
            } else {
                partnerStatusText.text = getString(R.string.partner_status_none)
            }

            // Einladungsliste
            if (invites.isEmpty()) {
                inviteListText.text = getString(R.string.partner_invites_empty)
            } else {
                inviteListText.text = invites.joinToString("\n") { invite ->
                    val partnerName = if (invite.creatorName == myName) {
                        invite.acceptorName
                    } else {
                        invite.creatorName
                    }

                    when {
                        invite.accepted && partnerName != "Unknown" ->
                            "✅ $partnerName"
                        invite.accepted ->
                            "✅ Verbunden"
                        else ->
                            "⏳ Ausstehend..."
                    }
                }
            }
        }
    }

    private fun showMyNameDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_two_names, null)
        val myNameInput: EditText = dialogView.findViewById(R.id.myNameInput)

        AlertDialog.Builder(this)
            .setTitle("Dein Name")
            .setMessage("Wie heißt du?")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Weiter") { _, _ ->
                val myName = myNameInput.text.toString().ifBlank { "Ich" }
                PartnerNameHelper.setMyName(this, myName)

                val link = InviteManager.createInviteLink(this)
                shareInvite(link)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
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