package io.github.nandomango702.stpuzzlerelay.daily

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import io.github.nandomango702.stpuzzlerelay.backend.BackendName
import java.util.Calendar

/**
 * One deterministic puzzle per calendar day, the same for every player.
 *
 * The puzzle type rotates through the currently enabled puzzles in a stable
 * order ([BackendName.all] is generated from CMakeLists.txt, so re-enabling
 * puzzles later automatically extends the rotation — the count is never
 * hardcoded). The seed is the date string, so generation is deterministic
 * for a given day. Completions are recorded per date in SharedPreferences.
 */
object DailyPuzzle {
    private const val KEY_PREFIX = "daily_complete_"

    /** Today's date as "2026-10-21", in the device's local timezone. */
    fun todayString(): String {
        val c = Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH)
        )
    }

    /** Enabled puzzles in a stable, deterministic order. */
    fun rotation(): List<BackendName> = BackendName.all.sortedBy { it.sourceName }

    /**
     * Which puzzle type is the daily for [dateStr] ("2026-10-21").
     * Rotates by day-of-year, so the same date always maps to the same type.
     */
    fun backendForDate(dateStr: String = todayString()): BackendName {
        val backends = rotation()
        require(backends.isNotEmpty()) { "No puzzles enabled" }
        val (y, m, d) = dateStr.split("-").map { it.toInt() }
        val cal = Calendar.getInstance().apply {
            set(y, m - 1, d, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        return backends[(dayOfYear - 1) % backends.size]
    }

    /**
     * Records the first completion of [dateStr]; later completions of the
     * same day are kept as-is (first completion wins). Returns the recorded
     * solve time in milliseconds.
     */
    fun recordCompletion(context: Context, dateStr: String, solveMillis: Long): Long {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val existing = prefs.getLong(KEY_PREFIX + dateStr, -1L)
        if (existing < 0) {
            prefs.edit { putLong(KEY_PREFIX + dateStr, solveMillis) }
            return solveMillis
        }
        return existing
    }

    /** Solve time in milliseconds if [dateStr] was completed, else null. */
    fun completionTime(context: Context, dateStr: String): Long? {
        val t = PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(KEY_PREFIX + dateStr, -1L)
        return if (t < 0) null else t
    }

    /** Formats milliseconds as mm:ss or h:mm:ss, matching the solve timer. */
    fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%02d:%02d".format(minutes, seconds)
    }
}
