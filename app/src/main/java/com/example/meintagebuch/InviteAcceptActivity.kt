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
import kotlinx.coroutines.launch

class InviteAcceptActivity : AppCompatActivity() {

    private val TAG = "InviteAcceptActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inviteId = intent?.data?.getQueryParameter("code")
        Log.d(TAG, "📥 Invite ID received: $inviteId")

        if (inviteId == null) {
            Toast.makeText(this, getString(R.string.invite_invalid), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Zuerst Einladung aus Firebase holen
        lifecycleScope.launch {
            try {
                val firebaseInvite = FirebaseManager.getInvite(inviteId)

                if (firebaseInvite == null) {
                    Log.e(TAG, "❌ Invite not found in Firebase")
                    Toast.makeText(
                        this@InviteAcceptActivity,
                        getString(R.string.invite_invalid),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@launch
                }

                Log.d(TAG, "✅ Invite found: creator=${firebaseInvite.creatorName}, accepted=${firebaseInvite.accepted}")

                if (firebaseInvite.accepted) {
                    Toast.makeText(
                        this@InviteAcceptActivity,
                        "Diese Einladung wurde bereits angenommen",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@launch
                }

                // Einladung ist gültig, Namen abfragen
                showNameInputDialog(inviteId, firebaseInvite.creatorName)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading invite", e)
                Toast.makeText(
                    this@InviteAcceptActivity,
                    "Fehler beim Laden der Einladung",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun showNameInputDialog(inviteId: String, creatorName: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_two_names, null)
        val myNameInput: EditText = dialogView.findViewById(R.id.myNameInput)

        AlertDialog.Builder(this)
            .setTitle("Einladung von $creatorName")
            .setMessage("Wie heißt du?")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.invite_accept_button)) { dialog, _ ->
                val myName = myNameInput.text.toString().ifBlank { "Partner" }

                // Meinen Namen speichern
                PartnerNameHelper.setMyName(this, myName)

                dialog.dismiss()
                acceptInvite(inviteId, myName, creatorName)
            }
            .setNegativeButton(getString(R.string.invite_cancel_button)) { _, _ ->
                finish()
            }
            .show()
    }

    private fun acceptInvite(inviteId: String, myName: String, creatorName: String) {
        Toast.makeText(this, "Einladung wird angenommen...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                Log.d(TAG, "💾 Accepting invite - My name: $myName, Creator: $creatorName")

                // In Firebase aktualisieren
                FirebaseManager.updateInviteAccept(inviteId, myName, true)

                // Lokal speichern
                val db = AppDatabase.getDatabase(this@InviteAcceptActivity)
                db.partnerInviteDao().insert(
                    PartnerInvite(
                        inviteId = inviteId,
                        creatorName = creatorName,  // Name des Einladenden
                        acceptorName = myName,      // Mein Name
                        accepted = true
                    )
                )

                Log.d(TAG, "✅ Invite accepted successfully")

                Toast.makeText(
                    this@InviteAcceptActivity,
                    "Verbunden mit $creatorName! 💕",
                    Toast.LENGTH_LONG
                ).show()

                val intent = Intent(this@InviteAcceptActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error accepting invite", e)
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