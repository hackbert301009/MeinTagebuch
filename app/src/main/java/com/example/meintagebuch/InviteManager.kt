package com.example.meintagebuch

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

object InviteManager {

    private const val TAG = "InviteManager"

    fun createInviteLink(context: Context): String {
        val inviteId = UUID.randomUUID().toString()

        // Meinen Namen holen
        val myName = PartnerNameHelper.getMyName(context).ifBlank { "Unbekannt" }

        Log.d(TAG, "Creating invite with creator name: $myName")

        val link = "https://app.hackbert.org/invite?code=$inviteId"

        CoroutineScope(Dispatchers.IO).launch {
            val invite = PartnerInvite(
                inviteId = inviteId,
                creatorName = myName,        // ICH bin der Creator
                acceptorName = "Unknown",    // Partner noch unbekannt
                accepted = false
            )

            // Lokal speichern
            AppDatabase.getDatabase(context)
                .partnerInviteDao()
                .insert(invite)

            // In Firebase speichern
            FirebaseManager.createInvite(invite)

            Log.d(TAG, "✅ Invite created: $inviteId")
        }

        return link
    }
}