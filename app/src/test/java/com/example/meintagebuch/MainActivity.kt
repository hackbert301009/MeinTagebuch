package com.example.meintagebuch

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.util.*
import android.content.Context
import android.content.res.Configuration
import android.widget.ImageView
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var thoughtCountText: TextView
    private lateinit var totalThoughtsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // WICHTIG: Sprache VOR setContentView laden
        loadLanguagePreference()

        setContentView(R.layout.activity_main)

        // Settings Button
        val settingsButton: ImageView = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        database = AppDatabase.getDatabase(this)

        // UI Elemente
        val thoughtButton: Button = findViewById(R.id.thoughtButton)
        thoughtCountText = findViewById(R.id.thoughtCountText)
        totalThoughtsText = findViewById(R.id.totalThoughtsText)

        val statisticsCard: MaterialCardView = findViewById(R.id.statisticsCard)
        val diaryCard: MaterialCardView = findViewById(R.id.diaryCard)
        val photosCard: MaterialCardView = findViewById(R.id.photosCard)
        val partnerCard: MaterialCardView = findViewById(R.id.partnerCard)

        // Gedanken-Button
        thoughtButton.setOnClickListener {
            lifecycleScope.launch {
                database.thoughtDao().insert(ThoughtEntry())
            }
        }

        // Beobachte Gedanken für heute
        observeTodayThoughts()

        // Beobachte Gesamt-Gedanken
        observeTotalThoughts()

        // Navigation zu anderen Screens
        statisticsCard.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        diaryCard.setOnClickListener {
            startActivity(Intent(this, DiaryListActivity::class.java))
        }

        photosCard.setOnClickListener {
            startActivity(Intent(this, PhotoGalleryActivity::class.java))
        }

        partnerCard.setOnClickListener {
            startActivity(Intent(this, PartnerActivity::class.java))
        }
    }

    private fun observeTodayThoughts() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        database.thoughtDao().getThoughtsCountForDay(startOfDay, endOfDay)
            .observe(this) { count ->
                // String-Ressource verwenden für Übersetzung
                thoughtCountText.text = getString(R.string.thought_count, count)
            }
    }

    private fun observeTotalThoughts() {
        database.thoughtDao().getAllThoughts().observe(this) { thoughts ->
            // String-Ressource verwenden für Übersetzung
            totalThoughtsText.text = getString(R.string.total_thoughts, thoughts.size)
        }
    }

    override fun onResume() {
        super.onResume()
        observeTodayThoughts()
        observeTotalThoughts()
    }

    private fun loadLanguagePreference() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "de") ?: "de"

        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}