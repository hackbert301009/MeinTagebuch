package com.example.meintagebuch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class UeberMichActivity : AppCompatActivity() {

    private val PREFS_NAME = "about_me_prefs"
    private val KEY_ABOUT_ME = "about_me_text"

    private lateinit var aboutMeEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ueber_mich)

        // Toolbar Setup
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Über mich"

        toolbar.setNavigationOnClickListener {
            finish()
        }

        aboutMeEditText = findViewById(R.id.aboutMeEditText)
        val saveButton: Button = findViewById(R.id.saveAboutMeButton)
        val websiteCard: MaterialCardView = findViewById(R.id.websiteCard)

        // Gespeicherten Text laden
        loadAboutMeText()

        // Speichern Button
        saveButton.setOnClickListener {
            saveAboutMeText()
        }

        // Website Card
        websiteCard.setOnClickListener {
            openWebsite("https://hackbert.org")
        }
    }

    private fun loadAboutMeText() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedText = prefs.getString(KEY_ABOUT_ME, "")
        aboutMeEditText.setText(savedText)
    }

    private fun saveAboutMeText() {
        val text = aboutMeEditText.text.toString()
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ABOUT_ME, text).apply()

        Toast.makeText(this, "✅ Gespeichert!", Toast.LENGTH_SHORT).show()
    }

    private fun openWebsite(url: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}