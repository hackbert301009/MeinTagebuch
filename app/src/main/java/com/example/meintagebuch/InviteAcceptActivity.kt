package com.example.meintagebuch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.meintagebuch.InviteAcceptActivity
class InviteAcceptActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inviteId = intent?.data?.getQueryParameter("code")

        if (inviteId != null) {
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(this@InviteAcceptActivity)
                db.partnerInviteDao().insert(
                    PartnerInvite(inviteId, partnerName = "Unknown", accepted = true)
                )
            }
        }

        finish()
    }
}
