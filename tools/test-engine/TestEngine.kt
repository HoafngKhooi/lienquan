import com.penguin.lqban.data.ChampionsData
import com.penguin.lqban.engine.BanScorer
import com.penguin.lqban.engine.SuggestRequest
import com.google.gson.Gson
import java.io.File

fun main() {
    val f = File("app/src/main/assets/champions.json")
    if (!f.exists()) { println("ERROR: ${f.absolutePath} không tồn tại"); return }
    val data = Gson().fromJson(f.readText(), ChampionsData::class.java)
    println("Loaded patch=${data.patch}, ${data.champions.size} champions")

    val req = SuggestRequest(
        banned = setOf("stuart"),
        allyPicks = setOf("nakroth"),
        allyIntent = setOf("nakroth"),
        side = "blue",
    )
    val result = BanScorer.suggest(req, data.champions, data.counterMatrix)
    println("Suggestions:")
    for (s in result) {
        println("  ${s.name} (${"%.0f".format(s.score)}) - ${s.reasons.joinToString { it.tag }}")
    }
}
