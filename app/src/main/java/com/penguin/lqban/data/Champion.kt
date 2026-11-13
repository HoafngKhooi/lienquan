package com.penguin.lqban.data

import com.google.gson.annotations.SerializedName

data class Champion(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("role") val role: String,
    @SerializedName("meta_score") val metaScore: Int,
    @SerializedName("win_rate") val winRate: Double,
    @SerializedName("ban_rate") val banRate: Double,
)

data class CounterEntry(
    @SerializedName("hard_counters") val hardCounters: List<String> = emptyList()
)

data class ChampionsData(
    @SerializedName("patch") val patch: String,
    @SerializedName("champions") val champions: List<Champion>,
    @SerializedName("counter_matrix") val counterMatrix: Map<String, CounterEntry>,
)
