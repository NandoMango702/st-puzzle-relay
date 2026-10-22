package io.github.nandomango702.stpuzzlerelay.playlist

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * A named, ordered list of puzzles. Each entry is a [BackendName] source name
 * (e.g. "loopy"), so playlists survive renames of display names.
 */
data class Playlist(val name: String, val games: List<String>)

/**
 * Loads and saves playlists as a JSON array in the default SharedPreferences,
 * so they persist across app restarts. Corrupt data is discarded, never crashes.
 */
object PlaylistStore {
    private const val KEY = "playlists_v1"

    fun load(context: Context): MutableList<Playlist> {
        val json = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(json)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val games = mutableListOf<String>()
                val ga = o.optJSONArray("games") ?: JSONArray()
                for (j in 0 until ga.length()) games.add(ga.getString(j))
                Playlist(o.getString("name"), games)
            }
        } catch (_: JSONException) {
            mutableListOf()
        }
    }

    fun save(context: Context, playlists: List<Playlist>) {
        val arr = JSONArray()
        for (p in playlists) {
            arr.put(JSONObject().put("name", p.name).put("games", JSONArray(p.games)))
        }
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit { putString(KEY, arr.toString()) }
    }
}
