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
    private val partnerId = "shared_partner_id"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diary_list)

        database = AppDatabase.getDatabase(this)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "📓 Tagebuch"

        toolbar.setNavigationOnClickListener {
            finish()
        }

        val recyclerView: RecyclerView = findViewById(R.id.diaryRecyclerView)
        val addButton: FloatingActionButton = findViewById(R.id.addDiaryButton)
        emptyText = findViewById(R.id.emptyText)

        adapter = DiaryAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Firebase-Einträge beobachten und synchronisieren
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Starting Firebase sync for diary entries")
                FirebaseManager.observeDiaryEntries(partnerId).collect { firebaseEntries ->
                    Log.d(TAG, "📥 Received ${firebaseEntries.size} entries from Firebase")

                    firebaseEntries.forEach { entry ->
                        try {
                            val existingEntry = database.diaryDao().getEntryById(entry.id)
                            if (existingEntry == null) {
                                Log.d(TAG, "📝 New entry from Firebase: ${entry.id} by ${entry.authorId}")
                                database.diaryDao().insert(entry)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing entry: ${entry.id}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing Firebase entries", e)
            }
        }

        // Lokale Einträge beobachten
        database.diaryDao().getAllEntries().observe(this) { entries ->
            Log.d(TAG, "📊 Local entries: ${entries.size}")
            if (entries.isEmpty()) {
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

    private fun showAddEntryDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_entry, null)
        val editText = dialogView.findViewById<EditText>(R.id.entryEditText)

        AlertDialog.Builder(this)
            .setTitle("✏️ Neuer Tagebuch-Eintrag")
            .setView(dialogView)
            .setPositiveButton("Speichern") { _, _ ->
                val text = editText.text.toString()
                if (text.isNotBlank()) {
                    lifecycleScope.launch {
                        try {
                            // Meinen Namen verwenden
                            val myName = PartnerNameHelper.getMyName(this@DiaryListActivity)
                                .ifBlank { "Ich" }

                            val entryId = UUID.randomUUID().toString()

                            val entry = DiaryEntry(
                                id = entryId,
                                text = text,
                                authorId = myName  // Mein Name als Autor
                            )

                            Log.d(TAG, "💾 Saving entry by: $myName")

                            // In Firebase speichern
                            FirebaseManager.saveDiaryEntry(partnerId, entry)

                            // Lokal speichern
                            database.diaryDao().insert(entry)

                            Log.d(TAG, "✅ Entry saved")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error saving entry", e)
                        }
                    }
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
}