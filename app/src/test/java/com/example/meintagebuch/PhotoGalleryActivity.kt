package com.example.meintagebuch

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PhotoGalleryActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var adapter: PhotoAdapter
    private lateinit var emptyText: TextView
    private var currentPhotoPath: String = ""

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImageUri(it) }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pickImageLauncher.launch("image/*")
        } else {
            Toast.makeText(this, "Berechtigung benötigt", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_gallery)

        database = AppDatabase.getDatabase(this)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "📸 Foto-Galerie"

        toolbar.setNavigationOnClickListener {
            finish()
        }

        val recyclerView: RecyclerView = findViewById(R.id.photoRecyclerView)
        val addButton: FloatingActionButton = findViewById(R.id.addPhotoButton)
        emptyText = findViewById(R.id.emptyText)

        adapter = PhotoAdapter { photo ->
            showPhotoDialog(photo)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Beobachte Fotos
        database.photoDao().getAllPhotos().observe(this) { photos ->
            if (photos.isEmpty()) {
                emptyText.visibility = TextView.VISIBLE
            } else {
                emptyText.visibility = TextView.GONE
            }
            adapter.setPhotos(photos)
        }

        addButton.setOnClickListener {
            checkPermissionAndPickImage()
        }
    }

    private fun checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                pickImageLauncher.launch("image/*")
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                pickImageLauncher.launch("image/*")
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun handleImageUri(uri: Uri) {
        try {
            // Kopiere Bild in App-Ordner
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMAN).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imageFile = File(storageDir, "IMG_$timeStamp.jpg")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(imageFile).use { output ->
                    input.copyTo(output)
                }
            }

            currentPhotoPath = imageFile.absolutePath
            showDescriptionDialog()

        } catch (e: Exception) {
            Toast.makeText(this, "Fehler beim Laden des Bildes", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDescriptionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_entry, null)
        val editText = dialogView.findViewById<EditText>(R.id.entryEditText)
        editText.hint = "Beschreibung (optional)"

        AlertDialog.Builder(this)
            .setTitle("📸 Beschreibung hinzufügen")
            .setView(dialogView)
            .setPositiveButton("Speichern") { _, _ ->
                val description = editText.text.toString()
                lifecycleScope.launch {
                    database.photoDao().insert(
                        PhotoEntry(
                            filePath = currentPhotoPath,
                            description = description
                        )
                    )
                }
            }
            .setNegativeButton("Abbrechen") { _, _ ->
                // Lösche Bild wenn abgebrochen
                File(currentPhotoPath).delete()
            }
            .show()
    }

    private fun showPhotoDialog(photo: PhotoEntry) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_photo_view, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.dialogPhotoImage)
        val descriptionView = dialogView.findViewById<TextView>(R.id.dialogPhotoDescription)

        val file = File(photo.filePath)
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            imageView.setImageBitmap(bitmap)
        }

        descriptionView.text = if (photo.description.isNotEmpty()) {
            photo.description
        } else {
            "Keine Beschreibung"
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Schließen", null)
            .setNegativeButton("Löschen") { _, _ ->
                lifecycleScope.launch {
                    database.photoDao().delete(photo)
                    File(photo.filePath).delete()
                }
            }
            .show()
    }
}
