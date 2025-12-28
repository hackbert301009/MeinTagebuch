package com.example.meintagebuch

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Toolbar einrichten
        val toolbar: Toolbar = findViewById(R.id.settingsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)

        val languageGroup: RadioGroup = findViewById(R.id.languageRadioGroup)
        val germanRadio: RadioButton = findViewById(R.id.radioGerman)
        val englishRadio: RadioButton = findViewById(R.id.radioEnglish)
        val ratingCard: MaterialCardView = findViewById(R.id.ratingCard)

        // Aktuelle Sprache laden
        val currentLang = getLanguagePreference()
        when (currentLang) {
            "de" -> germanRadio.isChecked = true
            "en" -> englishRadio.isChecked = true
        }

        // Sprache ändern
        languageGroup.setOnCheckedChangeListener { _, checkedId ->
            val newLang = when (checkedId) {
                R.id.radioGerman -> "de"
                R.id.radioEnglish -> "en"
                else -> "de"
            }

            if (newLang != currentLang) {
                saveLanguagePreference(newLang)
                setAppLocale(newLang)
                recreateApp()
            }
        }

        // Bewertung
        ratingCard.setOnClickListener {
            showRatingDialog()
        }
    }

    private fun showRatingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rating, null)
        val ratingBar: RatingBar = dialogView.findViewById(R.id.ratingBar)
        val ratingText: TextView = dialogView.findViewById(R.id.ratingText)

        // Aktuelle Bewertung laden
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentRating = prefs.getFloat("user_rating", 0f)
        ratingBar.rating = currentRating

        // Rating Text aktualisieren
        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            ratingText.text = when {
                rating == 0f -> getString(R.string.rating_none)
                rating <= 2f -> getString(R.string.rating_bad)
                rating <= 3f -> getString(R.string.rating_okay)
                rating <= 4f -> getString(R.string.rating_good)
                else -> getString(R.string.rating_excellent)
            }
        }

        // Initial text setzen
        if (currentRating > 0) {
            ratingText.text = when {
                currentRating <= 2f -> getString(R.string.rating_bad)
                currentRating <= 3f -> getString(R.string.rating_okay)
                currentRating <= 4f -> getString(R.string.rating_good)
                else -> getString(R.string.rating_excellent)
            }
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.rating_dialog_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.rating_save)) { _, _ ->
                val rating = ratingBar.rating
                if (rating > 0) {
                    prefs.edit().putFloat("user_rating", rating).apply()
                    Toast.makeText(
                        this,
                        getString(R.string.rating_thanks),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.invite_cancel_button), null)
            .show()
    }

    private fun getLanguagePreference(): String {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("language", "de") ?: "de"
    }

    private fun saveLanguagePreference(lang: String) {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("language", lang).apply()
    }

    private fun setAppLocale(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun recreateApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}