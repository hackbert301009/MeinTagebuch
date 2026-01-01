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
            Log.d(TAG, "✅ Firebase initialized")
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
            Log.d(TAG, "💾 Creating invite: ${invite.inviteId}")
            database.child("invites").child(invite.inviteId).setValue(invite).await()
            Log.d(TAG, "✅ Invite created")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating invite", e)
            throw e
        }
    }

    suspend fun getInvite(inviteId: String): PartnerInvite? {
        return try {
            Log.d(TAG, "📥 Getting invite: $inviteId")
            val snapshot = database.child("invites").child(inviteId).get().await()
            val invite = snapshot.getValue(PartnerInvite::class.java)
            Log.d(TAG, "Invite: $invite")
            invite
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting invite", e)
            null
        }
    }

    suspend fun updateInviteAccept(inviteId: String, acceptorName: String, accepted: Boolean) {
        try {
            Log.d(TAG, "💾 Updating invite: $inviteId with acceptor: $acceptorName")
            val updates = mapOf(
                "acceptorName" to acceptorName,
                "accepted" to accepted
            )
            database.child("invites").child(inviteId).updateChildren(updates).await()
            Log.d(TAG, "✅ Invite updated")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating invite", e)
            throw e
        }
    }

    fun observeInvites(): Flow<List<PartnerInvite>> = callbackFlow {
        Log.d(TAG, "👀 Observing invites")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val invites = mutableListOf<PartnerInvite>()
                for (child in snapshot.children) {
                    try {
                        child.getValue(PartnerInvite::class.java)?.let {
                            invites.add(it)
                            Log.d(TAG, "Invite: creator=${it.creatorName}, acceptor=${it.acceptorName}, accepted=${it.accepted}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing invite", e)
                    }
                }
                Log.d(TAG, "📥 Total invites: ${invites.size}")
                trySend(invites)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Error observing invites: ${error.message}")
                close(error.toException())
            }
        }

        database.child("invites").addValueEventListener(listener)

        awaitClose {
            database.child("invites").removeEventListener(listener)
        }
    }

    // ========================================
    // DIARY ENTRIES
    // ========================================

    suspend fun saveDiaryEntry(partnerId: String, entry: DiaryEntry) {
        try {
            Log.d(TAG, "💾 Saving diary entry: ${entry.id}")
            database.child("diary_entries")
                .child(partnerId)
                .child(entry.id)
                .setValue(entry)
                .await()
            Log.d(TAG, "✅ Entry saved")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving entry", e)
            throw e
        }
    }

    fun observeDiaryEntries(partnerId: String): Flow<List<DiaryEntry>> = callbackFlow {
        Log.d(TAG, "👀 Observing diary entries")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = mutableListOf<DiaryEntry>()
                for (child in snapshot.children) {
                    try {
                        child.getValue(DiaryEntry::class.java)?.let { entries.add(it) }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing entry", e)
                    }
                }
                entries.sortByDescending { it.timestamp }
                Log.d(TAG, "📥 Entries: ${entries.size}")
                trySend(entries)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ Error observing entries: ${error.message}")
                close(error.toException())
            }
        }

        database.child("diary_entries").child(partnerId).addValueEventListener(listener)

        awaitClose {
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