package com.example.meintagebuch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlinx.coroutines.tasks.await
class InviteAcceptActivity : AppCompatActivity() {

    private val TAG = "InviteAcceptActivity"
    private lateinit var myUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        myUserId = UserIdHelper.getUserId(this)

        val inviteId = intent?.data?.getQueryParameter("code")

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📥 Invite Accept Activity started")
        Log.d(TAG, "   Invite ID: $inviteId")
        Log.d(TAG, "   My User ID: $myUserId")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        if (inviteId == null) {
            Log.e(TAG, "❌ No invite ID in URL")
            Toast.makeText(this, "Ungültige Einladung", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadInvite(inviteId)
    }

    private fun loadInvite(inviteId: String) {
        Toast.makeText(this, "Lade Einladung...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                Log.d(TAG, "🔄 Trying to load invite from Firebase...")

                var firebaseInvite: PartnerInvite? = null
                repeat(3) { attempt ->
                    Log.d(TAG, "   Attempt ${attempt + 1}/3")
                    try {
                        firebaseInvite = FirebaseManager.getInvite(inviteId)
                        if (firebaseInvite != null) {
                            Log.d(TAG, "   ✅ Found invite!")
                            return@repeat
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "   ⚠️ Attempt ${attempt + 1} failed: ${e.message}")
                    }
                    if (attempt < 2) delay(1500)
                }

                if (firebaseInvite == null) {
                    Log.e(TAG, "❌ Could not load invite after 3 attempts")
                    Toast.makeText(
                        this@InviteAcceptActivity,
                        "Einladung nicht gefunden. Bitte versuche es erneut.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@launch
                }

                Log.d(TAG, "📋 Invite details:")
                Log.d(TAG, "   Creator: ${firebaseInvite!!.creatorName} (${firebaseInvite!!.creatorUserId})")
                Log.d(TAG, "   Accepted: ${firebaseInvite!!.accepted}")

                if (firebaseInvite!!.accepted) {
                    Log.w(TAG, "⚠️ Invite already accepted")
                    Toast.makeText(
                        this@InviteAcceptActivity,
                        "Diese Einladung wurde bereits angenommen",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@launch
                }

                showNameInputDialog(inviteId, firebaseInvite!!)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading invite", e)
                Log.e(TAG, "   Message: ${e.message}")
                Toast.makeText(
                    this@InviteAcceptActivity,
                    "Fehler beim Laden: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun showNameInputDialog(inviteId: String, invite: PartnerInvite) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_two_names, null)
        val myNameInput: EditText = dialogView.findViewById(R.id.myNameInput)

        AlertDialog.Builder(this)
            .setTitle("Einladung von ${invite.creatorName}")
            .setMessage("Wie heißt du?")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Annehmen") { dialog, _ ->
                val myName = myNameInput.text.toString().trim().ifBlank { "Partner" }
                PartnerNameHelper.setMyName(this, myName)
                dialog.dismiss()
                acceptInvite(inviteId, myName, invite)
            }
            .setNegativeButton("Abbrechen") { _, _ ->
                finish()
            }
            .show()
    }

    private fun acceptInvite(inviteId: String, myName: String, invite: PartnerInvite) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "✅ Accepting invite")
        Log.d(TAG, "   My name: $myName")
        Log.d(TAG, "   My User-ID: $myUserId")
        Log.d(TAG, "   Creator name: ${invite.creatorName}")
        Log.d(TAG, "   Creator User-ID: ${invite.creatorUserId}")

        Toast.makeText(this, "Einladung wird angenommen...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@InviteAcceptActivity)

                // Schritt 1: Invite lokal speichern
                Log.d(TAG, "💾 Saving invite locally...")
                val acceptedInvite = invite.copy(
                    acceptorName = myName,
                    acceptorUserId = myUserId,
                    accepted = true
                )
                db.partnerInviteDao().insert(acceptedInvite)
                Log.d(TAG, "✅ Invite saved locally")

                // Schritt 2: Partnership erstellen
                val partnershipId = inviteId  // Verwende invite-ID als partnership-ID!

                Log.d(TAG, "💑 Creating partnership...")
                Log.d(TAG, "   Partnership ID: $partnershipId")

                // Partnership für MICH (Acceptor)
                val myPartnership = Partnership(
                    partnershipId = partnershipId,
                    myUserId = myUserId,
                    partnerUserId = invite.creatorUserId,
                    myName = myName,
                    partnerName = invite.creatorName,
                    active = true
                )

                // Partnership für CREATOR (damit beide es sehen!)
                val creatorPartnership = Partnership(
                    partnershipId = partnershipId,
                    myUserId = invite.creatorUserId,
                    partnerUserId = myUserId,
                    myName = invite.creatorName,
                    partnerName = myName,
                    active = true
                )

                // Lokal speichern
                Log.d(TAG, "💾 Saving my partnership locally...")
                db.partnershipDao().insert(myPartnership)
                Log.d(TAG, "✅ My partnership saved locally")

                // Firebase: Beide Partnerships speichern
                try {
                    Log.d(TAG, "☁️ Saving partnerships to Firebase...")

                    // Meine Partnership
                    FirebaseManager.database.child("partnerships")
                        .child(myUserId)
                        .child(partnershipId)
                        .setValue(myPartnership)
                        .await()

                    // Creator's Partnership
                    FirebaseManager.database.child("partnerships")
                        .child(invite.creatorUserId)
                        .child(partnershipId)
                        .setValue(creatorPartnership)
                        .await()

                    Log.d(TAG, "✅ Partnerships saved to Firebase")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Firebase save failed (but continuing): ${e.message}")
                }

                // Schritt 3: Invite in Firebase akzeptieren
                try {
                    Log.d(TAG, "☁️ Updating invite in Firebase...")
                    FirebaseManager.updateInviteAccept(inviteId, myName, myUserId, true)
                    Log.d(TAG, "✅ Invite updated in Firebase")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Firebase invite update failed (but continuing): ${e.message}")
                }

                delay(500)  // Kurz warten für Firebase-Sync

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "✅✅✅ PARTNERSHIP CREATED ✅✅✅")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                Toast.makeText(
                    this@InviteAcceptActivity,
                    "Verbunden mit ${invite.creatorName}! 💕",
                    Toast.LENGTH_LONG
                ).show()

                val intent = Intent(this@InviteAcceptActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error accepting invite", e)
                Log.e(TAG, "   Message: ${e.message}")
                Log.e(TAG, "   Stack: ${e.stackTraceToString()}")

                Toast.makeText(
                    this@InviteAcceptActivity,
                    "Fehler: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }
}