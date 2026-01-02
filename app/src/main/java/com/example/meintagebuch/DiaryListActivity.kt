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

        // Firebase-Einträge beobachten und synchronisieren
        lifecycleScope.launch {
            try {
                Log.d(TAG, "👀 Starting Firebase sync for diary entries")

                FirebaseManager.observeDiaryEntries(partnerId).collect { firebaseEntries ->
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.d(TAG, "🔥 Received ${firebaseEntries.size} entries from Firebase")

                    firebaseEntries.forEach { entry ->
                        try {
                            Log.d(TAG, "   Entry ${entry.id.take(8)}:")
                            Log.d(TAG, "      Author: '${entry.authorId}'")
                            Log.d(TAG, "      Text: ${entry.text.take(30)}...")

                            val existingEntry = database.diaryDao().getEntryById(entry.id)
                            if (existingEntry == null) {
                                Log.d(TAG, "      → New entry, inserting")
                                database.diaryDao().insert(entry)
                            } else {
                                Log.d(TAG, "      → Entry already exists")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "   ❌ Error syncing entry: ${entry.id}", e)
                        }
                    }
                    Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error observing Firebase entries", e)
            }
        }

        // Lokale Einträge beobachten
        database.diaryDao().getAllEntries().observe(this) { entries ->
            Log.d(TAG, "📊 Local entries updated: ${entries.size}")

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
                // Meinen Namen verwenden (getrimmt!)
                val myName = PartnerNameHelper.getMyName(this@DiaryListActivity).trim()
                    .ifBlank { "Ich" }

                val entryId = UUID.randomUUID().toString()

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "💾 Saving new entry")
                Log.d(TAG, "   ID: $entryId")
                Log.d(TAG, "   Author (me): '$myName'")
                Log.d(TAG, "   Text: ${text.take(50)}...")

                val entry = DiaryEntry(
                    id = entryId,
                    text = text,
                    authorId = myName  // Mein Name als Autor (getrimmt!)
                )

                // In Firebase speichern
                Log.d(TAG, "🔄 Saving to Firebase...")
                FirebaseManager.saveDiaryEntry(partnerId, entry)
                Log.d(TAG, "✅ Saved to Firebase")

                // Lokal speichern
                Log.d(TAG, "🔄 Saving locally...")
                database.diaryDao().insert(entry)
                Log.d(TAG, "✅ Saved locally")

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving entry", e)
                Log.e(TAG, "   Message: ${e.message}")
                Log.e(TAG, "   Stack: ${e.stackTraceToString()}")
            }
        }
    }
}