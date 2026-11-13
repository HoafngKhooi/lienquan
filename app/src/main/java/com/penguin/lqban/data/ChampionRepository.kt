package com.penguin.lqban.data

import android.content.Context
import com.google.gson.Gson

object ChampionRepository {
    @Volatile private var cached: ChampionsData? = null

    fun load(context: Context): ChampionsData {
        cached?.let { return it }
        val json = context.assets.open("champions.json")
            .bufferedReader().use { it.readText() }
        val data = Gson().fromJson(json, ChampionsData::class.java)
        cached = data
        return data
    }
}
