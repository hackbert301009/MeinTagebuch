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

class InviteAcceptActivity : AppCompatActivity() {

    private val TAG = "InviteAcceptActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inviteId = intent?.data?.getQueryParameter("code")

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📥 Invite Accept Activity started")
        Log.d(TAG, "   Invite ID: $inviteId")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        if (inviteId == null) {
            Log.e(TAG, "❌ No invite ID in URL")
            Toast.makeText(this, getString(R.string.invite_invalid), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadInvite(inviteId)
    }

    private fun loadInvite(inviteId: String) {
        Toast.makeText(this, "Lade Einladung...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                Log.d(TAG, "🔄 Loading invite from Firebase...")

                // Mehrere Versuche, da Firebase manchmal langsam ist
                var firebaseInvite: PartnerInvite? = null
                repeat(3) { attempt ->
                    Log.d(TAG, "   Attempt ${attempt + 1}/3")
                    firebaseInvite = FirebaseManager.getInvite(inviteId)
                    if (firebaseInvite != null) {
                        Log.d(TAG, "   ✅ Found invite on attempt ${attempt + 1}")
                        return@repeat
                    }
                    if (attempt < 2) delay(1500) // Warten zwischen Versuchen
                }

                if (firebaseInvite == null) {
                    Log.e(TAG, "❌ Invite not found after 3 attempts")
                    Toast.makeText(
                        this@InviteAcceptActivity,
                        "Einladung nicht gefunden. Bitte versuche es erneut.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@launch
                }

                Log.d(TAG, "✅ Invite loaded:")
                Log.d(TAG, "   Creator: ${firebaseInvite!!.creatorName}")
                Log.d(TAG, "   Acceptor: ${firebaseInvite!!.acceptorName}")
                Log.d(TAG, "   Accepted: ${firebaseInvite!!.accepted}")

                // Prüfen ob bereits angenommen
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

                // Einladung ist gültig - Namen abfragen
                showNameInputDialog(inviteId, firebaseInvite!!.creatorName)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading invite", e)
                Toast.makeText(
                    this@InviteAcceptActivity,
                    "Fehler beim Laden: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun showNameInputDialog(inviteId: String, creatorName: String) {
        Log.d(TAG, "📝 Showing name input dialog")

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_two_names, null)
        val myNameInput: EditText = dialogView.findViewById(R.id.myNameInput)

        AlertDialog.Builder(this)
            .setTitle("Einladung von $creatorName")
            .setMessage("Wie heißt du?")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.invite_accept_button)) { dialog, _ ->
                val myName = myNameInput.text.toString().trim().ifBlank { "Partner" }

                Log.d(TAG, "✅ Name entered: $myName")

                // Meinen Namen SOFORT speichern
                PartnerNameHelper.setMyName(this, myName)
                Log.d(TAG, "✅ My name saved locally: $myName")

                dialog.dismiss()
                acceptInvite(inviteId, myName, creatorName)
            }
            .setNegativeButton(getString(R.string.invite_cancel_button)) { _, _ ->
                Log.d(TAG, "❌ User cancelled invite")
                finish()
            }
            .show()
    }

    private fun acceptInvite(inviteId: String, myName: String, creatorName: String) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "✅ Accepting invite")
        Log.d(TAG, "   Invite ID: $inviteId")
        Log.d(TAG, "   My name (acceptor): $myName")
        Log.d(TAG, "   Creator name: $creatorName")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        Toast.makeText(this, "Einladung wird angenommen...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@InviteAcceptActivity)

                // Schritt 1: In Firebase aktualisieren
                Log.d(TAG, "🔄 Updating Firebase...")
                FirebaseManager.updateInviteAccept(inviteId, myName, true)
                Log.d(TAG, "✅ Firebase updated")

                // Kurz warten um sicherzustellen dass Update durchging
                delay(500)

                // Schritt 2: Lokal speichern
                Log.d(TAG, "🔄 Saving locally...")
                val invite = PartnerInvite(
                    inviteId = inviteId,
                    creatorName = creatorName,
                    acceptorName = myName,
                    accepted = true
                )
                db.partnerInviteDao().insert(invite)
                Log.d(TAG, "✅ Saved locally")

                // Schritt 3: Verifizieren
                val savedLocally = db.partnerInviteDao().getInviteById(inviteId)
                Log.d(TAG, "🔍 Local verification:")
                Log.d(TAG, "   Creator: ${savedLocally?.creatorName}")
                Log.d(TAG, "   Acceptor: ${savedLocally?.acceptorName}")
                Log.d(TAG, "   Accepted: ${savedLocally?.accepted}")

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "✅✅✅ INVITE ACCEPTED SUCCESSFULLY ✅✅✅")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                Toast.makeText(
                    this@InviteAcceptActivity,
                    "Verbunden mit $creatorName! 💕",
                    Toast.LENGTH_LONG
                ).show()

                // Zur MainActivity
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