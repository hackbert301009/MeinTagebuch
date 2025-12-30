package com.example.meintagebuch

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

object InviteManager {

    fun createInviteLink(context: Context): String {
        val inviteId = UUID.randomUUID().toString()

        // HTTPS Link mit deiner Domain
        val link = "https://app.hackbert.org/invite?code=$inviteId"

        // Sowohl in lokaler Datenbank als auch in Firebase speichern
        CoroutineScope(Dispatchers.IO).launch {
            val invite = PartnerInvite(
                inviteId = inviteId,
                partnerName = "Unknown",
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