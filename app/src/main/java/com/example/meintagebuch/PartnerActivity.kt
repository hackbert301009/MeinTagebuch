package com.example.meintagebuch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class PartnerActivity : AppCompatActivity() {

    private val TAG = "PartnerActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partner)

        Log.d(TAG, "PartnerActivity created")

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
            Log.d(TAG, "Invite button clicked")
            val link = InviteManager.createInviteLink(this)
            shareInvite(link)
        }

        // Firebase-Einladungen beobachten und mit lokaler DB synchronisieren
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Starting Firebase observation")
                FirebaseManager.observeInvites().collect { firebaseInvites ->
                    Log.d(TAG, "Received ${firebaseInvites.size} invites from Firebase")

                    // Firebase-Daten in lokale DB synchronisieren
                    firebaseInvites.forEach { invite ->
                        try {
                            val localInvite = db.partnerInviteDao().getInviteById(invite.inviteId)
                            if (localInvite == null) {
                                // Neue Einladung aus Firebase in lokale DB einfügen
                                Log.d(TAG, "Inserting new invite: ${invite.inviteId}")
                                db.partnerInviteDao().insert(invite)
                            } else if (localInvite.accepted != invite.accepted ||
                                       localInvite.partnerName != invite.partnerName) {
                                // Einladung aktualisieren - KORRIGIERT!
                                Log.d(TAG, "Updating invite: ${invite.inviteId} with name: ${invite.partnerName}, accepted: ${invite.accepted}")
                                db.partnerInviteDao().updateNameAndAccept(
                                    invite.inviteId,
                                    invite.partnerName,
                                    invite.accepted  // WICHTIG: accepted-Status auch übergeben!
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing invite: ${invite.inviteId}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing Firebase invites", e)
            }
        }

        // Lokale Einladungen beobachten und UI aktualisieren
        db.partnerInviteDao().getAllInvites().observe(this) { invites ->
            Log.d(TAG, "Local invites updated: ${invites.size}")

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
        }
    }

    private fun shareInvite(link: String) {
        Log.d(TAG, "Sharing invite: $link")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.partner_invite_share, link))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.partner_invite_share_title)))
    }

    override fun onSupportNavigateUp(): Boolean {
        Log.d(TAG, "Navigate up pressed")
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "PartnerActivity destroyed")
    }
}