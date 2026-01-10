
import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PartnershipDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(partnership: Partnership)

    @Query("SELECT * FROM partnerships WHERE active = 1")
    fun getActivePartnerships(): LiveData<List<Partnership>>

    @Query("SELECT * FROM partnerships WHERE partnershipId = :partnershipId LIMIT 1")
    suspend fun getPartnershipById(partnershipId: String): Partnership?

    @Query("UPDATE partnerships SET active = 0 WHERE partnershipId = :partnershipId")
    suspend fun deactivatePartnership(partnershipId: String)

    @Query("DELETE FROM partnerships WHERE partnershipId = :partnershipId")
    suspend fun deletePartnership(partnershipId: String)

    @Query("SELECT * FROM partnerships WHERE active = 1 LIMIT 1")
    suspend fun getFirstActivePartnership(): Partnership?
}