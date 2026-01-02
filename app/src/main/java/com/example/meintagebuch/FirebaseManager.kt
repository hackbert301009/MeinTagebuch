package com.example.meintagebuch

import android.util.Log
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirebaseManager {

    private const val TAG = "FirebaseManager"
    private const val DATABASE_URL = "https://my-love-9c55d-default-rtdb.europe-west1.firebasedatabase.app"

    private val database: DatabaseReference by lazy {
        try {
            val db = FirebaseDatabase.getInstance(DATABASE_URL).reference
            Log.d(TAG, "✅ Firebase initialized with URL: $DATABASE_URL")
            db
        } catch (e: Exception) {
            Log.w(TAG, "Using default Firebase instance", e)
            FirebaseDatabase.getInstance().reference
        }
    }

    // ========================================
    // PARTNER INVITES
    // ========================================

    suspend fun createInvite(invite: PartnerInvite) {
        try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "💾 Creating invite in Firebase")
            Log.d(TAG, "   Path: invites/${invite.inviteId}")
            Log.d(TAG, "   Creator: ${invite.creatorName}")
            Log.d(TAG, "   Acceptor: ${invite.acceptorName}")
            Log.d(TAG, "   Accepted: ${invite.accepted}")

            // Speichern mit inviteId als Key
            database.child("invites")
                .child(invite.inviteId)
                .setValue(invite)
                .await()

            Log.d(TAG, "✅ Invite saved to Firebase")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating invite", e)
            Log.e(TAG, "   Message: ${e.message}")
            Log.e(TAG, "   Stack: ${e.stackTraceToString()}")
            throw e
        }
    }

    suspend fun getInvite(inviteId: String): PartnerInvite? {
        return try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🔍 Getting invite from Firebase")
            Log.d(TAG, "   Path: invites/$inviteId")

            val snapshot = database.child("invites")
                .child(inviteId)
                .get()
                .await()

            Log.d(TAG, "📊 Snapshot exists: ${snapshot.exists()}")

            if (snapshot.exists()) {
                Log.d(TAG, "📦 Raw data from Firebase:")
                snapshot.children.forEach { child ->
                    Log.d(TAG, "   ${child.key}: ${child.value}")
                }

                // Daten manuell auslesen und Objekt erstellen
                val creatorName = snapshot.child("creatorName").getValue(String::class.java) ?: "Unknown"
                val acceptorName = snapshot.child("acceptorName").getValue(String::class.java) ?: "Unknown"
                val accepted = snapshot.child("accepted").getValue(Boolean::class.java) ?: false
                val inviteIdFromDb = snapshot.child("inviteId").getValue(String::class.java) ?: inviteId

                val invite = PartnerInvite(
                    inviteId = inviteIdFromDb,
                    creatorName = creatorName,
                    acceptorName = acceptorName,
                    accepted = accepted
                )

                Log.d(TAG, "✅ Invite found and parsed:")
                Log.d(TAG, "   ID: ${invite.inviteId}")
                Log.d(TAG, "   Creator: ${invite.creatorName}")
                Log.d(TAG, "   Acceptor: ${invite.acceptorName}")
                Log.d(TAG, "   Accepted: ${invite.accepted}")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                invite
            } else {
                Log.w(TAG, "⚠️ Invite not found in Firebase")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting invite", e)
            Log.e(TAG, "   Message: ${e.message}")
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            null
        }
    }

    suspend fun updateInviteAccept(inviteId: String, acceptorName: String, accepted: Boolean) {
        try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "🔄 Updating invite in Firebase")
            Log.d(TAG, "   Path: invites/$inviteId")
            Log.d(TAG, "   Acceptor: $acceptorName")
            Log.d(TAG, "   Accepted: $accepted")

            val updates = mapOf(
                "acceptorName" to acceptorName,
                "accepted" to accepted
            )

            database.child("invites")
                .child(inviteId)
                .updateChildren(updates)
                .await()

            Log.d(TAG, "✅ Invite updated in Firebase")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating invite", e)
            Log.e(TAG, "   Message: ${e.message}")
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            throw e
        }
    }

    fun observeInvites(): Flow<List<PartnerInvite>> = callbackFlow {
        Log.d(TAG, "👀 Starting to observe invites")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "🔥 Invites changed in Firebase")
                Log.d(TAG, "   Children count: ${snapshot.childrenCount}")

                val invites = mutableListOf<PartnerInvite>()

                for (child in snapshot.children) {
                    try {
                        Log.d(TAG, "   Processing child: ${child.key}")

                        // Manuell auslesen
                        val inviteId = child.child("inviteId").getValue(String::class.java) ?: child.key ?: ""
                        val creatorName = child.child("creatorName").getValue(String::class.java) ?: "Unknown"
                        val acceptorName = child.child("acceptorName").getValue(String::class.java) ?: "Unknown"
                        val accepted = child.child("accepted").getValue(Boolean::class.java) ?: false

                        val invite = PartnerInvite(
                            inviteId = inviteId,
                            creatorName = creatorName,
                            acceptorName = acceptorName,
                            accepted = accepted
                        )

                        invites.add(invite)

                        Log.d(TAG, "      ✅ Parsed: creator=$creatorName, acceptor=$acceptorName, accepted=$accepted")
                    } catch (e: Exception) {
                        Log.e(TAG, "      ❌ Error parsing invite", e)
                    }
                }

                Log.d(TAG, "📊 Total invites parsed: ${invites.size}")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                trySend(invites)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Error observing invites: ${error.message}")
                close(error.toException())
            }
        }

        database.child("invites").addValueEventListener(listener)

        awaitClose {
            Log.d(TAG, "🛑 Stopping invite observation")
            database.child("invites").removeEventListener(listener)
        }
    }

    // ========================================
    // DIARY ENTRIES
    // ========================================

    suspend fun saveDiaryEntry(partnerId: String, entry: DiaryEntry) {
        try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "💾 Saving diary entry to Firebase")
            Log.d(TAG, "   Path: diary_entries/$partnerId/${entry.id}")
            Log.d(TAG, "   Author: ${entry.authorId}")
            Log.d(TAG, "   Text: ${entry.text.take(50)}...")

            database.child("diary_entries")
                .child(partnerId)
                .child(entry.id)
                .setValue(entry)
                .await()

            Log.d(TAG, "✅ Diary entry saved")
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving diary entry", e)
            throw e
        }
    }

    fun observeDiaryEntries(partnerId: String): Flow<List<DiaryEntry>> = callbackFlow {
        Log.d(TAG, "👀 Starting to observe diary entries for: $partnerId")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "🔥 Diary entries changed")
                Log.d(TAG, "   Children count: ${snapshot.childrenCount}")

                val entries = mutableListOf<DiaryEntry>()

                for (child in snapshot.children) {
                    try {
                        // Manuell auslesen
                        val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
                        val text = child.child("text").getValue(String::class.java) ?: ""
                        val authorId = child.child("authorId").getValue(String::class.java) ?: "Unknown"
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()

                        val entry = DiaryEntry(
                            id = id,
                            text = text,
                            authorId = authorId,
                            timestamp = timestamp
                        )

                        entries.add(entry)

                        Log.d(TAG, "   Entry: ${entry.id.take(8)}... by ${entry.authorId}")
                    } catch (e: Exception) {
                        Log.e(TAG, "   ❌ Error parsing entry", e)
                    }
                }

                entries.sortByDescending { it.timestamp }
                Log.d(TAG, "📊 Total entries: ${entries.size}")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                trySend(entries)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Error observing diary entries: ${error.message}")
                close(error.toException())
            }
        }

        database.child("diary_entries").child(partnerId).addValueEventListener(listener)

        awaitClose {
            Log.d(TAG, "🛑 Stopping diary entries observation")
            database.child("diary_entries").child(partnerId).removeEventListener(listener)
        }
    }

    // ========================================
    // THOUGHTS
    // ========================================

    suspend fun saveThought(partnerId: String, thought: ThoughtEntry) {
        try {
            val thoughtId = database.child("thoughts").child(partnerId).push().key ?: return
            database.child("thoughts").child(partnerId).child(thoughtId).setValue(thought).await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving thought", e)
        }
    }

    fun observeThoughts(partnerId: String): Flow<List<ThoughtEntry>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val thoughts = mutableListOf<ThoughtEntry>()
                for (child in snapshot.children) {
                    child.getValue(ThoughtEntry::class.java)?.let { thoughts.add(it) }
                }
                trySend(thoughts)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        database.child("thoughts").child(partnerId).addValueEventListener(listener)

        awaitClose {
            database.child("thoughts").child(partnerId).removeEventListener(listener)
        }
    }
}