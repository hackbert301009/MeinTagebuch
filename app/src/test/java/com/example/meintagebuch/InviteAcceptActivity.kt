package com.example.meintagebuch

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class InviteAcceptActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inviteId = intent?.data?.getQueryParameter("code")
        if (inviteId == null) {
            finish()
            return
        }

        val input = EditText(this)
        input.hint = "Dein Name"

        AlertDialog.Builder(this)
            .setTitle("Einladung annehmen?")
            .setMessage("Wie soll dein Name angezeigt werden?")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("@string/") { _, _ ->
                val name = input.text.toString().ifBlank { "Partner" }
                acceptInvite(inviteId, name)
            }
            .setNegativeButton("Abbrechen") { _, _ ->
                finish()
            }
            .show()
    }

    private fun acceptInvite(inviteId: String, name: String) {
        lifecycleScope.launch {
            AppDatabase.getDatabase(this@InviteAcceptActivity)
                .partnerInviteDao()
                .updateNameAndAccept(inviteId, name)

            finish()
        }
    }
}
