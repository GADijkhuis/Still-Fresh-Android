package com.stillfresh.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.lifecycle.lifecycleScope
import com.stillfresh.auth.AuthRepository
import com.stillfresh.components.AppLogo
import com.stillfresh.components.StillFreshButton
import com.stillfresh.components.StillFreshTextField
import com.stillfresh.theme.StillFreshTheme
import kotlinx.coroutines.launch

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StillFreshTheme {
                var isLoading by remember { mutableStateOf(false) }

                SignUpScreen(
                    isLoading = isLoading,
                    onSignUp = { username, email, password ->
                        isLoading = true
                        lifecycleScope.launch {
                            val result = AuthRepository.signUp(username, email, password)
                            isLoading = false
                            result.fold(
                                onSuccess = {
                                    Toast.makeText(this@SignUpActivity, "Account created! Check your email to confirm.", Toast.LENGTH_LONG).show()
                                    finish()
                                },
                                onFailure = { error ->
                                    Toast.makeText(this@SignUpActivity, "Sign up failed", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    onBackToLogin = { finish() }
                )
            }
        }
    }
}

@Composable
fun SignUpScreen(
    isLoading: Boolean = false,
    onSignUp: (username: String, email: String, password: String) -> Unit,
    onBackToLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
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
            AppLogo(size = 100.dp)

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "Create Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Join Still Fresh and keep\nyour groceries on track.",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Username field
            StillFreshTextField(
                value = username,
                onValueChange = { username = it },
                label = "Username",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            // Sign up button
            StillFreshButton(
                text = if (isLoading) "Creating account..." else "Sign Up",
                onClick = { onSignUp(username, email, password) },
                enabled = username.isNotBlank() && email.isNotBlank() && password.isNotBlank() && !isLoading,
                containerColor = Color.White,
                contentColor = tealBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Back to login
            TextButton(onClick = onBackToLogin) {
                Text(
                    text = "Already have an account? Log in",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
