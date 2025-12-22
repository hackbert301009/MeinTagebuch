package com.example.meintagebuch

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PhotoAdapter(
    private val onPhotoClick: (PhotoEntry) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    private var photos = emptyList<PhotoEntry>()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.photoImage)
        val descriptionView: TextView = itemView.findViewById(R.id.photoDescription)
        val dateView: TextView = itemView.findViewById(R.id.photoDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos[position]

        // Lade Bild
        val file = File(photo.filePath)
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            holder.imageView.setImageBitmap(bitmap)
        } else {
            holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.descriptionView.text = if (photo.description.isNotEmpty()) {
            photo.description
        } else {
            "Erinnerung"
        }

        holder.dateView.text = dateFormat.format(Date(photo.timestamp))

        holder.itemView.setOnClickListener {
            onPhotoClick(photo)
        }
    }

    override fun getItemCount() = photos.size

    fun setPhotos(photos: List<PhotoEntry>) {
        this.photos = photos
        notifyDataSetChanged()
    }
}