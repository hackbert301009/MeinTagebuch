package com.example.meintagebuch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class DiaryAdapter : RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder>() {

    private var entries = emptyList<DiaryEntry>()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)

    class DiaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.entryText)
        val dateView: TextView = itemView.findViewById(R.id.entryDate)
        val authorView: TextView = itemView.findViewById(R.id.entryAuthor)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diary_entry, parent, false)
        return DiaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        val entry = entries[position]
        holder.textView.text = entry.text
        holder.dateView.text = dateFormat.format(Date(entry.timestamp))
        if (entry.authorId == "ME") {
            holder.authorView.text = "Du 💖"
            holder.authorView.setTextColor(
                holder.itemView.context.getColor(R.color.primary)
            )
        } else {
            holder.authorView.text = "Partner 💑"
            holder.authorView.setTextColor(
                holder.itemView.context.getColor(R.color.accent)
            )
        }

    }

    override fun getItemCount() = entries.size

    fun setEntries(entries: List<DiaryEntry>) {
        this.entries = entries
        notifyDataSetChanged()
    }
}