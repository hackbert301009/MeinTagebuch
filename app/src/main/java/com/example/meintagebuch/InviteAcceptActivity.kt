package com.example.meintagebuch

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
        Log.d(TAG, "Invite ID received: $inviteId")

        if (inviteId == null) {
            Toast.makeText(this, getString(R.string.invite_invalid), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showNameInputDialog(inviteId)
    }

    private fun showNameInputDialog(inviteId: String) {
        val input = EditText(this)
        input.hint = getString(R.string.invite_accept_hint)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.invite_accept_title))
            .setMessage(getString(R.string.invite_accept_message))
            .setView(input)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.invite_accept_button)) { dialog, _ ->
                val name = input.text.toString().ifBlank {
                    getString(R.string.invite_default_name)
                }
                dialog.dismiss()
                acceptInvite(inviteId, name)
            }
            .setNegativeButton(getString(R.string.invite_cancel_button)) { _, _ ->
                finish()
            }
            .show()
    }

    private fun acceptInvite(inviteId: String, name: String) {
        // Zeige einen Loading-Toast
        Toast.makeText(this, "Einladung wird angenommen...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Trying to get invite from Firebase: $inviteId")

                // Einladung aus Firebase holen
                val firebaseInvite = FirebaseManager.getInvite(inviteId)
                Log.d(TAG, "Firebase invite: $firebaseInvite")

                if (firebaseInvite != null) {
                    // In Firebase aktualisieren
                    Log.d(TAG, "Updating Firebase with name: $name")
                    FirebaseManager.updateInvite(inviteId, name, true)

                    // Auch lokal speichern
                    val db = AppDatabase.getDatabase(this@InviteAcceptActivity)
                    val localInvite = db.partnerInviteDao().getInviteById(inviteId)

                    if (localInvite != null) {
                        Log.d(TAG, "Updating local invite")
                        db.partnerInviteDao().updateNameAndAccept(inviteId, name)
                    } else {
                        Log.d(TAG, "Inserting new local invite")
                        db.partnerInviteDao().insert(
                            PartnerInvite(
                                inviteId = inviteId,
                                partnerName = name,
                                accepted = true
                            )
                        )
                    }

                    Toast.makeText(
                        this@InviteAcceptActivity,
                        getString(R.string.invite_accepted),
                        Toast.LENGTH_LONG
                    ).show()

                    // Zur MainActivity navigieren
                    val intent = Intent(this@InviteAcceptActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    Log.e(TAG, "Firebase invite is null!")
                    Toast.makeText(
                        this@InviteAcceptActivity,
                        "Einladung nicht in Firebase gefunden. Erstelle neue Einladung...",
                        Toast.LENGTH_LONG
                    ).show()

                    // Wenn Einladung nicht existiert, erstelle sie
                    val newInvite = PartnerInvite(
                        inviteId = inviteId,
                        partnerName = name,
                        accepted = true
                    )

                    // In Firebase speichern
                    FirebaseManager.createInvite(newInvite)

                    // Lokal speichern
                    val db = AppDatabase.getDatabase(this@InviteAcceptActivity)
                    db.partnerInviteDao().insert(newInvite)

                    Toast.makeText(
                        this@InviteAcceptActivity,
                        getString(R.string.invite_accepted),
                        Toast.LENGTH_LONG
                    ).show()

                    // Zur MainActivity navigieren
                    val intent = Intent(this@InviteAcceptActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error accepting invite", e)
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