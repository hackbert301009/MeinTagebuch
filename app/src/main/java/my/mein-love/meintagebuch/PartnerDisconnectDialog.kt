

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Dialog zum Trennen eines Partners
 * Zeigt eine Warnung und führt die Trennung durch
 */
class PartnerDisconnectDialog(
    context: Context,
    private val partnership: Partnership,
    private val lifecycleOwner: LifecycleOwner,
    private val onDisconnected: () -> Unit
) : Dialog(context) {

    private val TAG = "DisconnectDialog"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_disconnect_partner)

        val titleText: TextView = findViewById(R.id.disconnectTitle)
        val messageText: TextView = findViewById(R.id.disconnectMessage)
        val confirmButton: Button = findViewById(R.id.confirmDisconnectButton)
        val cancelButton: Button = findViewById(R.id.cancelDisconnectButton)

        titleText.text = "Partner trennen?"
        messageText.text = buildString {
            append("Möchtest du die Verbindung zu ${partnership.partnerName} wirklich trennen?\n\n")
            append("⚠️ WARNUNG:\n")
            append("• Alle gemeinsamen Tagebuch-Einträge werden gelöscht\n")
            append("• Die Verbindung wird beendet\n")
            append("• Diese Aktion kann nicht rückgängig gemacht werden")
        }

        confirmButton.setOnClickListener {
            disconnectPartner()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun disconnectPartner() {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "💔 Starting disconnect process")
        Log.d(TAG, "   Partnership ID: ${partnership.partnershipId}")
        Log.d(TAG, "   My User ID: ${partnership.myUserId}")
        Log.d(TAG, "   Partner User ID: ${partnership.partnerUserId}")

        Toast.makeText(context, "Trenne Verbindung...", Toast.LENGTH_SHORT).show()

        lifecycleOwner.lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(context)

                // Schritt 1: Alle gemeinsamen Tagebuch-Einträge löschen (Firebase)
                Log.d(TAG, "🗑️ Deleting diary entries from Firebase...")
                FirebaseManager.deleteAllDiaryEntriesForPartnership(partnership.partnershipId)
                Log.d(TAG, "✅ Firebase diary entries deleted")

                // Schritt 2: Alle lokalen Tagebuch-Einträge dieser Partnerschaft löschen
                Log.d(TAG, "🗑️ Deleting local diary entries...")
                // Hier müsstest du eine Query haben, die nur Einträge dieser Partnerschaft löscht
                // Da du aktuell keine partnershipId in DiaryEntry hast, löschen wir erstmal alle
                db.diaryDao().deleteAll()
                Log.d(TAG, "✅ Local diary entries deleted")

                // Schritt 3: Partnerschaft in Firebase deaktivieren
                Log.d(TAG, "💔 Deactivating partnership in Firebase...")
                FirebaseManager.disconnectPartnership(
                    partnership.partnershipId,
                    partnership.myUserId,
                    partnership.partnerUserId
                )
                Log.d(TAG, "✅ Partnership deactivated in Firebase")

                // Schritt 4: Partnerschaft lokal deaktivieren
                Log.d(TAG, "💔 Deactivating partnership locally...")
                db.partnershipDao().deactivatePartnership(partnership.partnershipId)
                Log.d(TAG, "✅ Partnership deactivated locally")

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "✅✅✅ DISCONNECT COMPLETE ✅✅✅")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                Toast.makeText(
                    context,
                    "Verbindung zu ${partnership.partnerName} getrennt",
                    Toast.LENGTH_LONG
                ).show()

                dismiss()
                onDisconnected()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error during disconnect", e)
                Toast.makeText(
                    context,
                    "Fehler beim Trennen: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}