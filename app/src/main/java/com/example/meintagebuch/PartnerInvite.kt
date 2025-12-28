package com.example.meintagebuch

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "partner_invites")
data class PartnerInvite(
    @PrimaryKey val inviteId: String,
    val partnerName: String,
    val accepted: Boolean
)
