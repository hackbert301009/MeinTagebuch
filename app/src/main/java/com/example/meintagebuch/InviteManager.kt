package com.example.meintagebuch

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

object InviteManager {

    fun createInviteLink(context: Context): String {
        val inviteId = UUID.randomUUID().toString()

        // Einfach Link generieren – keine Rekursion!
        val link = "meintagebuch://invite?code=$inviteId"

        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getDatabase(context)
                .partnerInviteDao()
                .insert(
                    PartnerInvite(inviteId, partnerName = "Unknown", accepted = false)
                )
        }

        return link
    }
}
