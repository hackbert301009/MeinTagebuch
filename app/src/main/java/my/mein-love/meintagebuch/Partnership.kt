
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Repräsentiert eine aktive Partnerschaft zwischen zwei Usern
 */
@Entity(tableName = "partnerships")
data class Partnership(
    @PrimaryKey val partnershipId: String,  // Eindeutige ID der Partnerschaft
    val myUserId: String,                    // Meine User-ID
    val partnerUserId: String,               // Partner User-ID
    val myName: String,                      // Mein Name
    val partnerName: String,                 // Partner Name
    val createdAt: Long = System.currentTimeMillis(),
    val active: Boolean = true               // Ob die Partnerschaft noch aktiv ist
)