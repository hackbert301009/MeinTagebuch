package my.love.meintagebuch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.meintagebuch.R
import com.google.android.material.card.MaterialCardView

class HilfeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hilfe)

        // Toolbar Setup
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Hilfe & Unterstützung"

        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Nummer gegen Kummer Card
        val btnNummerGegenKummer: MaterialCardView = findViewById(R.id.btnNummerGegenKummer)
        btnNummerGegenKummer.setOnClickListener {
            openPhone("116111")
        }

        // TelefonSeelsorge Card
        val btnTelefonSeelsorge: MaterialCardView = findViewById(R.id.btnTelefonSeelsorge)
        btnTelefonSeelsorge.setOnClickListener {
            openPhone("08001110111")
        }

        // Bug Report Card
        val btnBugReport: MaterialCardView = findViewById(R.id.btnBugReport)
        btnBugReport.setOnClickListener {
            openWebsite("https://hackbert.org/kontakt")
        }
    }
    private fun openPhone(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        startActivity(intent)
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