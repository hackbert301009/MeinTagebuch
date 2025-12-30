package com.example.meintagebuch

import android.util.Log
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirebaseManager {

    private const val TAG = "FirebaseManager"

    // WICHTIG: Ersetze diese URL mit deiner echten Firebase Database URL!
    private const val DATABASE_URL = "https://my-love-9c55d-default-rtdb.europe-west1.firebasedatabase.app"

    private val database: DatabaseReference by lazy {
        try {
            // Versuche mit spezifischer URL zu initialisieren
            val db = FirebaseDatabase.getInstance(DATABASE_URL).reference
            Log.d(TAG, "Firebase Database initialized with URL: $DATABASE_URL")
            db
        } catch (e: Exception) {
            // Fallback zur Standard-Instance
            Log.w(TAG, "Using default Firebase instance", e)
            FirebaseDatabase.getInstance().reference
        }
    }

    // ========================================
    // PARTNER INVITES
    // ========================================

    suspend fun createInvite(invite: PartnerInvite) {
        try {
            Log.d(TAG, "Creating invite: ${invite.inviteId}")
            database.child("invites").child(invite.inviteId).setValue(invite).await()
            Log.d(TAG, "✅ Invite created successfully in Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating invite", e)
            throw e
        }
    }

    suspend fun getInvite(inviteId: String): PartnerInvite? {
        return try {
            Log.d(TAG, "Getting invite: $inviteId from Firebase")
            val snapshot = database.child("invites").child(inviteId).get().await()
            val invite = snapshot.getValue(PartnerInvite::class.java)
            Log.d(TAG, "Invite retrieved: $invite")
            invite
        } catch (e: Exception) {
            Log.e(TAG, "Error getting invite", e)
            null
        }
    }

    suspend fun updateInvite(inviteId: String, name: String, accepted: Boolean) {
        try {
            Log.d(TAG, "Updating invite: $inviteId with name: $name")
            val updates = mapOf(
                "partnerName" to name,
                "accepted" to accepted
            )
            database.child("invites").child(inviteId).updateChildren(updates).await()
            Log.d(TAG, "✅ Invite updated successfully in Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating invite", e)
            throw e
        }
    }

    fun observeInvites(): Flow<List<PartnerInvite>> = callbackFlow {
        Log.d(TAG, "Starting to observe invites from Firebase")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val invites = mutableListOf<PartnerInvite>()
                for (child in snapshot.children) {
                    child.getValue(PartnerInvite::class.java)?.let {
                        invites.add(it)
                        Log.d(TAG, "Invite observed: $it")
                    }
                }
                Log.d(TAG, "Total invites observed: ${invites.size}")
                trySend(invites)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error observing invites: ${error.message}")
                close(error.toException())
            }
        }

        database.child("invites").addValueEventListener(listener)

        awaitClose {
            Log.d(TAG, "Stopping invite observation")
            database.child("invites").removeEventListener(listener)
        }
    }

    // ========================================
    // DIARY ENTRIES
    // ========================================

    suspend fun saveDiaryEntry(partnerId: String, entry: DiaryEntry) {
        try {
            val entryId = database.child("diary_entries").child(partnerId).push().key ?: return
            Log.d(TAG, "Saving diary entry: $entryId")
            database.child("diary_entries").child(partnerId).child(entryId).setValue(entry).await()
            Log.d(TAG, "✅ Diary entry saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving diary entry", e)
            throw e
        }
    }

    fun observeDiaryEntries(partnerId: String): Flow<List<DiaryEntry>> = callbackFlow {
        Log.d(TAG, "Starting to observe diary entries for: $partnerId")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = mutableListOf<DiaryEntry>()
                for (child in snapshot.children) {
                    child.getValue(DiaryEntry::class.java)?.let { entries.add(it) }
                }
                entries.sortByDescending { it.timestamp }
                Log.d(TAG, "Diary entries observed: ${entries.size}")
                trySend(entries)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error observing diary entries: ${error.message}")
                close(error.toException())
            }
        }

        database.child("diary_entries").child(partnerId).addValueEventListener(listener)

        awaitClose {
            Log.d(TAG, "Stopping diary entries observation")
            database.child("diary_entries").child(partnerId).removeEventListener(listener)
        }
    }

    // ========================================
    // THOUGHTS
    // ========================================

    suspend fun saveThought(partnerId: String, thought: ThoughtEntry) {
        try {
            val thoughtId = database.child("thoughts").child(partnerId).push().key ?: return
            Log.d(TAG, "Saving thought: $thoughtId")
            database.child("thoughts").child(partnerId).child(thoughtId).setValue(thought).await()
            Log.d(TAG, "✅ Thought saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving thought", e)
            throw e
        }
    }

    fun observeThoughts(partnerId: String): Flow<List<ThoughtEntry>> = callbackFlow {
        Log.d(TAG, "Starting to observe thoughts for: $partnerId")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val thoughts = mutableListOf<ThoughtEntry>()
                for (child in snapshot.children) {
                    child.getValue(ThoughtEntry::class.java)?.let { thoughts.add(it) }
                }
                Log.d(TAG, "Thoughts observed: ${thoughts.size}")
                trySend(thoughts)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error observing thoughts: ${error.message}")
                close(error.toException())
            }
        }

        database.child("thoughts").child(partnerId).addValueEventListener(listener)

        awaitClose {
            Log.d(TAG, "Stopping thoughts observation")
            database.child("thoughts").child(partnerId).removeEventListener(listener)
        }
    }
}