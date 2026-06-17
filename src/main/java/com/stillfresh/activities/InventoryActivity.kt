package com.stillfresh.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stillfresh.components.InventoryView
import com.stillfresh.config.SupabaseConfig
import com.stillfresh.theme.StillFreshTheme
import io.github.jan.supabase.auth.auth

class InventoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val user = SupabaseConfig.client.auth.currentUserOrNull()
        if (user == null) {
            finish()
            return
        }
        val userId = user.id

        setContent {
            StillFreshTheme {
                InventoryView(
                    userId = userId,
                    onBack = { finish() }
                )
            }
        }
    }
}
