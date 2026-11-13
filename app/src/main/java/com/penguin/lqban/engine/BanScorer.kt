package com.penguin.lqban.engine

import com.penguin.lqban.data.Champion
import com.penguin.lqban.data.CounterEntry

object Weights {
    const val META = 1.0
    const val COUNTER = 120.0
    const val INTENT = 150.0
}

data class Reason(val tag: String, val detail: String)

data class Suggestion(
    val id: String,
    val name: String,
    val score: Double,
    val reasons: List<Reason>,
)

data class SuggestRequest(
    val banned: Set<String> = emptySet(),
    val allyPicks: Set<String> = emptySet(),
    val allyIntent: Set<String> = emptySet(),
    val side: String = "blue",
)

object BanScorer {
    private const val MAX_RESULTS = 3

    fun suggest(
        req: SuggestRequest,
        champions: List<Champion>,
        counterMatrix: Map<String, CounterEntry>,
    ): List<Suggestion> {
        val out = mutableListOf<Suggestion>()

        for (champ in champions) {
            if (champ.id in req.banned) continue
            if (champ.id in req.allyPicks) continue

            var score = Weights.META * champ.metaScore
            val reasons = mutableListOf<Reason>()

            for (allyId in req.allyPicks) {
                val cm = counterMatrix[allyId] ?: continue
                if (champ.id in cm.hardCounters) {
                    score += Weights.COUNTER
                    reasons.add(Reason("KHẮC CHẾ", "Khắc $allyId"))
                    break
                }
            }

            if (req.side == "blue" && champ.id in req.allyIntent) {
                score -= Weights.INTENT
            }

            if (champ.metaScore >= 88) {
                reasons.add(Reason("META", "Tier S+ (${champ.metaScore})"))
            }

            if (req.side == "red" && champ.metaScore >= 90) {
                reasons.add(Reason("ĐỘI ĐỎ", "Địch dễ cướp"))
            }

            if (score > 0 && reasons.isNotEmpty()) {
                out.add(Suggestion(champ.id, champ.name, score, reasons))
            }
        }

        out.sortByDescending { it.score }
        return out.take(MAX_RESULTS)
    }
}
