package app.unhook.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** A rate the user entered: "10 times a day", "$5 a week", "30 min a month". */
enum class Period(val days: Double) { DAY(1.0), WEEK(7.0), MONTH(30.4375) }

enum class EntryKind { NOTE, CRAVING }

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Key of a [app.unhook.domain.Preset], or "custom". */
    val kind: String,
    /** Day one: when the user decided to quit. Never moves on relapse. */
    val quitAt: Long,
    /** Start of the current clean streak; moves forward on every relapse. */
    val streakStart: Long,
    val times: Double = 0.0,
    val timesPeriod: Period = Period.DAY,
    val money: Double = 0.0,
    val moneyPeriod: Period = Period.DAY,
    val minutes: Double = 0.0,
    val minutesPeriod: Period = Period.DAY,
    val createdAt: Long = System.currentTimeMillis(),
    /** Key from [app.unhook.ui.HabitIcons]; empty means the preset's own icon. */
    @ColumnInfo(defaultValue = "")
    val icon: String = "",
)

/** A reset of the timer. [streakStart] is where the broken streak had begun. */
@Entity(
    tableName = "relapses",
    foreignKeys = [ForeignKey(entity = Habit::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("habitId")],
)
data class Relapse(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val at: Long,
    val streakStart: Long,
    val note: String = "",
)

@Entity(
    tableName = "reasons",
    foreignKeys = [ForeignKey(entity = Habit::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("habitId")],
)
data class Reason(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val text: String,
)

/** Something to buy with the money not spent on the habit. */
@Entity(
    tableName = "rewards",
    foreignKeys = [ForeignKey(entity = Habit::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("habitId")],
)
data class Reward(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val title: String,
    val price: Double,
    val boughtAt: Long? = null,
)

/** Diary: free notes and cravings the user got through without relapsing. */
@Entity(
    tableName = "entries",
    foreignKeys = [ForeignKey(entity = Habit::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("habitId")],
)
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val at: Long,
    val kind: EntryKind,
    val text: String = "",
    /** 1 (awful) .. 5 (great), or null when not given. */
    val mood: Int? = null,
)

@Dao
abstract class HabitsDao {
    @Query("SELECT * FROM habits ORDER BY createdAt")
    abstract fun observeAll(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY createdAt")
    abstract suspend fun all(): List<Habit>

    @Query("SELECT * FROM habits WHERE id = :id")
    abstract fun observe(id: Long): Flow<Habit?>

    @Query("SELECT * FROM habits WHERE id = :id")
    abstract suspend fun get(id: Long): Habit?

    @Insert
    abstract suspend fun insert(habit: Habit): Long

    @Update
    abstract suspend fun update(habit: Habit)

    @Delete
    abstract suspend fun delete(habit: Habit)

    @Query("SELECT * FROM relapses WHERE habitId = :habitId ORDER BY at")
    abstract fun observeRelapses(habitId: Long): Flow<List<Relapse>>

    @Query("SELECT * FROM relapses ORDER BY at")
    abstract suspend fun allRelapses(): List<Relapse>

    @Insert
    protected abstract suspend fun insertRelapse(relapse: Relapse): Long

    @Query("UPDATE habits SET streakStart = :at WHERE id = :habitId")
    protected abstract suspend fun moveStreak(habitId: Long, at: Long)

    /** Resets the timer to [at] (which may lie in the past, e.g. "it happened last night"). */
    @Transaction
    open suspend fun relapse(habit: Habit, at: Long, note: String) {
        val start = minOf(habit.streakStart, at)
        insertRelapse(Relapse(habitId = habit.id, at = at, streakStart = start, note = note))
        moveStreak(habit.id, at)
    }

    @Delete
    protected abstract suspend fun deleteRelapse(relapse: Relapse)

    /** Undo for a mistaken tap: drops the latest reset and gives the broken streak back. */
    @Transaction
    open suspend fun undoRelapse(relapse: Relapse) {
        deleteRelapse(relapse)
        moveStreak(relapse.habitId, relapse.streakStart)
    }

    @Query("SELECT * FROM reasons WHERE habitId = :habitId ORDER BY id")
    abstract fun observeReasons(habitId: Long): Flow<List<Reason>>

    @Query("SELECT * FROM reasons ORDER BY id")
    abstract suspend fun allReasons(): List<Reason>

    @Insert
    abstract suspend fun insertReason(reason: Reason): Long

    @Update
    abstract suspend fun updateReason(reason: Reason)

    @Delete
    abstract suspend fun deleteReason(reason: Reason)

    @Query("SELECT * FROM rewards WHERE habitId = :habitId ORDER BY boughtAt IS NOT NULL, price")
    abstract fun observeRewards(habitId: Long): Flow<List<Reward>>

    @Query("SELECT * FROM rewards ORDER BY id")
    abstract suspend fun allRewards(): List<Reward>

    @Insert
    abstract suspend fun insertReward(reward: Reward): Long

    @Update
    abstract suspend fun updateReward(reward: Reward)

    @Delete
    abstract suspend fun deleteReward(reward: Reward)

    @Query("SELECT * FROM entries WHERE habitId = :habitId ORDER BY at DESC")
    abstract fun observeEntries(habitId: Long): Flow<List<Entry>>

    @Query("SELECT * FROM entries ORDER BY id")
    abstract suspend fun allEntries(): List<Entry>

    @Insert
    abstract suspend fun insertEntry(entry: Entry): Long

    @Delete
    abstract suspend fun deleteEntry(entry: Entry)

    @Query("DELETE FROM habits")
    protected abstract suspend fun clearHabits()

    @Insert
    protected abstract suspend fun insertHabits(items: List<Habit>)

    @Insert
    abstract suspend fun insertRelapses(items: List<Relapse>)

    @Insert
    protected abstract suspend fun insertReasons(items: List<Reason>)

    @Insert
    protected abstract suspend fun insertRewards(items: List<Reward>)

    @Insert
    protected abstract suspend fun insertEntries(items: List<Entry>)

    /** Replaces everything with a backup (children cascade away with their habits). */
    @Transaction
    open suspend fun replaceAll(snapshot: Snapshot) {
        clearHabits()
        insertHabits(snapshot.habits)
        insertRelapses(snapshot.relapses)
        insertReasons(snapshot.reasons)
        insertRewards(snapshot.rewards)
        insertEntries(snapshot.entries)
    }

    suspend fun snapshot() = Snapshot(all(), allRelapses(), allReasons(), allRewards(), allEntries())
}

data class Snapshot(
    val habits: List<Habit>,
    val relapses: List<Relapse>,
    val reasons: List<Reason>,
    val rewards: List<Reward>,
    val entries: List<Entry>,
)

@Database(entities = [Habit::class, Relapse::class, Reason::class, Reward::class, Entry::class], version = 2)
abstract class UnhookDatabase : RoomDatabase() {
    abstract fun habits(): HabitsDao

    companion object {
        fun create(context: Context): UnhookDatabase =
            Room.databaseBuilder(context.applicationContext, UnhookDatabase::class.java, "unhook.db")
                .addMigrations(ADD_ICON)
                .build()

        private val ADD_ICON = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN icon TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
