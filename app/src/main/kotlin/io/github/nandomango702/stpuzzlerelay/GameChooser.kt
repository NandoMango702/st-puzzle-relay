package io.github.nandomango702.stpuzzlerelay

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Configuration
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.TextAppearanceSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.edit
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.gridlayout.widget.GridLayout
import androidx.preference.PreferenceManager
import io.github.nandomango702.stpuzzlerelay.Utils.sendFeedbackDialog
import io.github.nandomango702.stpuzzlerelay.backend.BackendName
import io.github.nandomango702.stpuzzlerelay.backend.BackendName.Companion.byLowerCase
import io.github.nandomango702.stpuzzlerelay.backend.GUESS
import io.github.nandomango702.stpuzzlerelay.backend.KEEN
import io.github.nandomango702.stpuzzlerelay.backend.LIGHTUP
import io.github.nandomango702.stpuzzlerelay.backend.NET
import io.github.nandomango702.stpuzzlerelay.backend.SIGNPOST
import io.github.nandomango702.stpuzzlerelay.backend.SOLO
import io.github.nandomango702.stpuzzlerelay.backend.TOWERS
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.CHOOSER_STYLE_KEY
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.SAVED_BACKEND
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.STATE_PREFS_NAME
import io.github.nandomango702.stpuzzlerelay.daily.DailyPuzzle
import io.github.nandomango702.stpuzzlerelay.databinding.ChooserBinding
import io.github.nandomango702.stpuzzlerelay.databinding.ListItemBinding
import io.github.nandomango702.stpuzzlerelay.launch.GameGenerator.Companion.executableIsMissing
import io.github.nandomango702.stpuzzlerelay.playlist.PlaylistsActivity
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

class GameChooser : ActivityWithLoadButton(), OnSharedPreferenceChangeListener {
    private lateinit var binding: ChooserBinding
    private lateinit var prefs: SharedPreferences
    private var useGrid = false
    private val itemBindings: MutableMap<BackendName, ListItemBinding> = LinkedHashMap()
    private var menu: Menu? = null
    private var scrollToOnNextLayout: BackendName? = null
    private var resumeTime: Long = 0
    private var wasNight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (executableIsMissing(this)) {
            finish()
            return
        }
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)
        wasNight = NightModeHelper.isNight(resources.configuration)
        val state = getSharedPreferences(STATE_PREFS_NAME, MODE_PRIVATE)
        val oldCS = state.getString(CHOOSER_STYLE_KEY, null)
        if (oldCS != null) {  // migrate to somewhere more sensible
            prefs.edit { putString(CHOOSER_STYLE_KEY, oldCS) }
            state.edit { remove(CHOOSER_STYLE_KEY) }
        }
        useGrid = prefs.getString(CHOOSER_STYLE_KEY, "list") == "grid"
        binding = ChooserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Actions live in the bottom bar (thumb reachability): it is the
        // support action bar, so the options menu renders there. The top bar
        // keeps only the title.
        binding.toolbar.title = getString(R.string.app_name)
        setSupportActionBar(binding.bottomBar)
        // The bottom bar carries icons only; the title stays up top.
        supportActionBar?.setDisplayShowTitleEnabled(false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomBar) { v, windowInsets ->
            // Edge-to-edge pattern: extend the bar behind the navigation bar
            // (padding) and grow its height by the same amount, so the icons
            // always keep their full action-bar height instead of being
            // crushed by the inset. Just adding padding inside the fixed
            // height squeezes the icons to nothing on 3-button nav.
            // Sides are padded too, so in landscape the icons never slide
            // under a side navigation bar.
            val insets = selfHandledInsets(windowInsets)
            v.updatePadding(left = insets.left, right = insets.right, bottom = insets.bottom)
            v.updateLayoutParams { height = bottomBarHeightPx() + insets.bottom }
            // Single source of truth for clearance: the list clears the bar
            // by the bar's full grown height (this runs after the table's own
            // inset listener, so it wins if the two ever disagree).
            applyTableInsets(windowInsets)
            WindowInsetsCompat.CONSUMED
        }
        buildViews()
        ViewCompat.setOnApplyWindowInsetsListener(binding.table) { v, windowInsets ->
            applyTableInsets(windowInsets)
            rethinkColumns(false)
            WindowInsetsCompat.CONSUMED
        }
        binding.scrollView.viewTreeObserver.addOnGlobalLayoutListener {
            if (scrollToOnNextLayout != null) {
                val v: View = itemBindings[scrollToOnNextLayout]!!.root
                binding.scrollView.requestChildRectangleOnScreen(
                    v,
                    Rect(0, 0, v.width, v.height),
                    true
                )
                scrollToOnNextLayout = null
            }
        }
        enableTableAnimations()
    }

    override fun onResume() {
        super.onResume()
        resumeTime = System.nanoTime()
        var currentBackend: BackendName? = null
        val state = getSharedPreferences(STATE_PREFS_NAME, MODE_PRIVATE)
        if (state.contains(SAVED_BACKEND)) {
            currentBackend = byLowerCase(state.getString(SAVED_BACKEND, null))
        }
        BackendName.all.forEach {
            val isCurrent = it === currentBackend
            itemBindings[it]!!.root.isActivated = isCurrent
            if (isCurrent) {
                // wait until we know the size
                scrollToOnNextLayout = it
            }
        }
        updateDailyTitle()
    }

    private fun enableTableAnimations() {
        binding.table.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }
    }

    private fun buildViews() {
        itemBindings.clear()
        BackendName.all.forEach { backend ->
            with (ListItemBinding.inflate(layoutInflater)) {
                itemBindings[backend] = this
                icon.setImageDrawable(backend.icon(this@GameChooser))
                text.text = SpannableStringBuilder(backend.displayName).apply {
                    setSpan(
                        TextAppearanceSpan(this@GameChooser, R.style.ChooserItemName),
                        0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    append(": ")
                    append(getString(backend.description))
                }
                text.visibility = if (useGrid) GONE else VISIBLE
                with (root) {
                    ignoreTouchAfterResume()
                    setOnClickListener {
                        startActivity(Intent(this@GameChooser, GamePlay::class.java).apply {
                            data = Uri.fromParts(GamePlay.OUR_SCHEME, backend.toString(), null)
                        })
                    }
                    setOnLongClickListener {
                        toggleStarred(backend)
                        true
                    }
                    isFocusable = true
                    layoutParams = mkLayoutParams()
                }
                binding.table.addView(root)
            }
        }
        rethinkColumns(true)
    }

    @SuppressLint("ClickableViewAccessibility") // Does not define a new click mechanism
    private fun View.ignoreTouchAfterResume() {
        setOnTouchListener { _: View?, _: MotionEvent? -> System.nanoTime() - resumeTime < 300000000 }
    }

    private fun mkLayoutParams(): GridLayout.LayoutParams =
        GridLayout.LayoutParams().apply { setGravity(Gravity.CENTER_HORIZONTAL) }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val isNight = NightModeHelper.isNight(resources.configuration)
        if (wasNight != isNight) {
            BackendName.all.forEach {
                itemBindings[it]!!.icon.setImageDrawable(it.icon(this))
            }
        }
        rethinkColumns(wasNight != isNight)
        // https://github.com/chrisboyle/sgtpuzzles/issues/227
        menu?.let {
            it.clear()
            onCreateOptionsMenu(it)
        }
        wasNight = isNight
    }

    private var columns = 0
    private var colWidthPx = 0
    private fun rethinkColumns(force: Boolean) {
        val dm = resources.displayMetrics
        val insets = ViewCompat.getRootWindowInsets(binding.root)?.getInsets(systemBars() or displayCutout() or ime())
        val screenWidthPx = (dm.widthPixels - (insets?.left ?: 0) - (insets?.right ?: 0)).toDouble()
        val colWidthDipNeeded = if (useGrid) 72 else 298
        val screenWidthDip = screenWidthPx / dm.density
        val newColumns = floor(screenWidthDip / colWidthDipNeeded).toInt().coerceAtLeast(1)
        val newColWidthPx = floor(screenWidthPx / newColumns).toInt()
        if (force || columns != newColumns || colWidthPx != newColWidthPx) {
            columns = newColumns
            colWidthPx = newColWidthPx
            val (starred, others) = BackendName.all.partition { isStarred(it) }
            val anyStarred = starred.isNotEmpty()
            binding.gamesStarred.visibility = if (anyStarred) VISIBLE else GONE
            var row = 0
            if (anyStarred) {
                setGridCells(binding.gamesStarred, 0, row++, columns)
                row = setViewsGridCells(row, starred.map {itemBindings[it]!!}, true)
            }
            binding.gamesOthers.setText(if (anyStarred) R.string.games_others else R.string.games_others_none_starred)
            setGridCells(binding.gamesOthers, 0, row++, columns)
            setViewsGridCells(row, others.map {itemBindings[it]!!}, false)
        }
    }

    private fun setGridCells(v: View, x: Int, y: Int, w: Int) {
        v.layoutParams = (v.layoutParams as GridLayout.LayoutParams).apply {
            width = colWidthPx * w
            columnSpec = GridLayout.spec(x, w, GridLayout.START)
            rowSpec = GridLayout.spec(y, 1, GridLayout.START)
            setGravity(if (useGrid && w == 1) Gravity.CENTER_HORIZONTAL else Gravity.START)
        }
    }

    private fun setViewsGridCells(
        startRow: Int,
        itemBindings: List<ListItemBinding>,
        starred: Boolean
    ): Int {
        var col = 0
        var row = startRow
        itemBindings.forEach {
            it.star.visibility = if (starred) VISIBLE else GONE
            if (col >= columns) {
                col = 0
                row++
            }
            setGridCells(it.root, col++, row, 1)
        }
        row++
        return row
    }

    private fun isStarred(game: BackendName): Boolean =
        prefs.getBoolean("starred_$game", DEFAULT_STARRED.contains(game))

    private fun toggleStarred(game: BackendName) {
        prefs.edit { putBoolean("starred_$game", !isStarred(game)) }
        rethinkColumns(true)
    }

    /** Short label for the daily menu item, e.g. "Oct 21". */
    private fun dailyDateLabel(): String =
        java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            .format(java.util.Calendar.getInstance().time)

    /**
     * Opens today's daily puzzle. If it's already solved, shows the completed
     * state (with the solve time) instead of a fresh puzzle.
     */
    private fun openDaily() {
        val date = DailyPuzzle.todayString()
        val doneMillis = DailyPuzzle.completionTime(this, date)
        if (doneMillis != null) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.daily_complete_title, dailyDateLabel()))
                .setMessage(
                    getString(
                        R.string.daily_complete_message,
                        DailyPuzzle.formatTime(doneMillis)
                    )
                )
                .setPositiveButton(R.string.play_again) { _, _ -> launchDaily(date) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            launchDaily(date)
        }
    }

    private fun launchDaily(date: String) {
        val backend = DailyPuzzle.backendForDate(date)
        startActivity(Intent(this, GamePlay::class.java).apply {
            data = Uri.fromParts(GamePlay.OUR_SCHEME, backend.toString(), null)
            putExtra(GamePlay.EXTRA_DAILY_DATE, date)
        })
    }

    /**
     * Launches a uniformly random puzzle from the given pool, exactly as if
     * the user had tapped it: default params, so a fresh random game.
     * The pool comes from the live enabled set (BackendName.all), never a
     * hardcoded count, so the full upstream puzzle set slots in later.
     */
    private fun launchRandom(pool: Collection<BackendName>) {
        val backend = pool.random()
        startActivity(Intent(this, GamePlay::class.java).apply {
            data = Uri.fromParts(GamePlay.OUR_SCHEME, backend.toString(), null)
        })
    }

    private fun launchRandomFavorite() {
        val starred = BackendName.all.filter { isStarred(it) }
        if (starred.isEmpty()) {
            Toast.makeText(this, R.string.random_no_favorites, Toast.LENGTH_LONG).show()
        } else {
            launchRandom(starred)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        this.menu = menu
        menuInflater.inflate(R.menu.chooser, menu)
        // The bottom bar always shows every action as an icon (six icons fit
        // any phone width), and the daily entry carries today's date.
        for (i in listOf(R.id.load, R.id.settings, R.id.playlists, R.id.daily, R.id.random, R.id.help_menu)) {
            menu.findItem(i).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        // The random-picker and help submenus would open downward from the
        // bottom bar, into the navigation-bar area. Show them as upward-opening
        // popups instead. Consuming the tap here preempts the framework
        // submenu: MenuItemImpl.invoke() runs this click listener first and
        // never shows a submenu when it returns true.
        menu.findItem(R.id.random).setOnMenuItemClickListener {
            showUpwardPopup(R.id.random, R.menu.popup_random)
            true
        }
        menu.findItem(R.id.help_menu).setOnMenuItemClickListener {
            showUpwardPopup(R.id.help_menu, R.menu.popup_help)
            true
        }
        updateDailyTitle()
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        updateDailyTitle()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (handleMenuItem(item)) {
            // https://issuetracker.google.com/issues/36994881
            invalidateOptionsMenu()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateDailyTitle() {
        // Label the daily entry with today's date, e.g. "Daily · Oct 21".
        menu?.findItem(R.id.daily)?.title =
            "${getString(R.string.daily)} · ${dailyDateLabel()}"
    }

    /**
     * Shows a small menu above the bottom bar, anchored to the tapped icon
     * (action-item views carry their menu item's ID, so findViewById locates
     * the exact icon). Gravity.TOP makes the popup open upward instead of
     * downward into the navigation-bar area.
     */
    private fun showUpwardPopup(anchorItemId: Int, menuRes: Int) {
        val anchor = binding.bottomBar.findViewById<View>(anchorItemId) ?: binding.bottomBar
        PopupMenu(this, anchor).apply {
            gravity = Gravity.TOP
            setForceShowIcon(true)
            menuInflater.inflate(menuRes, menu)
            setOnMenuItemClickListener { handleMenuItem(it) }
        }.show()
    }

    private fun bottomBarHeightPx(): Int {
        val tv = TypedValue()
        return if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true))
            TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        else (56 * resources.displayMetrics.density).roundToInt()
    }

    /**
     * The system-bar insets this screen handles itself. The root layout does
     * not fitSystemWindows, so these listeners are the single source of truth
     * — but only when the window actually draws edge-to-edge. Edge-to-edge is
     * enforced on Android 15+ for this app's targetSdk; on older versions the
     * decor reserves the system-bar areas itself, so applying the insets
     * again would double-count them.
     */
    private fun selfHandledInsets(windowInsets: WindowInsetsCompat): Insets =
        if (Build.VERSION.SDK_INT >= 35) windowInsets.getInsets(systemBars())
        else Insets.NONE

    /** Applies the table's side and bottom clearance (see selfHandledInsets). */
    private fun applyTableInsets(windowInsets: WindowInsetsCompat) {
        val insets = selfHandledInsets(windowInsets)
        binding.table.updatePadding(
            left = insets.left,
            right = insets.right,
            // Clear the bar's grown height, plus the largest of the nav-bar /
            // cutout / keyboard insets at the bottom edge, plus a margin.
            bottom = bottomBarHeightPx() + max(
                insets.bottom,
                windowInsets.getInsets(displayCutout() or ime()).bottom
            ) + (8 * resources.displayMetrics.density).roundToInt()
        )
    }

    private fun handleMenuItem(item: MenuItem): Boolean {
        var ret = true
        when (item.itemId) {
            R.id.settings -> startActivity(Intent(this, PrefsActivity::class.java))
            R.id.playlists -> startActivity(Intent(this, PlaylistsActivity::class.java))
            R.id.daily -> openDaily()
            R.id.random_all -> launchRandom(BackendName.all)
            R.id.random_favorites -> launchRandomFavorite()
            R.id.load -> loadGame()
            R.id.contents -> startActivity(Intent(this, HelpActivity::class.java).apply {
                putExtra(HelpActivity.TOPIC, "index")
            })
            R.id.feedback -> sendFeedbackDialog(this)
            else -> ret = false
        }
        return ret
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key != CHOOSER_STYLE_KEY) return
        val newGrid = "grid" == prefs.getString(CHOOSER_STYLE_KEY, "list")
        if (useGrid == newGrid) return
        useGrid = newGrid
        updateDailyTitle()
        itemBindings.values.forEach { it.text.visibility = if (useGrid) GONE else VISIBLE }
        rethinkColumns(true)
    }

    companion object {

        private val DEFAULT_STARRED = setOf(GUESS, KEEN, LIGHTUP, NET, SIGNPOST, SOLO, TOWERS)

    }
}