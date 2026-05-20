package com.stillfresh.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.stillfresh.components.HomeBottomBar
import com.stillfresh.components.HomeHeader
import com.stillfresh.config.SupabaseConfig
import com.stillfresh.theme.StillFreshTheme
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.json.jsonPrimitive

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val user = SupabaseConfig.client.auth.currentUserOrNull()
        val username = user?.userMetadata?.get("username")?.jsonPrimitive?.content ?: "User"

        setContent {
            StillFreshTheme {
                HomeScreen(
                    username = username,
                    onAddClick = {
                        startActivity(Intent(this@HomeActivity, ScanReceiptActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    username: String,
    onAddClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            HomeHeader(
                username = username,
                onProfileClick = { /* TODO: open profile */ }
            )
        },
        bottomBar = {
            HomeBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onAddClick = onAddClick
            )
        }
    ) { paddingValues ->
        // Empty content area for now
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Content coming soon",
                color = Color.Gray,
                fontSize = 16.sp
            )
        }
    }
}
