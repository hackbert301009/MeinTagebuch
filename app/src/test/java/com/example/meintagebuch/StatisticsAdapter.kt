package com.example.meintagebuch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

data class DayStatistic(
    val date: String,
    val count: Int,
    val timestamp: Long
)

class StatisticsAdapter : RecyclerView.Adapter<StatisticsAdapter.StatisticViewHolder>() {

    private var statistics = emptyList<DayStatistic>()

    class StatisticViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateView: TextView = itemView.findViewById(R.id.statisticDate)
        val countView: TextView = itemView.findViewById(R.id.statisticCount)
        val dayNameView: TextView = itemView.findViewById(R.id.statisticDayName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatisticViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_statistic_day, parent, false)
        return StatisticViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatisticViewHolder, position: Int) {
        val statistic = statistics[position]

        holder.countView.text = "${statistic.count}×"

        // Wochentag anzeigen
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = statistic.timestamp
        val dayName = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Montag"
            Calendar.TUESDAY -> "Dienstag"
            Calendar.WEDNESDAY -> "Mittwoch"
            Calendar.THURSDAY -> "Donnerstag"
            Calendar.FRIDAY -> "Freitag"
            Calendar.SATURDAY -> "Samstag"
            Calendar.SUNDAY -> "Sonntag"
            else -> ""
        }
        holder.dayNameView.text = dayName

        // Highlight für heute
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        if (statistic.timestamp == today.timeInMillis) {
            holder.dateView.text = "Heute - ${statistic.date}"
        } else {
            holder.dateView.text = statistic.date
        }
    }

    override fun getItemCount() = statistics.size

    fun setStatistics(statistics: List<DayStatistic>) {
        this.statistics = statistics
        notifyDataSetChanged()
    }
}