package com.example.meintagebuch

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.UUID

class DiaryListActivity : AppCompatActivity() {

    private val TAG = "DiaryListActivity"
    private lateinit var database: AppDatabase
    private lateinit var adapter: DiaryAdapter
    private lateinit var emptyText: TextView
    private var currentPartnershipId: String? = null
    private var myUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_list)

        database = AppDatabase.getDatabase(this)
        myUserId = UserIdHelper.getUserId(this)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "📔 Tagebuch"

        toolbar.setNavigationOnClickListener {
            finish()
        }

        val recyclerView: RecyclerView = findViewById(R.id.diaryRecyclerView)
        val addButton: FloatingActionButton = findViewById(R.id.addDiaryButton)
        emptyText = findViewById(R.id.emptyText)

        adapter = DiaryAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Prüfen ob Partnership existiert
        lifecycleScope.launch {
            val partnership = database.partnershipDao().getFirstActivePartnership()

            if (partnership != null) {
                // Hat Partner - gemeinsames Tagebuch
                currentPartnershipId = partnership.partnershipId
                Log.d(TAG, "✅ Has partnership: ${partnership.partnershipId}")
                Log.d(TAG, "   Partner: ${partnership.partnerName}")

                toolbar.subtitle = "Mit ${partnership.partnerName}"

                // Firebase-Einträge für diese Partnerschaft beobachten
                observeFirebaseEntries(partnership.partnershipId)
            } else {
                // Kein Partner - persönliches Tagebuch
                currentPartnershipId = "personal_$myUserId"
                Log.d(TAG, "ℹ️ No partnership - using personal diary")

                toolbar.subtitle = "Persönliches Tagebuch"

                // Optionaler Firebase-Sync auch für Singles
                observeFirebaseEntries(currentPartnershipId!!)
            }
        }

        // Lokale Einträge beobachten
        database.diaryDao().getAllEntries().observe(this) { entries ->
            Log.d(TAG, "📊 Local entries updated: ${entries.size}")

            if (entries.isEmpty()) {
                emptyText.text = "Noch keine Einträge. Erstelle deinen ersten Eintrag!"
                emptyText.visibility = TextView.VISIBLE
            } else {
                emptyText.visibility = TextView.GONE
            }
            adapter.setEntries(entries)
        }

        addButton.setOnClickListener {
            showAddEntryDialog()
        }
    }

    private fun observeFirebaseEntries(partnershipId: String) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "👀 Starting Firebase sync for: $partnershipId")

                FirebaseManager.observeDiaryEntries(partnershipId).collect { firebaseEntries ->
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "🔥 Received ${firebaseEntries.size} entries from Firebase")

                    firebaseEntries.forEach { entry ->
                        try {
                            val existingEntry = database.diaryDao().getEntryById(entry.id)
                            if (existingEntry == null) {
                                Log.d(TAG, "   → New entry, inserting: ${entry.text.take(30)}...")
                                database.diaryDao().insert(entry)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "   ❌ Error syncing entry", e)
                        }
                    }
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error observing Firebase entries", e)
                Log.e(TAG, "   This is OK - Firebase might not be accessible")
                // WICHTIG: Keine Toast-Nachricht - App funktioniert auch ohne Firebase
            }
        }
    }

    private fun showAddEntryDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_entry, null)
        val editText = dialogView.findViewById<EditText>(R.id.entryEditText)

        AlertDialog.Builder(this)
            .setTitle("✏️ Neuer Tagebuch-Eintrag")
            .setView(dialogView)
            .setPositiveButton("Speichern") { _, _ ->
                val text = editText.text.toString().trim()
                if (text.isNotBlank()) {
                    saveNewEntry(text)
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun saveNewEntry(text: String) {
        lifecycleScope.launch {
            try {
                val myName = PartnerNameHelper.getMyName(this@DiaryListActivity).trim()
                    .ifBlank { "Ich" }

                val entryId = UUID.randomUUID().toString()

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "💾 Saving new entry")
                Log.d(TAG, "   Partnership/Personal ID: $currentPartnershipId")
                Log.d(TAG, "   Author: $myName")
                Log.d(TAG, "   Text: ${text.take(50)}...")

                val entry = DiaryEntry(
                    id = entryId,
                    text = text,
                    authorId = myName
                )

                // ZUERST lokal speichern (funktioniert immer!)
                Log.d(TAG, "💾 Saving locally...")
                database.diaryDao().insert(entry)
                Log.d(TAG, "✅ Saved locally")

                // DANN Firebase versuchen (optional)
                currentPartnershipId?.let { partnershipId ->
                    try {
                        Log.d(TAG, "☁️ Trying to save to Firebase...")
                        FirebaseManager.saveDiaryEntry(partnershipId, entry)
                        Log.d(TAG, "✅ Saved to Firebase")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Firebase save failed, but entry is saved locally", e)
                        // WICHTIG: Kein Fehler anzeigen - lokales Speichern hat funktioniert!
                    }
                }

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving entry", e)
                runOnUiThread {
                    AlertDialog.Builder(this@DiaryListActivity)
                        .setTitle("Fehler")
                        .setMessage("Eintrag konnte nicht gespeichert werden: ${e.message}")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }
}