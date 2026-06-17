package com.stillfresh.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.stillfresh.auth.AuthRepository
import com.stillfresh.config.SupabaseConfig
import com.stillfresh.theme.StillFreshTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

class AccountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val user = SupabaseConfig.client.auth.currentUserOrNull()
        val username = user?.userMetadata?.get("username")?.jsonPrimitive?.content ?: "User"
        val email = user?.email ?: "No email"

        setContent {
            StillFreshTheme {
                AccountScreen(
                    username = username,
                    email = email,
                    onBack = { finish() },
                    onLogout = {
                        lifecycleScope.launch {
                            AuthRepository.logout()
                            val intent = Intent(this@AccountActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    },
                    onChangePassword = { currentPassword, newPassword, onSuccess, onError ->
                        lifecycleScope.launch {
                            try {
                                if (currentPassword == newPassword) {
                                    onError("New password must be different from your current password")
                                    return@launch
                                }

                                val verifyResult = AuthRepository.login(email, currentPassword)
                                if (verifyResult.isFailure) {
                                    onError("Current password is incorrect")
                                    return@launch
                                }

                                val userRow = SupabaseConfig.client.postgrest["custom_users"]
                                    .select { filter { eq("email", email) } }
                                    .decodeSingleOrNull<Map<String, String>>()

                                val userSalt = userRow?.get("password_salt")
                                    ?: throw Exception("Could not find user data")

                                val hashedNewPassword = com.stillfresh.CryptoHelper.hashPassword(newPassword, userSalt)

                                SupabaseConfig.client.auth.updateUser {
                                    password = hashedNewPassword
                                }
                                onSuccess()
                                Toast.makeText(this@AccountActivity, "Password updated!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                val message = when {
                                    e.message?.contains("same_password", ignoreCase = true) == true ->
                                        "New password must be different from your current password"
                                    else -> e.message ?: "Failed to update password"
                                }
                                onError(message)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AccountScreen(
    username: String,
    email: String,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onChangePassword: (currentPassword: String, newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit
) {
    val teal = Color(0xFF70B9BE)
    val darkText = Color(0xFF2D3436)

    var showPasswordDialog by remember { mutableStateOf(false) }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var isLocked by remember { mutableStateOf(false) }
    var lockSecondsRemaining by remember { mutableIntStateOf(0) }

    LaunchedEffect(isLocked) {
        if (isLocked) {
            lockSecondsRemaining = 300
            while (lockSecondsRemaining > 0) {
                delay(1000L)
                lockSecondsRemaining--
            }
            isLocked = false
            failedAttempts = 0
        }
    }

    val avatarUrl = "https://api.dicebear.com/8.x/bottts-neutral/png?seed=$username&size=200"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = darkText)
            }
            Text(text = "Account", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = darkText)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(teal.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = username, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = darkText)

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    AccountSettingsRow(icon = Icons.Filled.Person, label = "Username", value = username, teal = teal)
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                    AccountSettingsRow(icon = Icons.Filled.Email, label = "Email", value = email, teal = teal)
                    HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(horizontal = 16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { showPasswordDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = teal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Change password", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = darkText)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFEBEB),
                    contentColor = Color(0xFFE53935)
                )
            ) {
                Icon(Icons.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log out", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            isLocked = isLocked,
            lockSecondsRemaining = lockSecondsRemaining,
            onConfirm = { current, new, onSuccess, onError ->
                onChangePassword(current, new, {
                    onSuccess()
                    failedAttempts = 0
                    showPasswordDialog = false
                }, { msg ->
                    failedAttempts++
                    if (failedAttempts >= 5) {
                        isLocked = true
                    }
                    val displayMsg = if (isLocked) {
                        "Too many failed attempts. Try again in 5 minutes."
                    } else {
                        "$msg (${5 - failedAttempts} attempts remaining)"
                    }
                    onError(displayMsg)
                })
            },
            onDismiss = { showPasswordDialog = false }
        )
    }
}

@Composable
fun ChangePasswordDialog(
    isLocked: Boolean,
    lockSecondsRemaining: Int,
    onConfirm: (currentPassword: String, newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val teal = Color(0xFF70B9BE)
    val isValid = currentPassword.isNotEmpty() && newPassword.length >= 6 && newPassword == confirmPassword && !isLoading && !isLocked

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text(text = "Change Password", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D3436))

            Spacer(modifier = Modifier.height(20.dp))

            if (errorMessage != null || isLocked) {
                val displayMessage = if (isLocked) {
                    val mins = lockSecondsRemaining / 60
                    val secs = lockSecondsRemaining % 60
                    "Too many failed attempts. Try again in ${mins}:${secs.toString().padStart(2, '0')}"
                } else {
                    errorMessage ?: ""
                }
                Text(
                    text = displayMessage,
                    color = Color(0xFFE53935),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEBEB), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it; errorMessage = null },
                label = { Text("Current password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = teal, focusedLabelColor = teal, cursorColor = teal)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it; errorMessage = null },
                label = { Text("New password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = teal, focusedLabelColor = teal, cursorColor = teal)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                label = { Text("Confirm new password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = teal, focusedLabelColor = teal, cursorColor = teal)
            )

            if (newPassword.isNotEmpty() && newPassword.length < 6) {
                Text("Password must be at least 6 characters", color = Color(0xFFE53935), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                Text("Passwords don't match", color = Color(0xFFE53935), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Cancel", color = Color.Gray)
                }
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onConfirm(currentPassword, newPassword,
                            { isLoading = false },
                            { msg -> isLoading = false; errorMessage = msg }
                        )
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = teal)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Save", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSettingsRow(icon: ImageVector, label: String, value: String, teal: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = teal, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 13.sp, color = Color.Gray)
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF2D3436))
        }
    }
}
