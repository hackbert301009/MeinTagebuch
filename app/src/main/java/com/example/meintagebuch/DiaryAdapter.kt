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

            // Meinen Namen holen (getrimmt!)
            val myName = PartnerNameHelper.getMyName(itemView.context).trim()

            // Entry author auch trimmen für Vergleich
            val entryAuthor = entry.authorId.trim()

            // Debug-Logging
            android.util.Log.d("DiaryAdapter", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            android.util.Log.d("DiaryAdapter", "Entry: ${entry.text.take(30)}...")
            android.util.Log.d("DiaryAdapter", "   Entry author: '$entryAuthor'")
            android.util.Log.d("DiaryAdapter", "   My name: '$myName'")
            android.util.Log.d("DiaryAdapter", "   Are equal (case-insensitive): ${entryAuthor.equals(myName, ignoreCase = true)}")

            // Autor-Name anzeigen
            if (entryAuthor.equals(myName, ignoreCase = true)) {
                // Mein eigener Eintrag
                authorView.text = "📝 ${itemView.context.getString(R.string.diary_author_me)}"
                android.util.Log.d("DiaryAdapter", "   → Showing as MY entry")
            } else {
                // Eintrag vom Partner
                authorView.text = "💑 $entryAuthor"
                android.util.Log.d("DiaryAdapter", "   → Showing as PARTNER entry")
            }
            android.util.Log.d("DiaryAdapter", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            authorView.visibility = View.VISIBLE
        }
    }
}