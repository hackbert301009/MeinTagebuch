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

class MainActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var thoughtCountText: TextView
    private lateinit var totalThoughtsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                thoughtCountText.text = "$count Mal"
            }
    }

    private fun observeTotalThoughts() {
        database.thoughtDao().getAllThoughts().observe(this) { thoughts ->
            totalThoughtsText.text = "Insgesamt: ${thoughts.size} Gedanken"
        }
    }

    override fun onResume() {
        super.onResume()
        observeTodayThoughts()
        observeTotalThoughts()
    }
}