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

    // ⬅️ WICHTIG: 'database' ist jetzt PUBLIC damit InviteAcceptActivity darauf zugreifen kann!
    val database: DatabaseReference by lazy {
        try {
            val db = FirebaseDatabase.getInstance(DATABASE_URL).reference
            Log.d(TAG, "✅ Firebase initialized with URL: $DATABASE_URL")
            db
        } catch (e: Exception) {
            Log.w(TAG, "Using default Firebase instance", e)
            FirebaseDatabase.getInstance().reference
        }
    }

    // Alle anderen Funktionen bleiben gleich...
    // (Rest wie in firebase_manager_updated)

    suspend fun createInvite(invite: PartnerInvite) {
        try {
            Log.d(TAG, "💾 Creating invite in Firebase")
            database.child("invites").child(invite.inviteId).setValue(invite).await()
            Log.d(TAG, "✅ Invite saved to Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating invite", e)
            throw e
        }
    }

    suspend fun getInvite(inviteId: String): PartnerInvite? {
        return try {
            Log.d(TAG, "🔍 Getting invite from Firebase: $inviteId")
            val snapshot = database.child("invites").child(inviteId).get().await()

            if (snapshot.exists()) {
                val creatorName = snapshot.child("creatorName").getValue(String::class.java) ?: "Unknown"
                val acceptorName = snapshot.child("acceptorName").getValue(String::class.java) ?: "Unknown"
                val accepted = snapshot.child("accepted").getValue(Boolean::class.java) ?: false
                val inviteIdFromDb = snapshot.child("inviteId").getValue(String::class.java) ?: inviteId
                val creatorUserId = snapshot.child("creatorUserId").getValue(String::class.java) ?: ""
                val acceptorUserId = snapshot.child("acceptorUserId").getValue(String::class.java) ?: ""

                PartnerInvite(
                    inviteId = inviteIdFromDb,
                    creatorName = creatorName,
                    acceptorName = acceptorName,
                    accepted = accepted,
                    creatorUserId = creatorUserId,
                    acceptorUserId = acceptorUserId
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting invite", e)
            null
        }
    }

    suspend fun updateInviteAccept(inviteId: String, acceptorName: String, acceptorUserId: String, accepted: Boolean) {
        try {
            val updates = mapOf(
                "acceptorName" to acceptorName,
                "acceptorUserId" to acceptorUserId,
                "accepted" to accepted
            )
            database.child("invites").child(inviteId).updateChildren(updates).await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating invite", e)
            throw e
        }
    }

    fun observeInvites(): Flow<List<PartnerInvite>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val invites = mutableListOf<PartnerInvite>()
                for (child in snapshot.children) {
                    try {
                        val inviteId = child.child("inviteId").getValue(String::class.java) ?: child.key ?: ""
                        val creatorName = child.child("creatorName").getValue(String::class.java) ?: "Unknown"
                        val acceptorName = child.child("acceptorName").getValue(String::class.java) ?: "Unknown"
                        val accepted = child.child("accepted").getValue(Boolean::class.java) ?: false
                        val creatorUserId = child.child("creatorUserId").getValue(String::class.java) ?: ""
                        val acceptorUserId = child.child("acceptorUserId").getValue(String::class.java) ?: ""

                        invites.add(PartnerInvite(inviteId, creatorName, acceptorName, accepted, creatorUserId, acceptorUserId))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing invite", e)
                    }
                }
                trySend(invites)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        database.child("invites").addValueEventListener(listener)
        awaitClose { database.child("invites").removeEventListener(listener) }
    }

    suspend fun createPartnership(partnership: Partnership) {
        try {
            Log.d(TAG, "💑 Creating partnership in Firebase")

            database.child("partnerships")
                .child(partnership.myUserId)
                .child(partnership.partnershipId)
                .setValue(partnership)
                .await()

            database.child("partnerships")
                .child(partnership.partnerUserId)
                .child(partnership.partnershipId)
                .setValue(partnership)
                .await()

            Log.d(TAG, "✅ Partnership created")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating partnership", e)
            throw e
        }
    }

    suspend fun disconnectPartnership(partnershipId: String, myUserId: String, partnerUserId: String) {
        try {
            Log.d(TAG, "💔 Disconnecting partnership: $partnershipId")

            val updates = mapOf("active" to false)

            database.child("partnerships")
                .child(myUserId)
                .child(partnershipId)
                .updateChildren(updates)
                .await()

            database.child("partnerships")
                .child(partnerUserId)
                .child(partnershipId)
                .updateChildren(updates)
                .await()

            Log.d(TAG, "✅ Partnership disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error disconnecting partnership", e)
            throw e
        }
    }

    fun observePartnerships(userId: String): Flow<List<Partnership>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val partnerships = mutableListOf<Partnership>()

                for (child in snapshot.children) {
                    try {
                        val partnershipId = child.child("partnershipId").getValue(String::class.java) ?: ""
                        val myUserId = child.child("myUserId").getValue(String::class.java) ?: ""
                        val partnerUserId = child.child("partnerUserId").getValue(String::class.java) ?: ""
                        val myName = child.child("myName").getValue(String::class.java) ?: ""
                        val partnerName = child.child("partnerName").getValue(String::class.java) ?: ""
                        val createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L
                        val active = child.child("active").getValue(Boolean::class.java) ?: true

                        partnerships.add(Partnership(
                            partnershipId, myUserId, partnerUserId, myName, partnerName, createdAt, active
                        ))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing partnership", e)
                    }
                }

                trySend(partnerships)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        database.child("partnerships").child(userId).addValueEventListener(listener)
        awaitClose { database.child("partnerships").child(userId).removeEventListener(listener) }
    }

    suspend fun saveDiaryEntry(partnershipId: String, entry: DiaryEntry) {
        try {
            database.child("diary_entries")
                .child(partnershipId)
                .child(entry.id)
                .setValue(entry)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving diary entry", e)
            throw e
        }
    }

    fun observeDiaryEntries(partnershipId: String): Flow<List<DiaryEntry>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = mutableListOf<DiaryEntry>()

                for (child in snapshot.children) {
                    try {
                        val id = child.child("id").getValue(String::class.java) ?: ""
                        val text = child.child("text").getValue(String::class.java) ?: ""
                        val authorId = child.child("authorId").getValue(String::class.java) ?: ""
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L

                        entries.add(DiaryEntry(id, text, authorId, timestamp))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing entry", e)
                    }
                }

                entries.sortByDescending { it.timestamp }
                trySend(entries)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        database.child("diary_entries").child(partnershipId).addValueEventListener(listener)
        awaitClose { database.child("diary_entries").child(partnershipId).removeEventListener(listener) }
    }

    suspend fun deleteAllDiaryEntriesForPartnership(partnershipId: String) {
        try {
            database.child("diary_entries").child(partnershipId).removeValue().await()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting diary entries", e)
            throw e
        }
    }

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
        awaitClose { database.child("thoughts").child(partnerId).removeEventListener(listener) }
    }
}