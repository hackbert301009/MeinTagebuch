package com.example.meintagebuch

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PartnerInviteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invite: PartnerInvite)

    @Query("SELECT * FROM partner_invites")
    fun getAllInvites(): LiveData<List<PartnerInvite>>

    @Query("SELECT * FROM partner_invites WHERE inviteId = :inviteId LIMIT 1")
    suspend fun getInviteById(inviteId: String): PartnerInvite?

    @Query("""
        UPDATE partner_invites 
        SET partnerName = :name, accepted = 1 
        WHERE inviteId = :inviteId
    """)
    suspend fun updateNameAndAccept(inviteId: String, name: String)
}
