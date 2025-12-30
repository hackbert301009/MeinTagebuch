package com.example.meintagebuch

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
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

class DiaryListActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var adapter: DiaryAdapter
    private lateinit var emptyText: TextView
    private val partnerId = "shared_partner_id"  // Gemeinsame Partner-ID

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

        // Firebase-Einträge beobachten und mit lokaler DB synchronisieren
        lifecycleScope.launch {
            FirebaseManager.observeDiaryEntries(partnerId).collect { firebaseEntries ->
                // Firebase-Daten in lokale DB synchronisieren
                firebaseEntries.forEach { entry ->
                    // Prüfen ob Eintrag bereits lokal existiert (vereinfacht)
                    // In echter App würde man eine eindeutige ID verwenden
                    database.diaryDao().insert(entry)
                }
            }
        }

        // Lokale Tagebuch-Einträge beobachten
        database.diaryDao().getAllEntries().observe(this) { entries ->
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
                        val authorName = getAuthorName()
                        val entry = DiaryEntry(
                            text = text,
                            authorId = authorName
                        )

                        // Lokal speichern
                        database.diaryDao().insert(entry)

                        // In Firebase speichern
                        FirebaseManager.saveDiaryEntry(partnerId, entry)
                    }
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun getAuthorName(): String {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("my_name", "Ich") ?: "Ich"
    }
}