package my.love.meintagebuch

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meintagebuch.AppDatabase
import com.example.meintagebuch.DayStatistic
import com.example.meintagebuch.R
import com.example.meintagebuch.StatisticsAdapter
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StatisticsActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var adapter: StatisticsAdapter
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        database = AppDatabase.Companion.getDatabase(this)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "📊 Statistiken"

        toolbar.setNavigationOnClickListener {
            finish()
        }

        val recyclerView: RecyclerView = findViewById(R.id.statisticsRecyclerView)
        emptyText = findViewById(R.id.emptyText)

        adapter = StatisticsAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadStatistics()
    }

    private fun loadStatistics() {
        database.thoughtDao().getAllThoughts().observe(this) { thoughts ->
            if (thoughts.isEmpty()) {
                emptyText.visibility = TextView.VISIBLE
                return@observe
            }

            emptyText.visibility = TextView.GONE

            // Gruppiere Gedanken nach Tagen
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
            val groupedByDay = thoughts.groupBy { thought ->
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = thought.timestamp
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }

            val statistics = groupedByDay.map { (timestamp, thoughtsOfDay) ->
                DayStatistic(
                    date = dateFormat.format(Date(timestamp)),
                    count = thoughtsOfDay.size,
                    timestamp = timestamp
                )
            }.sortedByDescending { it.timestamp }

            adapter.setStatistics(statistics)
        }
    }
}