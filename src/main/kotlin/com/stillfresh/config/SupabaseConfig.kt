package com.stillfresh.config

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseConfig {

    private val supabaseUrl: String
        get() = System.getenv("SUPABASE_URL")
            ?: error("SUPABASE_URL environment variable is not set")

    private val supabaseKey: String
        get() = System.getenv("SUPABASE_KEY")
            ?: error("SUPABASE_KEY environment variable is not set")

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}
