package com.stillfresh.auth

import com.stillfresh.CryptoHelper
import com.stillfresh.config.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.postgrest.postgrest

object AuthRepository {

    private val client = SupabaseConfig.client

    /**
     * Sign up a new user with username, email, and password.
     * The username is passed as metadata and the database trigger
     * will encrypt it and store it in the users table.
     */
    suspend fun signUp(username: String, email: String, password: String): Result<Unit> {
        return try {
            val userSalt = CryptoHelper.generateSalt()
            val customHashedPassword = CryptoHelper.hashPassword(password, userSalt)

            val authResult = client.auth.signUpWith(Email) {
                this.email = email
                this.password = customHashedPassword
                this.data = buildJsonObject {
                    put("username", username)
                }
            }

            val userId = authResult?.id ?: throw Exception("An error occurred during sign up.")

            val userData = mapOf(
                "id" to userId,
                "email" to email,
                "password_salt" to userSalt
            )

            client.postgrest["custom_users"].insert(userData)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Log in with email and password.
     */
    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val userRow = client.postgrest["custom_users"]
                .select { filter { eq("email", email) } }
                .decodeSingleOrNull<Map<String, String>>()

            val userSalt = userRow?.get("password_salt")
                ?: throw Exception("An error occured retrieving user information.")

            val customHashedPassword = CryptoHelper.hashPassword(password, userSalt)

            client.auth.signInWith(Email) {
                this.email = email
                this.password = customHashedPassword
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Log out the current user.
     */
    suspend fun logout(): Result<Unit> {
        return try {
            client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
