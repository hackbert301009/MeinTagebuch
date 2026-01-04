package com.example.meintagebuch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class PartnerActivity : AppCompatActivity() {

    private val TAG = "PartnerActivity"
    private lateinit var db: AppDatabase
    private lateinit var myUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partner)

        val toolbar: Toolbar = findViewById(R.id.partnerToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.partner_title)

        db = AppDatabase.getDatabase(this)
        myUserId = UserIdHelper.getUserId(this)

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "👤 My User ID: $myUserId")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        val inviteButton: Button = findViewById(R.id.inviteButton)
        val disconnectButton: Button = findViewById(R.id.disconnectPartnerButton)
        val partnerStatusText: TextView = findViewById(R.id.partnerStatusText)

        // Invite Button
        inviteButton.setOnClickListener {
            if (!PartnerNameHelper.hasMyName(this)) {
                showMyNameDialog()
            } else {
                createAndShareInvite()
            }
        }

        // Disconnect Button
        disconnectButton.setOnClickListener {
            lifecycleScope.launch {
                val partnership = db.partnershipDao().getFirstActivePartnership()
                if (partnership != null) {
                    showDisconnectDialog(partnership)
                } else {
                    Toast.makeText(
                        this@PartnerActivity,
                        "Keine aktive Verbindung vorhanden",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Firebase Sync - Partnerschaften beobachten
        lifecycleScope.launch {
            try {
                FirebaseManager.observePartnerships(myUserId).collect { firebasePartnerships ->
                    Log.d(TAG, "🔥 Firebase partnerships: ${firebasePartnerships.size}")

                    firebasePartnerships.forEach { partnership ->
                        val existing = db.partnershipDao().getPartnershipById(partnership.partnershipId)
                        if (existing == null) {
                            Log.d(TAG, "   → New partnership, inserting")
                            db.partnershipDao().insert(partnership)
                        } else if (existing.active != partnership.active) {
                            Log.d(TAG, "   → Partnership status changed")
                            db.partnershipDao().insert(partnership)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error observing partnerships", e)
            }
        }

        // Firebase Sync - Invites beobachten (wie vorher)
        lifecycleScope.launch {
            try {
                FirebaseManager.observeInvites().collect { firebaseInvites ->
                    firebaseInvites.forEach { invite ->
                        try {
                            val localInvite = db.partnerInviteDao().getInviteById(invite.inviteId)
                            if (localInvite == null) {
                                db.partnerInviteDao().insert(invite)
                            } else if (localInvite.accepted != invite.accepted) {
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
                Log.e(TAG, "❌ Error observing invites", e)
            }
        }

        // Lokale Partnerschaften beobachten
        db.partnershipDao().getActivePartnerships().observe(this) { partnerships ->
            Log.d(TAG, "📊 Active partnerships: ${partnerships.size}")

            if (partnerships.isEmpty()) {
                partnerStatusText.text = getString(R.string.partner_status_none)
                disconnectButton.isEnabled = false
                disconnectButton.alpha = 0.5f
            } else {
                val names = partnerships.map { it.partnerName }.joinToString(", ")
                partnerStatusText.text = getString(R.string.partner_status_connected, names)
                disconnectButton.isEnabled = true
                disconnectButton.alpha = 1.0f
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
                val myName = myNameInput.text.toString().trim().ifBlank { "Ich" }
                PartnerNameHelper.setMyName(this, myName)
                createAndShareInvite()
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun createAndShareInvite() {
        Toast.makeText(this, "Erstelle Einladung...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val link = InviteManager.createInviteLink(this@PartnerActivity)
                shareInvite(link)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error creating invite", e)
                Toast.makeText(
                    this@PartnerActivity,
                    "Fehler: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun shareInvite(link: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.partner_invite_share, link))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.partner_invite_share_title)))
    }

    private fun showDisconnectDialog(partnership: Partnership) {
        val dialog = PartnerDisconnectDialog(
            context = this,
            partnership = partnership,
            lifecycleOwner = this,
            onDisconnected = {
                // UI aktualisiert sich automatisch durch LiveData
                Toast.makeText(this, "Verbindung getrennt", Toast.LENGTH_SHORT).show()
            }
        )
        dialog.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}