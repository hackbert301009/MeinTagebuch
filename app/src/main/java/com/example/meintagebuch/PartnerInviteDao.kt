package com.example.meintagebuch
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.lifecycle.LiveData
import androidx.room.*
@Dao
interface PartnerInviteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invite: PartnerInvite)

    @Query("SELECT * FROM partner_invites")
    fun getAllInvites(): LiveData<List<PartnerInvite>>

    @Query("UPDATE partner_invites SET accepted = 1 WHERE inviteId = :id")
    suspend fun markAccepted(id: String)

    @Query("SELECT * FROM partner_invites WHERE inviteId = :id LIMIT 1")
    suspend fun getInviteById(id: String): PartnerInvite?
}
