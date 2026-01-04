package com.example.meintagebuch

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay

object InviteManager {

    private const val TAG = "InviteManager"

    suspend fun createInviteLink(context: Context): String {
        val inviteId = java.util.UUID.randomUUID().toString()
        val myName = PartnerNameHelper.getMyName(context).trim().ifBlank { "Unbekannt" }
        val myUserId = UserIdHelper.getUserId(context)  // NEU!

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📝 Creating new invite")
        Log.d(TAG, "   ID: $inviteId")
        Log.d(TAG, "   Creator (me): $myName")
        Log.d(TAG, "   Creator User-ID: $myUserId")  // NEU!
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        val invite = PartnerInvite(
            inviteId = inviteId,
            creatorName = myName,
            acceptorName = "Unknown",
            accepted = false,
            creatorUserId = myUserId,  // NEU!
            acceptorUserId = ""        // NEU!
        )

        // Lokal speichern
        try {
            AppDatabase.getDatabase(context)
                .partnerInviteDao()
                .insert(invite)
            Log.d(TAG, "✅ Invite saved locally")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save invite locally", e)
            throw Exception("Lokales Speichern fehlgeschlagen: ${e.message}")
        }

        // In Firebase speichern
        try {
            Log.d(TAG, "🔄 Saving to Firebase...")
            FirebaseManager.createInvite(invite)

            // Verifizierung
            var verified = false
            repeat(5) { attempt ->
                delay(1500)
                val savedInvite = FirebaseManager.getInvite(inviteId)
                if (savedInvite != null) {
                    verified = true
                    return@repeat
                }
            }

            if (!verified) {
                Log.w(TAG, "⚠️ Could not verify invite, but continuing")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save invite to Firebase", e)
            throw Exception("Firebase-Speicherung fehlgeschlagen: ${e.message}")
        }

        val link = "https://app.hackbert.org/invite?code=$inviteId"
        Log.d(TAG, "✅ Invite link created: $link")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        return link
    }
}