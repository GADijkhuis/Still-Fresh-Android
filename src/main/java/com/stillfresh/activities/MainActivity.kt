package com.stillfresh.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.stillfresh.R
import com.stillfresh.config.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Check the status and show the status value in a Toast
        lifecycleScope.launch {
            val status = getSupabaseAuthStatus()
            Toast.makeText(this@MainActivity, status.toString(), Toast.LENGTH_LONG).show()
        }
    }

    suspend fun getSupabaseAuthStatus(): SessionStatus {
        val supaClient = SupabaseConfig.client

        //Wait for when supabase is ready initializing
        return supaClient.auth.sessionStatus.filter { it !is SessionStatus.Initializing }.first()
    }
}