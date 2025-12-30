package com.example.meintagebuch

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseException

class MyApplication : Application() {

    private val TAG = "MyApplication"

    override fun onCreate() {
        super.onCreate()

        try {
            // Firebase initialisieren
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "✅ Firebase App initialized")

            // WICHTIG: Database URL explizit setzen
            // Ersetze diese URL mit deiner echten URL aus der Firebase Console!
            val databaseUrl = "https://my-love-9c55d-default-rtdb.europe-west1.firebasedatabase.app"

            val database = FirebaseDatabase.getInstance(databaseUrl)

            // Offline-Persistenz aktivieren (nur einmal!)
            database.setPersistenceEnabled(true)

            Log.d(TAG, "✅ Firebase Database initialized with URL: $databaseUrl")
            Log.d(TAG, "✅ Firebase reference: ${database.reference}")

        } catch (e: DatabaseException) {
            // Persistenz bereits aktiviert - das ist OK
            Log.w(TAG, "Firebase persistence already enabled (this is OK)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing Firebase", e)
        }
    }
}