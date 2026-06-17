package com.stillfresh.handlers

import android.content.Context
import com.stillfresh.dataclasses.FoodTip
import kotlinx.serialization.json.Json

object FoodTipHandler {
    fun getTips(context: Context): List<FoodTip> {

        val json =
            context.assets.open("food_tips.json")
                .bufferedReader()
                .use { it.readText() }

        return Json.decodeFromString(json)
    }
}