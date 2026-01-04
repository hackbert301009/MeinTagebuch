package com.example.meintagebuch

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "partner_invites")
data class PartnerInvite(
    @PrimaryKey val inviteId: String,
    val creatorName: String = "Unknown",
    val acceptorName: String = "Unknown",
    val accepted: Boolean = false,
    // NEU: User-IDs hinzugefügt
    val creatorUserId: String = "",           // User-ID des Einladenden
    val acceptorUserId: String = ""           // User-ID des Annehmenden
)