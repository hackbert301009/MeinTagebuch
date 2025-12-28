package com.example.meintagebuch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class DiaryAdapter : RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder>() {

    private var entries = listOf<DiaryEntry>()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    fun setEntries(newEntries: List<DiaryEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diary_entry, parent, false)
        return DiaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        val entry = entries[position]
        holder.bind(entry)
    }

    override fun getItemCount() = entries.size

    inner class DiaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.entryText)
        private val dateView: TextView = itemView.findViewById(R.id.entryDate)
        private val authorView: TextView = itemView.findViewById(R.id.entryAuthor)

        fun bind(entry: DiaryEntry) {
            textView.text = entry.text
            dateView.text = dateFormat.format(Date(entry.timestamp))

            // Autor anzeigen
            authorView.text = when (entry.authorId) {
                "ME" -> "📝 ${itemView.context.getString(R.string.diary_author_me)}"
                else -> "💑 ${entry.authorId}"
            }
            authorView.visibility = View.VISIBLE
        }
    }
}