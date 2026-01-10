
import android.content.Context
import java.util.UUID

/**
 * Verwaltet eine eindeutige User-ID pro Gerät
 * Diese ID identifiziert den User in Firebase
 */
object UserIdHelper {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_USER_ID = "user_id"

    /**
     * Holt oder erstellt die User-ID für dieses Gerät
     * Diese ID wird beim ersten Start generiert und bleibt dann konstant
     */
    fun getUserId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var userId = prefs.getString(KEY_USER_ID, null)

        if (userId == null) {
            // Erste Installation - neue User-ID erstellen
            userId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_USER_ID, userId).apply()
        }

        return userId
    }
}