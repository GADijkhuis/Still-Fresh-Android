package com.stillfresh.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.stillfresh.auth.AuthRepository
import com.stillfresh.components.AppLogo
import com.stillfresh.components.StillFreshButton
import com.stillfresh.components.StillFreshOutlinedButton
import com.stillfresh.components.StillFreshTextField
import com.stillfresh.config.SupabaseConfig
import com.stillfresh.handlers.NotificationHandler
import com.stillfresh.theme.StillFreshTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notifications disabled. You won't receive expiry alerts.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        NotificationHandler.createNotificationChannel(this)
        checkNotificationPermission()

        // Check for existing session before showing login screen
        lifecycleScope.launch {
            val status = SupabaseConfig.client.auth.sessionStatus
                .filter { it !is SessionStatus.Initializing }
                .first()

            if (status is SessionStatus.Authenticated) {
                // Already logged in — go straight to home
                startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                finish()
                return@launch
            }

            // No session — show login screen
            setContent {
                StillFreshTheme {
                    var isLoading by remember { mutableStateOf(false) }

                    LoginScreen(
                        isLoading = isLoading,
                        onLogin = { email, password ->
                            isLoading = true
                            lifecycleScope.launch {
                                val result = AuthRepository.login(email, password)
                                isLoading = false
                                result.fold(
                                    onSuccess = {
                                        startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                                        finish()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(this@MainActivity, error.message ?: "Login failed", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        onSignUp = {
                            startActivity(Intent(this@MainActivity, SignUpActivity::class.java))
                        }
                    )
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                else -> {
                    // Directly ask for the permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    isLoading: Boolean = false,
    onLogin: (email: String, password: String) -> Unit,
    onSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val tealBackground = Color(0xFF70B9BE)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tealBackground)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo
            AppLogo(size = 120.dp)

            Spacer(modifier = Modifier.height(16.dp))

            // App name
            Text(
                text = "StillFresh?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Your groceries called-\nthey want to stay fresh.",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Email field
            StillFreshTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password field
            StillFreshTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color.White
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Login button
            StillFreshButton(
                text = if (isLoading) "Logging in..." else "Log in",
                onClick = { onLogin(email, password) },
                enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
                containerColor = Color.White,
                contentColor = tealBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Divider with "or"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = "  or  ",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign up button
            StillFreshOutlinedButton(
                text = "Create New Account",
                onClick = onSignUp,
                borderColor = Color.White,
                contentColor = Color.White
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
