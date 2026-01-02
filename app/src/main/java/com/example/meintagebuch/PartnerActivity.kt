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

        // === INVITE BUTTON ===
        inviteButton.setOnClickListener {
            if (!PartnerNameHelper.hasMyName(this)) {
                showMyNameDialog()
            } else {
                createAndShareInvite()
            }
        }

        // === FIREBASE SYNC ===
        lifecycleScope.launch {
            try {
                Log.d(TAG, "👀 Starting Firebase observation...")

                FirebaseManager.observeInvites().collect { firebaseInvites ->
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "🔥 Firebase sync: ${firebaseInvites.size} invites")

                    firebaseInvites.forEach { firebaseInvite ->
                        Log.d(TAG, "   Processing invite ${firebaseInvite.inviteId}:")
                        Log.d(TAG, "      Creator: ${firebaseInvite.creatorName}")
                        Log.d(TAG, "      Acceptor: ${firebaseInvite.acceptorName}")
                        Log.d(TAG, "      Accepted: ${firebaseInvite.accepted}")

                        try {
                            val localInvite = db.partnerInviteDao().getInviteById(firebaseInvite.inviteId)

                            if (localInvite == null) {
                                Log.d(TAG, "      → New invite, inserting locally")
                                db.partnerInviteDao().insert(firebaseInvite)
                            } else if (localInvite.accepted != firebaseInvite.accepted ||
                                       localInvite.acceptorName != firebaseInvite.acceptorName) {
                                Log.d(TAG, "      → Invite changed, updating locally")
                                Log.d(TAG, "         Old: accepted=${localInvite.accepted}, acceptor=${localInvite.acceptorName}")
                                Log.d(TAG, "         New: accepted=${firebaseInvite.accepted}, acceptor=${firebaseInvite.acceptorName}")
                                db.partnerInviteDao().updateAccept(
                                    firebaseInvite.inviteId,
                                    firebaseInvite.acceptorName,
                                    firebaseInvite.accepted
                                )
                            } else {
                                Log.d(TAG, "      → No changes")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "      ❌ Error syncing invite", e)
                        }
                    }
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error observing Firebase", e)
            }
        }

        // === LOCAL INVITES OBSERVATION ===
        db.partnerInviteDao().getAllInvites().observe(this) { invites ->
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "📊 Local invites changed: ${invites.size} total")

            val myName = PartnerNameHelper.getMyName(this).trim()
            Log.d(TAG, "   My name: '$myName'")

            if (myName.isBlank()) {
                Log.w(TAG, "⚠️ My name is blank!")
                partnerStatusText.text = getString(R.string.partner_status_none)
                inviteListText.text = "Bitte gib zuerst deinen Namen ein"
                return@observe
            }

            // Akzeptierte Invites finden
            val acceptedInvites = invites.filter { it.accepted }
            Log.d(TAG, "   Accepted invites: ${acceptedInvites.size}")

            // Partner-Namen extrahieren
            val partnerNames = mutableListOf<String>()

            acceptedInvites.forEach { invite ->
                Log.d(TAG, "   Checking invite ${invite.inviteId}:")
                Log.d(TAG, "      Creator: '${invite.creatorName}'")
                Log.d(TAG, "      Acceptor: '${invite.acceptorName}'")
                Log.d(TAG, "      My name: '$myName'")

                val partnerName = when {
                    // Ich bin der Creator → Partner ist der Acceptor
                    invite.creatorName.trim().equals(myName, ignoreCase = true) -> {
                        if (invite.acceptorName != "Unknown" && invite.acceptorName.isNotBlank()) {
                            Log.d(TAG, "      → I'm creator, partner is acceptor: ${invite.acceptorName}")
                            invite.acceptorName.trim()
                        } else {
                            Log.d(TAG, "      → I'm creator, but no acceptor yet")
                            null
                        }
                    }
                    // Ich bin der Acceptor → Partner ist der Creator
                    invite.acceptorName.trim().equals(myName, ignoreCase = true) -> {
                        if (invite.creatorName != "Unknown" && invite.creatorName.isNotBlank()) {
                            Log.d(TAG, "      → I'm acceptor, partner is creator: ${invite.creatorName}")
                            invite.creatorName.trim()
                        } else {
                            Log.d(TAG, "      → I'm acceptor, but no creator")
                            null
                        }
                    }
                    else -> {
                        Log.d(TAG, "      → Neither creator nor acceptor matches my name")
                        Log.d(TAG, "         Creator=='$myName': ${invite.creatorName.trim().equals(myName, ignoreCase = true)}")
                        Log.d(TAG, "         Acceptor=='$myName': ${invite.acceptorName.trim().equals(myName, ignoreCase = true)}")
                        null
                    }
                }

                partnerName?.let {
                    if (it != "Unknown") {
                        partnerNames.add(it)
                        Log.d(TAG, "      ✅ Added partner: $it")
                    }
                }
            }

            // Partner-Status setzen
            if (partnerNames.isNotEmpty()) {
                val names = partnerNames.distinct().joinToString(", ")
                partnerStatusText.text = getString(R.string.partner_status_connected, names)
                Log.d(TAG, "✅ Partner status: Connected with $names")
            } else {
                partnerStatusText.text = getString(R.string.partner_status_none)
                Log.d(TAG, "⚠️ Partner status: No partners")
            }

            // Einladungsliste anzeigen
            if (invites.isEmpty()) {
                inviteListText.text = getString(R.string.partner_invites_empty)
            } else {
                val listText = invites.joinToString("\n\n") { invite ->
                    val partnerName = when {
                        invite.creatorName.trim().equals(myName, ignoreCase = true) ->
                            invite.acceptorName.trim()
                        invite.acceptorName.trim().equals(myName, ignoreCase = true) ->
                            invite.creatorName.trim()
                        else -> "Unknown"
                    }

                    val status = when {
                        invite.accepted && partnerName != "Unknown" && partnerName.isNotBlank() ->
                            "✅ Verbunden mit $partnerName"
                        invite.accepted ->
                            "✅ Verbunden"
                        partnerName != "Unknown" && partnerName.isNotBlank() ->
                            "⏳ Warte auf $partnerName"
                        else ->
                            "⏳ Ausstehend..."
                    }

                    "$status\n   ID: ${invite.inviteId.take(8)}..."
                }
                inviteListText.text = listText
            }

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }

    private fun showMyNameDialog() {
        Log.d(TAG, "📝 Showing name input dialog")

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_two_names, null)
        val myNameInput: EditText = dialogView.findViewById(R.id.myNameInput)

        AlertDialog.Builder(this)
            .setTitle("Dein Name")
            .setMessage("Wie heißt du?")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Weiter") { _, _ ->
                val myName = myNameInput.text.toString().trim().ifBlank { "Ich" }
                Log.d(TAG, "✅ Name entered: $myName")

                PartnerNameHelper.setMyName(this, myName)
                createAndShareInvite()
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun createAndShareInvite() {
        Log.d(TAG, "🔄 Creating invite...")
        Toast.makeText(this, "Erstelle Einladung...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val link = InviteManager.createInviteLink(this@PartnerActivity)
                Log.d(TAG, "✅ Invite created: $link")
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
        Log.d(TAG, "📤 Sharing invite link")
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