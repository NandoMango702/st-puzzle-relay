package io.github.nandomango702.stpuzzlerelay.playlist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import io.github.nandomango702.stpuzzlerelay.GamePlay
import io.github.nandomango702.stpuzzlerelay.R
import io.github.nandomango702.stpuzzlerelay.backend.BackendName.Companion.byLowerCase
import io.github.nandomango702.stpuzzlerelay.databinding.ActivityPlaylistsBinding
import io.github.nandomango702.stpuzzlerelay.databinding.PlaylistRowBinding

/** Lists the user's saved playlists. Play starts the first puzzle of a playlist. */
class PlaylistsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlaylistsBinding
    private val playlists = mutableListOf<Playlist>()
    private lateinit var adapter: PlaylistsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.playlists)
        }
        adapter = PlaylistsAdapter()
        binding.playlistList.adapter = adapter
        binding.playlistList.emptyView = binding.emptyView
        binding.newPlaylist.setOnClickListener {
            startActivity(Intent(this, PlaylistEditorActivity::class.java))
        }
        // Thumb reachability: the button lives at the bottom, above the nav bar.
        // The inset goes on the *margin* (lifting the button), not padding:
        // padding would stretch the button down behind the nav bar.
        val baseBottomMargin =
            (binding.newPlaylist.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(binding.newPlaylist) { v, windowInsets ->
            val inset = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = baseBottomMargin + inset
            }
            windowInsets
        }
    }

    override fun onResume() {
        super.onResume()
        playlists.clear()
        playlists.addAll(PlaylistStore.load(this))
        adapter.notifyDataSetChanged()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun playPlaylist(position: Int, shuffle: Boolean = false) {
        val playlist = playlists.getOrNull(position) ?: return
        val validGames = playlist.games.filter { byLowerCase(it) != null }
        if (validGames.isEmpty()) {
            Toast.makeText(this, R.string.playlist_pick_at_least_one, Toast.LENGTH_SHORT).show()
            return
        }
        // Shuffle only affects this playthrough; the saved playlist order is untouched.
        val playOrder = if (shuffle) validGames.shuffled() else validGames
        val first = byLowerCase(playOrder[0])!!
        startActivity(Intent(this, GamePlay::class.java).apply {
            data = Uri.fromParts(GamePlay.OUR_SCHEME, first.toString(), null)
            putExtra(GamePlay.EXTRA_PLAYLIST_NAME, playlist.name)
            putStringArrayListExtra(GamePlay.EXTRA_PLAYLIST_GAMES, ArrayList(playOrder))
            putExtra(GamePlay.EXTRA_PLAYLIST_INDEX, 0)
        })
    }

    private fun editPlaylist(position: Int) {
        startActivity(Intent(this, PlaylistEditorActivity::class.java).apply {
            putExtra(PlaylistEditorActivity.EXTRA_PLAYLIST_POSITION, position)
        })
    }

    private fun deletePlaylist(position: Int) {
        val playlist = playlists.getOrNull(position) ?: return
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.delete_playlist_confirm, playlist.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                playlists.removeAt(position)
                PlaylistStore.save(this, playlists)
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private inner class PlaylistsAdapter : BaseAdapter() {
        override fun getCount() = playlists.size
        override fun getItem(position: Int): Any = playlists[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = (convertView?.tag as? PlaylistRowBinding)
                ?: PlaylistRowBinding.inflate(
                    LayoutInflater.from(this@PlaylistsActivity), parent, false
                ).also { it.root.tag = it }
            val playlist = playlists[position]
            row.playlistName.text = playlist.name
            row.playlistCount.text = resources.getQuantityString(
                R.plurals.playlist_puzzle_count, playlist.games.size, playlist.games.size
            )
            row.playButton.setOnClickListener { playPlaylist(position) }
            row.shuffleButton.setOnClickListener { playPlaylist(position, shuffle = true) }
            row.editButton.setOnClickListener { editPlaylist(position) }
            row.deleteButton.setOnClickListener { deletePlaylist(position) }
            // Accessibility: the row's buttons already speak; give the row itself a label too.
            row.root.contentDescription =
                "${playlist.name}, ${row.playlistCount.text}"
            return row.root
        }
    }
}
