package com.example.meintagebuch

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

object InviteManager {

    fun createInviteLink(context: Context): String {
        val inviteId = UUID.randomUUID().toString()

        // Meinen Namen für die Einladung holen
        val myName = PartnerNameHelper.getMyName(context).ifBlank { "Unbekannt" }

        val link = "https://app.hackbert.org/invite?code=$inviteId"

        CoroutineScope(Dispatchers.IO).launch {
            val invite = PartnerInvite(
                inviteId = inviteId,
                partnerName = myName,  // Mein Name als "partnerName" für den anderen
                accepted = false
            )

            // Lokal speichern
            AppDatabase.getDatabase(context)
                .partnerInviteDao()
                .insert(invite)

            // In Firebase speichern
            FirebaseManager.createInvite(invite)
        }

        return link
    }
}