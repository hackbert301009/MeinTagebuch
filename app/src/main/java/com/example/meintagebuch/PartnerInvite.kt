package com.example.meintagebuch

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "partner_invites")
data class PartnerInvite(
    @PrimaryKey val inviteId: String,
    val creatorName: String = "Unknown",      // Name des Einladenden
    val acceptorName: String = "Unknown",     // Name des Annehmenden
    val accepted: Boolean = false
)