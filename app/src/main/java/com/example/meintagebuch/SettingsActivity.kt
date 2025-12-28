package com.example.meintagebuch

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
        // Zurück zur MainActivity und App neu laden
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