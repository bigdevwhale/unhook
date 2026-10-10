package app.unhook.data

import org.json.JSONArray
import org.json.JSONObject

/** Plain JSON backup: everything stays on the device unless the user exports it. */
object Backup {
    private const val VERSION = 1

    fun encode(s: Snapshot): String = JSONObject()
        .put("app", "unhook")
        .put("version", VERSION)
        .put("habits", JSONArray(s.habits.map {
            JSONObject()
                .put("id", it.id).put("name", it.name).put("kind", it.kind)
                .put("quitAt", it.quitAt).put("streakStart", it.streakStart)
                .put("times", it.times).put("timesPeriod", it.timesPeriod.name)
                .put("money", it.money).put("moneyPeriod", it.moneyPeriod.name)
                .put("minutes", it.minutes).put("minutesPeriod", it.minutesPeriod.name)
                .put("createdAt", it.createdAt)
                .put("icon", it.icon)
        }))
        .put("relapses", JSONArray(s.relapses.map {
            JSONObject().put("id", it.id).put("habitId", it.habitId).put("at", it.at).put("streakStart", it.streakStart).put("note", it.note)
        }))
        .put("reasons", JSONArray(s.reasons.map {
            JSONObject().put("id", it.id).put("habitId", it.habitId).put("text", it.text)
        }))
        .put("rewards", JSONArray(s.rewards.map {
            JSONObject().put("id", it.id).put("habitId", it.habitId).put("title", it.title).put("price", it.price)
                .put("boughtAt", it.boughtAt ?: JSONObject.NULL)
        }))
        .put("entries", JSONArray(s.entries.map {
            JSONObject().put("id", it.id).put("habitId", it.habitId).put("at", it.at).put("kind", it.kind.name)
                .put("text", it.text).put("mood", it.mood ?: JSONObject.NULL)
        }))
        .toString(2)

    /** Throws [IllegalArgumentException] when the text is not an Unhook backup. */
    fun decode(text: String): Snapshot {
        val root = runCatching { JSONObject(text) }.getOrNull()
        require(root != null && root.optString("app") == "unhook") { "Not an Unhook backup" }
        return Snapshot(
            habits = root.list("habits") {
                Habit(
                    id = getLong("id"), name = getString("name"), kind = getString("kind"),
                    quitAt = getLong("quitAt"), streakStart = getLong("streakStart"),
                    times = getDouble("times"), timesPeriod = Period.valueOf(getString("timesPeriod")),
                    money = getDouble("money"), moneyPeriod = Period.valueOf(getString("moneyPeriod")),
                    minutes = getDouble("minutes"), minutesPeriod = Period.valueOf(getString("minutesPeriod")),
                    createdAt = getLong("createdAt"),
                    icon = optString("icon"),
                )
            },
            relapses = root.list("relapses") {
                Relapse(getLong("id"), getLong("habitId"), getLong("at"), getLong("streakStart"), optString("note"))
            },
            reasons = root.list("reasons") { Reason(getLong("id"), getLong("habitId"), getString("text")) },
            rewards = root.list("rewards") {
                Reward(getLong("id"), getLong("habitId"), getString("title"), getDouble("price"), if (isNull("boughtAt")) null else getLong("boughtAt"))
            },
            entries = root.list("entries") {
                Entry(getLong("id"), getLong("habitId"), getLong("at"), EntryKind.valueOf(getString("kind")), optString("text"), if (isNull("mood")) null else getInt("mood"))
            },
        )
    }

    private fun <T> JSONObject.list(key: String, read: JSONObject.() -> T): List<T> {
        val arr = optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).map { arr.getJSONObject(it).read() }
    }
}
