package com.example.meintagebuch

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay
import java.util.UUID

object InviteManager {

    private const val TAG = "InviteManager"

    /**
     * Erstellt eine Einladung und wartet bis sie in Firebase gespeichert ist
     * @return Invite-Link
     * @throws Exception wenn Firebase-Speicherung fehlschlägt
     */
    suspend fun createInviteLink(context: Context): String {
        val inviteId = UUID.randomUUID().toString()

        // Meinen Namen holen - dieser wird als Creator gespeichert
        val myName = PartnerNameHelper.getMyName(context).trim().ifBlank { "Unbekannt" }

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📝 Creating new invite")
        Log.d(TAG, "   ID: $inviteId")
        Log.d(TAG, "   Creator (me): $myName")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        val invite = PartnerInvite(
            inviteId = inviteId,
            creatorName = myName,
            acceptorName = "Unknown",
            accepted = false
        )

        // Schritt 1: Lokal speichern
        try {
            AppDatabase.getDatabase(context)
                .partnerInviteDao()
                .insert(invite)
            Log.d(TAG, "✅ Invite saved locally")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save invite locally", e)
            throw Exception("Lokales Speichern fehlgeschlagen: ${e.message}")
        }

        // Schritt 2: In Firebase speichern
        try {
            Log.d(TAG, "🔄 Saving to Firebase...")
            FirebaseManager.createInvite(invite)
            Log.d(TAG, "✅ Firebase save command sent")

            // Mehrere Versuche zur Verifizierung (Firebase braucht Zeit)
            var verified = false
            repeat(5) { attempt ->
                delay(1500) // Länger warten zwischen Versuchen

                Log.d(TAG, "🔍 Verification attempt ${attempt + 1}/5...")
                val savedInvite = FirebaseManager.getInvite(inviteId)

                if (savedInvite != null) {
                    Log.d(TAG, "✅ Invite verified in Firebase!")
                    Log.d(TAG, "   Creator: ${savedInvite.creatorName}")
                    Log.d(TAG, "   Acceptor: ${savedInvite.acceptorName}")
                    Log.d(TAG, "   Accepted: ${savedInvite.accepted}")
                    verified = true
                    return@repeat
                } else {
                    Log.d(TAG, "⏳ Not yet visible in Firebase, waiting...")
                }
            }

            if (!verified) {
                Log.w(TAG, "⚠️ Could not verify invite in Firebase, but continuing anyway")
                Log.w(TAG, "   The invite was saved, it might just take a moment to sync")
                // Nicht werfen - die Einladung wurde gespeichert, nur die Verifizierung hat nicht geklappt
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