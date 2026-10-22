package io.github.nandomango702.stpuzzlerelay

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Configuration.HARDKEYBOARDHIDDEN_NO
import android.content.res.Configuration.KEYBOARD_NOKEYS
import android.content.res.Configuration.NAVIGATIONHIDDEN_YES
import android.content.res.Configuration.NAVIGATION_DPAD
import android.content.res.Configuration.NAVIGATION_TRACKBALL
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_MENU
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_ALWAYS
import android.view.MenuItem.SHOW_AS_ACTION_WITH_TEXT
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.WindowManager.BadTokenException
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.NavUtils
import androidx.core.app.ShareCompat.IntentBuilder
import androidx.core.content.edit
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.util.TypedValueCompat.pxToDp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import io.github.nandomango702.stpuzzlerelay.GameView.LimitDPIMode.LIMIT_AUTO
import io.github.nandomango702.stpuzzlerelay.GameView.LimitDPIMode.LIMIT_OFF
import io.github.nandomango702.stpuzzlerelay.GameView.LimitDPIMode.LIMIT_ON
import io.github.nandomango702.stpuzzlerelay.NightModeHelper.Companion.isNight
import io.github.nandomango702.stpuzzlerelay.Utils.readAllOf
import io.github.nandomango702.stpuzzlerelay.Utils.sendFeedbackDialog
import io.github.nandomango702.stpuzzlerelay.Utils.toastFirstFewTimes
import io.github.nandomango702.stpuzzlerelay.Utils.unlikelyBug
import io.github.nandomango702.stpuzzlerelay.backend.BRIDGES
import io.github.nandomango702.stpuzzlerelay.backend.BackendName
import io.github.nandomango702.stpuzzlerelay.backend.BackendName.Companion.byLowerCase
import io.github.nandomango702.stpuzzlerelay.backend.GameEngine
import io.github.nandomango702.stpuzzlerelay.backend.GameEngine.KeysResult
import io.github.nandomango702.stpuzzlerelay.backend.GameEngineImpl.Companion.fromLaunch
import io.github.nandomango702.stpuzzlerelay.backend.GameEngineImpl.Companion.getDefaultParams
import io.github.nandomango702.stpuzzlerelay.backend.GameEngineImpl.Companion.getPrefs
import io.github.nandomango702.stpuzzlerelay.backend.GameEngineImpl.Companion.identifyBackend
import io.github.nandomango702.stpuzzlerelay.backend.LOOPY
import io.github.nandomango702.stpuzzlerelay.backend.MINES
import io.github.nandomango702.stpuzzlerelay.backend.NET
import io.github.nandomango702.stpuzzlerelay.backend.PALISADE
import io.github.nandomango702.stpuzzlerelay.backend.UsedByJNI
import io.github.nandomango702.stpuzzlerelay.buttons.ArrowMode
import io.github.nandomango702.stpuzzlerelay.buttons.ButtonsView
import io.github.nandomango702.stpuzzlerelay.buttons.SEEN_SWAP_L_R_TOAST
import io.github.nandomango702.stpuzzlerelay.buttons.SWAP_L_R_KEY
import io.github.nandomango702.stpuzzlerelay.config.ConfigBuilder
import io.github.nandomango702.stpuzzlerelay.config.ConfigBuilder.Event.CFG_SETTINGS
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.ARROW_KEYS_KEY_SUFFIX
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.AUTO_ORIENT
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.AUTO_ORIENT_DEFAULT
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.BRIDGES_SHOW_H_KEY
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.CHOOSER_STYLE_KEY
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.COMPLETED_PROMPT_KEY
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.CONTROLS_REMINDERS_KEY
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.FULLSCREEN_KEY
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.LAST_PARAMS_PREFIX
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.LATIN_M_UNDO_SEEN
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.LATIN_SHOW_M_KEY
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.LIMIT_DPI_KEY
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.LONG_PRESS_TIMEOUT
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.MOUSE_BACK_KEY
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.MOUSE_LONG_PRESS_KEY
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.ORIENTATION_KEY
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.REDO_NEW_GAME_SEEN
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.SAVED_BACKEND
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.SAVED_COMPLETED_PREFIX
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.SAVED_GAME_PREFIX
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.SAVED_TIMER_PREFIX
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.STATE_PREFS_NAME
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.STAY_AWAKE_KEY
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.UNDO_NEW_GAME_SEEN
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.UNDO_REDO_KBD_DEFAULT
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.UNDO_REDO_KBD_KEY
import io.github.nandomango702.stpuzzlerelay.config.PrefsConstants.VICTORY_FLASH_KEY
import io.github.nandomango702.stpuzzlerelay.daily.DailyPuzzle
import io.github.nandomango702.stpuzzlerelay.databinding.CompletedDialogBinding
import io.github.nandomango702.stpuzzlerelay.databinding.MainBinding
import io.github.nandomango702.stpuzzlerelay.launch.DeprecatedProgressDialog
import io.github.nandomango702.stpuzzlerelay.launch.GameGenerator
import io.github.nandomango702.stpuzzlerelay.launch.GameGenerator.Companion.cleanUpOldExecutables
import io.github.nandomango702.stpuzzlerelay.launch.GameGenerator.Companion.executableIsMissing
import io.github.nandomango702.stpuzzlerelay.launch.GameLaunch
import io.github.nandomango702.stpuzzlerelay.launch.GameLaunch.Companion.fromContentURI
import io.github.nandomango702.stpuzzlerelay.launch.GameLaunch.Companion.fromSeed
import io.github.nandomango702.stpuzzlerelay.launch.GameLaunch.Companion.ofGameID
import io.github.nandomango702.stpuzzlerelay.launch.GameLaunch.Companion.ofLocalState
import io.github.nandomango702.stpuzzlerelay.launch.GameLaunch.Companion.ofSavedGameFromIntent
import io.github.nandomango702.stpuzzlerelay.launch.GameLaunch.Companion.toGenerate
import io.github.nandomango702.stpuzzlerelay.launch.GameLaunch.Companion.toGenerateFromChooser
import io.github.nandomango702.stpuzzlerelay.launch.GameLaunch.Companion.undoingOrRedoingNewGame
import io.github.nandomango702.stpuzzlerelay.launch.GameLaunch.Origin.BUTTON_OR_MENU_IN_ACTIVITY
import io.github.nandomango702.stpuzzlerelay.launch.GameLaunch.Origin.INTENT_COMPLEX_URI
import io.github.nandomango702.stpuzzlerelay.launch.MenuEntry
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.MessageFormat
import java.util.concurrent.Future
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.roundToInt

class GamePlay : ActivityWithLoadButton(), OnSharedPreferenceChangeListener, GameGenerator.Callback,
    ConfigBuilder.ActivityCallbacks, GameEngine.ActivityCallbacks {
    private lateinit var _binding: MainBinding
    private lateinit var statusBar: TextView
    private var timerView: TextView? = null
    private lateinit var buttons: ButtonsView
    private lateinit var mainLayout: RelativeLayout
    private lateinit var gameView: GameView
    private lateinit var prefs: SharedPreferences
    private lateinit var state: SharedPreferences
    private lateinit var gameGenerator: GameGenerator
    private var progress: DeprecatedProgressDialog? = null
    private val gameTypesById: MutableMap<Int, String> = linkedMapOf()
    private var gameTypesMenu = arrayOf<MenuEntry>()
    private var currentType = 0
    private var generationInProgress: Future<*>? = null
    private var solveEnabled = false
    private var customVisible = false
    private var undoEnabled = false
    private var redoEnabled = false
    private var undoIsLoadGame = false
    private var redoIsLoadGame = false
    private var undoToGame: String? = null
    private var redoToGame: String? = null
    private var gameWantsTimer = false

    // Solve timer: starts on the player's first move, stops when the puzzle is solved,
    // and pauses while the puzzle isn't visible (time away doesn't count).
    // solvePausedAtRealtime marks when the current pause began, or null when not paused.
    private var solveStartRealtime: Long? = null
    private var solveFinalMillis: Long? = null
    private var solvePausedAtRealtime: Long? = null

    // Playlist play mode: set from intent extras when launched from PlaylistsActivity.
    // playlistGames holds BackendName source names in play order; playlistIndex is the
    // position of the puzzle currently on screen (-1 when not in a playlist).
    private var playlistName: String? = null
    private var playlistGames: List<String> = emptyList()
    private var playlistIndex: Int = -1

    private fun inPlaylistMode() = playlistIndex in playlistGames.indices

    /** Date string ("2026-10-21") when this GamePlay is showing the daily puzzle, else null. */
    private var dailyModeDate: String? = null

    private fun inDailyMode() = dailyModeDate != null

    private fun readPlaylistExtras(intent: Intent) {
        val games = intent.getStringArrayListExtra(EXTRA_PLAYLIST_GAMES)
        if (!games.isNullOrEmpty()) {
            playlistName = intent.getStringExtra(EXTRA_PLAYLIST_NAME)
            playlistGames = games.toList()
            playlistIndex = intent.getIntExtra(EXTRA_PLAYLIST_INDEX, 0).coerceIn(games.indices)
        } else {
            playlistName = null
            playlistGames = emptyList()
            playlistIndex = -1
        }
    }

    // specifically the screenshots test
    @get:VisibleForTesting
    var gameEngine = GameEngine.NOT_LOADED_YET
        private set
    var currentBackend: BackendName? = null
    private var startingBackend: BackendName? = null
    private var lastKeys = ""
    private var lastKeysIfArrows = ""
    private var menu: Menu? = null
    private var maybeUndoRedo = "" + GameView.UI_UNDO.toChar() + GameView.UI_REDO.toChar()
    private var everCompleted = false
    private var _wasNight = false
    private var appStartIntentOnResume: Intent? = null
    private var swapLROn = false

    private enum class UIVisibility(val value: Int) {
        UNDO(1), REDO(2), CUSTOM(4), SOLVE(8), STATUS(16)
    }

    private enum class MsgType {
        TIMER, COMPLETED, BACK_TIME_ELAPSED, SOLVE_TIMER
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            // Consume the press and do nothing, because the user pressed a key very recently
            // so it was probably accidental. If they meant it, they can try again, by which time
            // this handler will be disabled.
        }
    }

    private class PuzzlesHandler(outer: GamePlay) : Handler(Looper.getMainLooper()) {
        val ref = WeakReference(outer)

        override fun handleMessage(msg: Message) {
            val outer = ref.get()
            outer?.handleMessage(msg)
        }
    }

    val handler: Handler = PuzzlesHandler(this)

    private fun handleMessage(msg: Message) {
        when (MsgType.entries[msg.what]) {
            MsgType.TIMER -> {
                if (progress == null) {
                    gameEngine.timerTick()
                }
                if (gameWantsTimer) {
                    handler.sendMessageDelayed(
                        handler.obtainMessage(MsgType.TIMER.ordinal),
                        TIMER_INTERVAL.toLong()
                    )
                }
            }

            MsgType.COMPLETED -> {
                try {
                    completedInternal()
                } catch (activityWentAway: BadTokenException) {
                    // fine, nothing we can do here
                    Log.d(TAG, "completed failed!", activityWentAway)
                }
            }

            MsgType.BACK_TIME_ELAPSED -> {
                onBackPressedCallback.isEnabled = false
            }

            MsgType.SOLVE_TIMER -> {
                updateSolveTimerDisplay()
                if (solveStartRealtime != null && solveFinalMillis == null) {
                    handler.sendMessageDelayed(
                        handler.obtainMessage(MsgType.SOLVE_TIMER.ordinal),
                        SOLVE_TIMER_INTERVAL.toLong()
                    )
                }
            }
        }
    }

    /** Elapsed solve time in milliseconds; 0 if the timer hasn't started. */
    private fun solveElapsedMillis(): Long {
        val start = solveStartRealtime ?: return 0
        return (solveFinalMillis ?: SystemClock.elapsedRealtime()) - start
    }

    private fun formatSolveTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%02d:%02d".format(minutes, seconds)
    }

    private fun updateSolveTimerDisplay() {
        timerView?.text = formatSolveTime(solveElapsedMillis())
    }

    /** Starts the solve timer on the player's first move of a puzzle. */
    private fun maybeStartSolveTimer() {
        if (solveStartRealtime == null && solveFinalMillis == null
            && currentBackend != null && progress == null
        ) {
            solveStartRealtime = SystemClock.elapsedRealtime()
            handler.sendMessage(handler.obtainMessage(MsgType.SOLVE_TIMER.ordinal))
        }
    }

    /** Freezes the timer at the moment the puzzle is solved. */
    private fun stopSolveTimer() {
        val start = solveStartRealtime
        if (start != null && solveFinalMillis == null) {
            solveFinalMillis = SystemClock.elapsedRealtime() - start
            handler.removeMessages(MsgType.SOLVE_TIMER.ordinal)
            updateSolveTimerDisplay()
        }
    }

    /** Clears the timer for a fresh puzzle. */
    private fun resetSolveTimer() {
        handler.removeMessages(MsgType.SOLVE_TIMER.ordinal)
        solveStartRealtime = null
        solveFinalMillis = null
        solvePausedAtRealtime = null
        updateSolveTimerDisplay()
    }

    /**
     * Restores the solve timer after a saved game is reloaded, so the clock
     * resumes instead of resetting. Falls back to zero if nothing was saved.
     */
    private fun restoreSolveTimer() {
        val backend = currentBackend
        val elapsed = if (backend != null) state.getLong(SAVED_TIMER_PREFIX + backend, -1) else -1
        resetSolveTimer()
        if (elapsed >= 0) {
            solveStartRealtime = SystemClock.elapsedRealtime() - elapsed
            updateSolveTimerDisplay()
            handler.sendMessage(handler.obtainMessage(MsgType.SOLVE_TIMER.ordinal))
        }
    }

    /**
     * Freezes the solve timer when the puzzle stops being visible.
     * The start time is shifted forward on resume, so time away doesn't count.
     */
    private fun pauseSolveTimer() {
        if (solveStartRealtime != null && solveFinalMillis == null && solvePausedAtRealtime == null) {
            solvePausedAtRealtime = SystemClock.elapsedRealtime()
            handler.removeMessages(MsgType.SOLVE_TIMER.ordinal)
        }
    }

    /** Unfreezes the solve timer when the puzzle becomes visible again. */
    private fun resumeSolveTimer() {
        val pausedAt = solvePausedAtRealtime
        solvePausedAtRealtime = null
        val start = solveStartRealtime
        if (start != null && solveFinalMillis == null) {
            if (pausedAt != null) {
                solveStartRealtime = start + (SystemClock.elapsedRealtime() - pausedAt)
            }
            if (!handler.hasMessages(MsgType.SOLVE_TIMER.ordinal)) {
                handler.sendMessage(handler.obtainMessage(MsgType.SOLVE_TIMER.ordinal))
            }
        }
    }

    private fun showProgress(launch: GameLaunch) {        progress = DeprecatedProgressDialog(this, launch, {
            abort(null, launch.origin.shouldReturnToChooserOnFail)
        }, {
            resetBackendState(launch.whichBackend)
            currentBackend = null // prevent save undoing our reset
            abort(null, true)
        })
    }

    private fun resetBackendState(backend: BackendName) {
        state.edit().apply {
            remove(SAVED_GAME_PREFIX + backend)
            remove(SAVED_COMPLETED_PREFIX + backend)
            remove(LAST_PARAMS_PREFIX + backend)
            remove(SAVED_TIMER_PREFIX + backend)
            apply()
        }
        prefs.edit { remove(backend.preferencesName) }
    }

    private fun dismissProgress() {
        try {
            progress?.dismiss()
        } catch (_: IllegalArgumentException) {
        } // race condition?
        progress = null
    }

    private fun saveToString(): String {
        check(currentBackend != null && progress == null) { "saveToString in invalid state" }
        val baos = ByteArrayOutputStream()
        gameEngine.serialise(baos)
        val saved = baos.toString()
        check(saved.isNotEmpty()) { "serialise returned empty string" }
        return saved
    }

    private fun save() {
        if (currentBackend == null) return
        val saved = saveToString()
        state.edit().apply {
            remove("engineName")
            putString(SAVED_BACKEND, currentBackend.toString())
            putString(SAVED_GAME_PREFIX + currentBackend, saved)
            putBoolean(SAVED_COMPLETED_PREFIX + currentBackend, everCompleted)
            putString(LAST_PARAMS_PREFIX + currentBackend, gameEngine.currentParams)
            // Persist the solve timer alongside the board, so leaving and coming
            // back resumes the clock instead of resetting it.
            val timerKey = SAVED_TIMER_PREFIX + currentBackend
            val elapsed = solveElapsedForSave()
            if (elapsed >= 0) putLong(timerKey, elapsed) else remove(timerKey)
            apply()
        }
    }

    /**
     * Accumulated solve time in ms for saving; -1 if the timer never started
     * or the puzzle is already solved (solved games are never restored).
     */
    private fun solveElapsedForSave(): Long {
        val start = solveStartRealtime ?: return -1
        if (solveFinalMillis != null) return -1
        val end = solvePausedAtRealtime ?: SystemClock.elapsedRealtime()
        return max(0, end - start)
    }

    fun gameViewResized() {
        if (progress == null && gameView.w > 10 && gameView.h > 10) {
            val bottomInsetPx = if (resources.configuration.orientation == ORIENTATION_LANDSCAPE) {
                val insets = ViewCompat.getRootWindowInsets(gameView)?.getInsets(systemBars() or displayCutout() or ime())
                max(insets?.bottom?.minus(statusBar.height) ?: 0, 0)
            } else 0
            val bottomInset = pxToDp(bottomInsetPx.toFloat(), resources.displayMetrics).toInt()
            gameEngine.resizeEvent(gameView.wDip, gameView.hDip, bottomInset)
        }
    }

    fun suggestDensity(w: Int, h: Int): Float {
        return gameEngine.suggestDensity(w, h)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)
        state = getSharedPreferences(STATE_PREFS_NAME, MODE_PRIVATE)
        gameGenerator = GameGenerator()
        applyStayAwake()
        applyOrientation()
        super.onCreate(savedInstanceState)
        readPlaylistExtras(intent)
        if (executableIsMissing(this)) {
            finish()
            return
        }
        inflateContent()
        applyFullscreen()
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT)
        onBackPressedDispatcher.addCallback(onBackPressedCallback)
        appStartIntentOnResume = intent
        cleanUpOldExecutables(prefs, state, File(applicationInfo.dataDir))
    }

    // Also called from onConfigurationChanged()
    private fun inflateContent() {
        _binding = MainBinding.inflate(layoutInflater)
        setContentView(_binding.root)
        setSupportActionBar(_binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayUseLogoEnabled(false)
            addOnMenuVisibilityListener { visible: Boolean ->
                // https://code.google.com/p/android/issues/detail?id=69205
                if (!visible) {
                    this@GamePlay.invalidateOptionsMenu()
                    rethinkActionBarCapacity()
                }
            }
        }
        mainLayout = _binding.mainLayout
        statusBar = _binding.statusBar
        timerView = _binding.timerView
        updateSolveTimerDisplay()
        gameView = _binding.gameView
        ViewCompat.setOnApplyWindowInsetsListener(gameView) { v, windowInsets ->
            gameViewResized()
            WindowInsetsCompat.CONSUMED
        }
        buttons = _binding.buttons.apply {
            onKeyListener.value = { c: Int, isRepeat: Boolean ->
                val ch = c.toChar()
                val maybeLowercase =
                    if (!swapLROn && currentBackend === PALISADE && "HJKL".indexOf(ch) > -1) ch.lowercaseChar()
                    else ch
                runOnUiThread {
                    sendKey(0, 0, maybeLowercase.code, isRepeat)
                }
            }
            onSwapLRListener.value = { swap: Boolean ->
                runOnUiThread {
                    setSwapLR(swap)
                    toastFirstFewTimes(
                        this@GamePlay, state, SEEN_SWAP_L_R_TOAST, 4,
                        if (swap) R.string.toast_swap_l_r_on else R.string.toast_swap_l_r_off
                    )
                }
            }
        }
        gameView.requestFocus()
        _wasNight = isNight(resources.configuration)
        applyLimitDPI(false)
        applyLongPressTimeout()
        applyMouseLongPress()
        applyMouseBackKey()
        window.setBackgroundDrawable(null)
    }

    /** work around Android issue 21181 whose bug page has vanished :-(  */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return progress == null && (gameView.onKeyDown(keyCode, event)
                || super.onKeyDown(keyCode, event))
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEYCODE_MENU) {
            if (hackForSubmenus == null) openOptionsMenu()
            hackForSubmenus!!.performIdentifierAction(R.id.game_menu, 0)
            return true
        }
        // work around Android issue 21181
        return progress == null && (gameView.onKeyUp(keyCode, event)
                || super.onKeyUp(keyCode, event))
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        // Only delegate for MENU key, delegating for other keys might break something(?)
        if (progress == null && event.keyCode == KEYCODE_MENU && gameView.dispatchKeyEvent(event))
            true
        else
            super.dispatchKeyEvent(event)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readPlaylistExtras(intent)
        // Daily mode is per-launch: any new intent without the extra leaves daily mode.
        dailyModeDate = intent.getStringExtra(EXTRA_DAILY_DATE)
        progress?.let {
            stopGameGeneration()
            dismissProgress()
        }
        var backendFromChooser: BackendName? = null
        // Don't regenerate on resurrecting a URL-bound activity from the recent list
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0) {
            val s = intent.getStringExtra("game")
            val u = intent.data
            if (!s.isNullOrEmpty()) {
                Log.d(TAG, "starting game from Intent, " + s.length + " bytes")
                val launch: GameLaunch = try {
                    ofSavedGameFromIntent(s)
                } catch (e: IllegalArgumentException) {
                    abort(e.message, true) // invalid file
                    return
                }
                startGame(launch)
                return
            } else if (u != null) {
                Log.d(TAG, "URI is: \"$u\"")
                if (OUR_SCHEME == u.scheme) {
                    val split = u.schemeSpecificPart.split(":".toRegex(), limit = 2).toTypedArray()
                    val incoming = byLowerCase(split[0])
                    if (incoming == null) {
                        abort("Unrecognised game in URI: $u", true)
                        return
                    }
                    if (split.size > 1) {
                        if (split[1].contains(":")) {
                            startGame(ofGameID(incoming, split[1], INTENT_COMPLEX_URI))
                        } else {  // params only
                            startGame(toGenerate(incoming, split[1], INTENT_COMPLEX_URI))
                        }
                        return
                    }
                    if (incoming == currentBackend && !everCompleted
                        && intent.getStringExtra(EXTRA_DAILY_DATE) == null
                    ) {
                        // already alive & playing incomplete game of that kind; keep it.
                        // (A daily launch always proceeds: the user explicitly asked for today's puzzle.)
                        return
                    }
                    backendFromChooser = incoming
                }
                if (backendFromChooser == null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && ("file".equals(u.scheme, ignoreCase = true) || u.scheme == null)
                    ) {
                        unlikelyBug(this, R.string.old_file_manager)
                        return
                    }
                    try {
                        checkSize(u)
                        startGame(fromContentURI(readAllOf(contentResolver.openInputStream(u))))
                    } catch (e: IllegalArgumentException) {
                        e.printStackTrace()
                        abort(e.message, true)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        abort(e.message, true)
                    }
                    return
                }
            }
        }
        if (backendFromChooser != null) {
            val dailyDate = dailyModeDate
            if (dailyDate != null) {
                // Daily puzzle: deterministic seed from the date, always generated fresh.
                // Raw default params (no per-device orientation tweak) so every player
                // gets the identical puzzle that day.
                val params = getDefaultParams(backendFromChooser)
                Log.d(TAG, "generating daily puzzle for $dailyDate")
                startGame(fromSeed(backendFromChooser, "$params#$dailyDate"))
                return
            }
            val savedGame =
                state.getString(SAVED_GAME_PREFIX + backendFromChooser, null)
            // We have a saved game, and if it's completed the user probably wants a fresh one.
            // Theoretically we could silently load it and ask midend_status() but remembering is
            // still faster and some people play large games.
            val wasCompleted = state.getBoolean(
                SAVED_COMPLETED_PREFIX + backendFromChooser,
                false
            )
            if (savedGame == null || wasCompleted) {
                Log.d(TAG, "generating as requested")
                startGame(toGenerateFromChooser(backendFromChooser))
                return
            }
            Log.d(TAG, "restoring last state of $backendFromChooser")
            startGame(ofLocalState(backendFromChooser, savedGame, true))
        } else {
            val savedBackend = byLowerCase(state.getString(SAVED_BACKEND, null))
            if (savedBackend != null) {
                val savedGame =
                    state.getString(SAVED_GAME_PREFIX + savedBackend, null)
                if (savedGame == null) {
                    Log.e(TAG, "missing state for $savedBackend")
                    startGame(toGenerateFromChooser(savedBackend))
                } else {
                    Log.d(TAG, "normal launch; resuming game of $savedBackend")
                    startGame(ofLocalState(savedBackend, savedGame, false))
                }
            } else {
                Log.d(TAG, "no state, starting chooser")
                startChooserAndFinish()
            }
        }
    }

    private fun warnOfStateLoss(
        backend: BackendName,
        continueLoading: Runnable,
        returnToChooser: Boolean
    ) {
        var careAboutOldGame =
            !state.getBoolean(SAVED_COMPLETED_PREFIX + backend, true)
        if (careAboutOldGame) {
            val savedGame = state.getString(SAVED_GAME_PREFIX + backend, null)
            if (savedGame == null || savedGame.contains("NSTATES :1:1")) {
                careAboutOldGame = false
            }
        }
        if (careAboutOldGame) {
            AlertDialog.Builder(this@GamePlay)
                .setMessage(
                    MessageFormat.format(getString(R.string.replaceGame), backend.displayName)
                )
                .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                    continueLoading.run()
                }
                .setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int ->
                    abort(null, returnToChooser)
                }
                .show()
        } else {
            continueLoading.run()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        this.menu = menu
        menuInflater.inflate(R.menu.main, menu)
        applyUndoRedoKbd()
        return true
    }

    private var hackForSubmenus: Menu? = null
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        hackForSubmenus = menu
        updateUndoRedoEnabled()
        val enableType = generationInProgress != null || gameTypesById.isNotEmpty() || customVisible
        menu.findItem(R.id.type_menu).apply {
            isEnabled = enableType
            isVisible = enableType
        }
        return true
    }

    private fun orientGameType(backendName: BackendName?, type: String): String {
        if (!prefs.getBoolean(AUTO_ORIENT, AUTO_ORIENT_DEFAULT)) {
            return type
        }
        val viewLandscape = gameView.w > gameView.h
        val matcher = DIMENSIONS.matcher(type)
        if (matcher.matches()) {
            with(matcher) {
                val w = group(1)!!.toInt()
                val h = group(3)!!.toInt()
                val typeLandscape = w > h
                if (typeLandscape != viewLandscape) {
                    return group(3)!! + group(2)!! + "x" + group(2)!! + group(1) + group(4)!!
                }
            }
        }
        return type
    }

    private fun updateUndoRedoEnabled() {
        val undoItem = menu!!.findItem(R.id.undo)
        val redoItem = menu!!.findItem(R.id.redo)
        undoItem.isEnabled = undoEnabled
        redoItem.isEnabled = redoEnabled
        undoItem.setIcon(if (undoEnabled) R.drawable.ic_action_undo else R.drawable.ic_action_undo_disabled)
        redoItem.setIcon(if (redoEnabled) R.drawable.ic_action_redo else R.drawable.ic_action_redo_disabled)
    }

    private fun startChooserAndFinish() {
        NavUtils.navigateUpFromSameTask(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        var ret = true
        // these all build menus on demand because we had to wait for anchor buttons to exist
        when (itemId) {
            R.id.game_menu -> doGameMenu()
            R.id.type_menu -> doTypeMenu()
            R.id.help_menu -> doHelpMenu()
            android.R.id.home -> if (inPlaylistMode()) finish() else startChooserAndFinish()
            R.id.undo -> sendKey(0, 0, GameView.UI_UNDO)
            R.id.redo -> sendKey(0, 0, GameView.UI_REDO)
            else -> ret = super.onOptionsItemSelected(item)
        }
        // https://code.google.com/p/android/issues/detail?id=69205
        invalidateOptionsMenu()
        rethinkActionBarCapacity()
        return ret
    }

    private fun doGameMenu() {
        val gameMenu = popupMenuWithIcons()
        gameMenu.menuInflater.inflate(R.menu.game_menu, gameMenu.menu)
        val solveItem = gameMenu.menu.findItem(R.id.solve)
        solveItem.isEnabled = solveEnabled
        solveItem.isVisible = solveEnabled
        // Re-show the completion dialog if it was dismissed (e.g. via the back button).
        val showResultItem = gameMenu.menu.findItem(R.id.show_result)
        showResultItem.isEnabled = reshowCompletion != null
        showResultItem.isVisible = reshowCompletion != null
        gameMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.new_game -> startNewGame()
                R.id.show_result -> reshowCompletion?.invoke()
                R.id.restart -> {
                    resetSolveTimer()
                    gameEngine.restartEvent()
                }
                R.id.solve -> solveMenuItemClicked()
                R.id.load -> loadGame()
                R.id.save -> try {
                    saveLauncher.launch(suggestFilenameForShare())
                } catch (_: ActivityNotFoundException) {
                    unlikelyBug(this, R.string.saf_missing_short)
                }
                R.id.share -> share()
                R.id.settings -> startActivity(Intent(this@GamePlay, PrefsActivity::class.java)
                    .putExtra(PrefsActivity.PrefsMainFragment.BACKEND_EXTRA, currentBackend.toString()))
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        gameMenu.show()
    }

    private fun solveMenuItemClicked() {
        try {
            gameEngine.solveEvent()
        } catch (e: IllegalArgumentException) {
            messageBox(getString(R.string.Error), e.message, false)
        }
    }

    private val typeClickListener = MenuItem.OnMenuItemClickListener { item: MenuItem ->
        val itemId = item.itemId
        if (itemId == R.id.custom) {
            gameEngine.configEvent(this, CFG_SETTINGS.jni, this, currentBackend!!)
        } else {
            val presetParams = orientGameType(
                currentBackend, gameTypesById[itemId]!!
            )
            Log.d(TAG, "preset: $itemId: $presetParams")
            startGame(
                toGenerate(currentBackend!!, presetParams, BUTTON_OR_MENU_IN_ACTIVITY)
            )
        }
        true
    }

    private fun doTypeMenu(
        menuEntries: Array<MenuEntry> = gameTypesMenu,
        includeCustom: Boolean = true
    ) {
        val typeMenu = PopupMenu(this@GamePlay, findViewById(R.id.type_menu))
        typeMenu.menuInflater.inflate(R.menu.type_menu, typeMenu.menu)
        for (entry in menuEntries) {
            val added = typeMenu.menu.add(
                R.id.typeGroup,
                entry.id,
                Menu.NONE,
                orientGameType(currentBackend, entry.title)
            )
            if (entry.params != null) {
                added.setOnMenuItemClickListener(typeClickListener)
                if (currentType == entry.id) {
                    added.isChecked = true
                }
            } else if (entry.submenu != null) {
                if (menuContainsCurrent(entry.submenu)) {
                    added.isChecked = true
                }
                added.setOnMenuItemClickListener {
                    doTypeMenu(entry.submenu, false)
                    true
                }
            }
        }
        typeMenu.menu.setGroupCheckable(R.id.typeGroup, true, true)
        if (includeCustom) {
            typeMenu.menu.findItem(R.id.custom).apply {
                isVisible = customVisible
                setOnMenuItemClickListener(typeClickListener)
                if (currentType < 0) isChecked = true
            }
        }
        typeMenu.show()
    }

    private fun menuContainsCurrent(submenu: Array<MenuEntry>?): Boolean {
        for (entry in submenu!!) {
            if (entry.id == currentType) {
                return true
            }
            if (entry.submenu != null && menuContainsCurrent(entry.submenu)) {
                return true
            }
        }
        return false
    }

    private fun doHelpMenu() {
        PopupMenu(this@GamePlay, findViewById(R.id.help_menu)).apply {
            setForceShowIcon(true)
            menuInflater.inflate(R.menu.help_menu, menu)
            menu.findItem(R.id.solve).apply {
                isEnabled = solveEnabled
                isVisible = solveEnabled
            }
            menu.findItem(R.id.this_game).title = MessageFormat.format(
                getString(R.string.how_to_play_game), this@GamePlay.title
            )
            setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.this_game -> {
                        startActivity(Intent(this@GamePlay, HelpActivity::class.java)
                                .putExtra(HelpActivity.TOPIC, gameEngine.htmlHelpTopic()))
                        return@setOnMenuItemClickListener true
                    }
                    R.id.solve -> {
                        solveMenuItemClicked()
                        return@setOnMenuItemClickListener true
                    }
                    R.id.feedback -> {
                        sendFeedbackDialog(this@GamePlay)
                        return@setOnMenuItemClickListener true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun popupMenuWithIcons(): PopupMenu =
        PopupMenu(this, findViewById(R.id.game_menu)).apply {
            setForceShowIcon(true)
        }

    private fun share() {
        val saved = saveToString()
        val uriWithMimeType: Uri = try {
            writeCacheFile(saved)
        } catch (_: IOException) {
            unlikelyBug(this, R.string.cache_fail_short)
            return
        }
        val intentBuilder = IntentBuilder(this)
            .setStream(uriWithMimeType)
            .setType(MIME_TYPE)
        startActivity(intentBuilder.createChooserIntent())
    }

    @Throws(IOException::class)
    private fun writeCacheFile(content: String): Uri {
        val shareDir = File(cacheDir, "share").apply { mkdir() }
        val file = File(shareDir, suggestFilenameForShare())
        FileOutputStream(file).use {
            it.write(content.toByteArray())
        }
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    }

    private fun suggestFilenameForShare(): String = currentBackend!!.displayName + ".sgtp"

    private val saveLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument(MIME_TYPE)) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            try {
                val saved = saveToString()
                contentResolver.openFileDescriptor(uri, "w").use { pfd ->
                    if (pfd == null) throw IOException("Could not open $uri")
                    FileOutputStream(pfd.fileDescriptor).use { fos ->
                        fos.write(saved.toByteArray())
                    }
                }
            } catch (e: IOException) {
                messageBox(
                    getString(R.string.Error),
                    getString(R.string.save_failed_prefix) + e.message,
                    false
                )
            }
        }

    private fun isPalisadeFullCursor(): Boolean =
        if (currentBackend === PALISADE) getPrefs(this, currentBackend!!)?.contains("cursor-mode=full") ?: false else false

    private fun abort(why: String?, returnToChooser: Boolean) {
        stopGameGeneration()
        dismissProgress()
        if (!why.isNullOrEmpty()) {
            messageBox(getString(R.string.Error), why, returnToChooser)
        } else if (returnToChooser) {
            startChooserAndFinish()
            return
        }
        startingBackend = currentBackend
        currentBackend?.let {
            setKeys(gameEngine.requestKeys(it, gameEngine.currentParams, isPalisadeFullCursor()))
        }
    }

    private fun startNewGame() {
        startGame(
            toGenerate(
                currentBackend!!,
                orientGameType(currentBackend, gameEngine.currentParams!!),
                BUTTON_OR_MENU_IN_ACTIVITY
            )
        )
    }

    override fun startGame(launch: GameLaunch) {
        startGame(launch, false)
    }

    private fun startGame(launch: GameLaunch, isRedo: Boolean) {
        Log.d(TAG, "startGame: $launch")
        check(progress == null) { "startGame while already starting!" }
        val previousGame: String?
        if (isRedo || launch.needsGenerating) {
            gameEngine.purgeStates()
            redoToGame = null
            previousGame = if (currentBackend == null) null else saveToString()
        } else {
            previousGame = null
        }
        showProgress(launch)
        stopGameGeneration()
        startingBackend = launch.whichBackend
        if (launch.needsGenerating) {
            startGameGeneration(launch, previousGame)
        } else if (!launch.origin.isOfLocalState && launch.saved != null) {
            warnOfStateLoss(
                launch.whichBackend,
                { startGameConfirmed(launch, previousGame) },
                launch.origin.shouldReturnToChooserOnFail
            )
        } else {
            startGameConfirmed(launch, previousGame)
        }
    }

    private fun startGameGeneration(launch: GameLaunch, previousGame: String?) {
        val args = listOf(launch.whichBackend.toString()) +
                (launch.seed?.let { listOf("--seed", it) } ?: listOf(decideParams(launch)))
        generationInProgress =
            gameGenerator.generate(applicationInfo, launch, args, previousGame, this)
    }

    private fun decideParams(launch: GameLaunch): String {
        launch.params?.let {
            Log.d(TAG, "Using specified params: $it")
            return it
        }
        getLastParams(launch.whichBackend)?.let {
            Log.d(TAG, "Using last params: $it")
            return it
        }
        orientGameType(launch.whichBackend, getDefaultParams(launch.whichBackend)).let {
            Log.d(TAG, "Using default params with orientation: $it")
            return it
        }
    }

    override fun gameGeneratorSuccess(launch: GameLaunch, previousGame: String?) {
        runOnUiThread { startGameConfirmed(launch, previousGame) }
    }

    override fun gameGeneratorFailure(e: Exception, launch: GameLaunch) {
        runOnUiThread {
            if (launch.origin.shouldResetBackendStateOnFail() && hasState(launch.whichBackend)) {
                resetBackendState(launch.whichBackend)
                generationInProgress = null
                dismissProgress()
                startGame(launch)
            } else {
                abort(e.message, launch.origin.shouldReturnToChooserOnFail)  // probably bogus params
            }
        }
    }

    private fun hasState(backend: BackendName): Boolean =
        state.run {
            contains(SAVED_GAME_PREFIX + backend)
                || contains(SAVED_COMPLETED_PREFIX + backend)
                || contains(LAST_PARAMS_PREFIX + backend)
        }

    private fun startGameConfirmed(launch: GameLaunch, previousGame: String?) {
        check(!(launch.saved == null && launch.gameID == null)) { "startGameConfirmed with un-generated game" }
        val changingGame = previousGame == null || startingBackend !== identifyBackend(previousGame)
        undoToGame = if (previousGame != null && !changingGame && previousGame != launch.saved) {
            previousGame
        } else {
            null
        }
        gameEngine = try {
            fromLaunch(launch, this, gameView, this)
        } catch (e: IllegalArgumentException) {
            abort(e.message, launch.origin.shouldReturnToChooserOnFail) // probably bogus params
            return
        }
        currentBackend = startingBackend
        refreshColours()
        gameView.resetZoomForClear()
        gameView.clear()
        applyUndoRedoKbd()
        gameView.keysHandled = 0
        everCompleted = false
        reshowCompletion = null
        if (launch.saved != null) {
            // Reloading a saved game: pick up the timer where it left off.
            restoreSolveTimer()
        } else {
            resetSolveTimer()
        }
        val currentParams = orientGameType(currentBackend, gameEngine.currentParams!!)
        refreshPresets(currentParams)
        gameView.setDragModeFor(currentBackend)
        title = currentBackend!!.displayName
        supportActionBar?.title = currentBackend!!.title
        val flags = gameEngine.uiVisibility
        changedState(
            flags and UIVisibility.UNDO.value > 0,
            flags and UIVisibility.REDO.value > 0
        )
        customVisible = flags and UIVisibility.CUSTOM.value > 0
        solveEnabled = flags and UIVisibility.SOLVE.value > 0
        setStatusBarVisibility(flags and UIVisibility.STATUS.value > 0)
        setKeys(gameEngine.requestKeys(currentBackend!!, currentParams, isPalisadeFullCursor()))
        inertiaFollow(false)
        // We have a saved completion flag but completion could have been undone; find out whether
        // it's really completed
        if (launch.origin.shouldHighlightCompletionOnLaunch() && gameEngine.isCompletedNow) {
            completed()
        }
        val hasArrows = computeArrowMode(currentBackend).hasArrows()
        gameEngine.setCursorVisibility(hasArrows)
        if (changingGame) {
            if (prefs.getBoolean(CONTROLS_REMINDERS_KEY, true)) {
                if (hasArrows || !showToastIfExists(currentBackend!!.controlsToastNoArrows)) {
                    showToastIfExists(currentBackend!!.controlsToast)
                }
            }
        }
        dismissProgress()
        gameView.rebuildBitmap()
        menu?.let { onPrepareOptionsMenu(it) }
        save()
    }

    private fun showToastIfExists(@StringRes reminderId: Int): Boolean {
        if (reminderId == 0) {
            return false
        }
        Toast.makeText(this@GamePlay, reminderId, Toast.LENGTH_LONG).show()
        return true
    }

    private fun refreshPresets(currentParams: String) {
        currentType = -1
        gameTypesMenu = gameEngine.presets
        gameTypesById.clear()
        populateGameTypesById(gameTypesMenu, currentParams)
    }

    private fun populateGameTypesById(menuEntries: Array<MenuEntry>, currentParams: String) {
        for (entry in menuEntries) {
            entry.params?.let {
                gameTypesById[entry.id] = it
                if (orientGameType(currentBackend, currentParams)
                        == orientGameType(currentBackend, it)) {
                    currentType = entry.id
                }
            }
            entry.submenu?.let {
                populateGameTypesById(it, currentParams)
            }
        }
    }

    private fun checkSize(uri: Uri) {
        val cursor =
            contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null, null)
        cursor?.use {
            it.moveToFirst()
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            if (it.isNull(sizeIndex)) return
            require(it.getInt(sizeIndex) <= MAX_SAVE_SIZE) { getString(R.string.file_too_big) }
        }
    }

    private fun getLastParams(whichBackend: BackendName): String? =
        state.getString(LAST_PARAMS_PREFIX + whichBackend, null)?.let {
            orientGameType(whichBackend, it)
        }

    private fun stopGameGeneration() {
        generationInProgress?.run {
            cancel(true)
            generationInProgress = null
        }
    }

    override fun onPause() {
        handler.removeMessages(MsgType.TIMER.ordinal)
        pauseSolveTimer()
        if (progress == null) save()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        resumeSolveTimer()
        appStartIntentOnResume?.let {
            onNewIntent(it)
            appStartIntentOnResume = null
        }
    }

    override fun onDestroy() {
        stopGameGeneration()
        gameGenerator.onDestroy()
        gameEngine.onDestroy()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(f: Boolean) {
        if (f && gameWantsTimer && currentBackend != null && !handler.hasMessages(MsgType.TIMER.ordinal)) {
            gameEngine.resetTimerBaseline()
            handler.sendMessageDelayed(handler.obtainMessage(MsgType.TIMER.ordinal), TIMER_INTERVAL.toLong())
        }
    }

    private fun setSwapLR(swap: Boolean) {
        if (!currentBackend!!.swapLRNatively) swapLROn = swap
        currentBackend!!.putSwapLR(this, swap)
        buttons.swapLR.value = swap
    }

    fun sendKey(p: PointF, k: Int) {
        sendKey(p.x.roundToInt(), p.y.roundToInt(), k)
    }

    @JvmOverloads
    fun sendKey(x: Int, y: Int, k: Int, isRepeat: Boolean = false) {
        if (progress != null || currentBackend == null) return
        if (k == GameView.UI_UNDO && undoIsLoadGame) {
            if (!isRepeat) {
                toastFirstFewTimes(this, state, UNDO_NEW_GAME_SEEN, 3, R.string.undo_new_game_toast)
                val launchUndo = undoingOrRedoingNewGame(undoToGame!!)
                redoToGame = saveToString()
                startGame(launchUndo)
            }
            return
        }
        if (k == GameView.UI_REDO && redoIsLoadGame) {
            if (!isRepeat) {
                toastFirstFewTimes(this, state, REDO_NEW_GAME_SEEN, 3, R.string.redo_new_game_toast)
                val launchRedo = undoingOrRedoingNewGame(redoToGame!!)
                redoToGame = null
                startGame(launchRedo, true)
            }
            return
        }
        maybeStartSolveTimer()
        gameEngine.keyEvent(x, y, maybeSwapMouse(k))
        if (GameView.CURSOR_KEYS.contains(k)) {
            gameView.ensureCursorVisible(gameEngine.cursorLocation)
        }
        if (k == 'M'.code && currentBackend!!.isLatin) {
            toastFirstFewTimes(
                this, state, LATIN_M_UNDO_SEEN, 3,
                R.string.latin_m_undo_toast
            )
        }
        gameView.requestFocus()
        onBackPressedCallback.isEnabled = true
        handler.removeMessages(MsgType.BACK_TIME_ELAPSED.ordinal)
        handler.sendMessageDelayed(
            handler.obtainMessage(MsgType.BACK_TIME_ELAPSED.ordinal),
            PREVENT_BACK_AFTER_KEY_PRESS_MILLIS.toLong()
        )
    }

    private fun maybeSwapMouse(k: Int): Int {
        if (swapLROn && k >= GameView.FIRST_MOUSE && k <= GameView.LAST_MOUSE) {
            val whichButton: Int = (k - GameView.FIRST_MOUSE) % 3
            if (whichButton == 0) {
                return k + 2 // left; send right
            } else if (whichButton == 2) {
                return k - 2 // right; send left
            }
        }
        return k
    }

    private fun setKeyboardVisibility(whichBackend: BackendName?, c: Configuration) {
        val newArrowMode = computeArrowMode(whichBackend)
        val shouldHaveSwap =
            lastArrowMode === ArrowMode.ARROWS_LEFT_RIGHT_CLICK || whichBackend === PALISADE || whichBackend === NET
        val maybeSwapLRKey = if (shouldHaveSwap) SWAP_L_R_KEY.toString() else ""
        val newKeys =
            if (shouldShowFullSoftKeyboard(c)) filterKeys(newArrowMode) + maybeSwapLRKey + maybeUndoRedo else maybeSwapLRKey + maybeUndoRedo
        val swap = whichBackend?.getSwapLR(this) ?: false
        if (whichBackend?.swapLRNatively != true) swapLROn = swap
        with (buttons) {
            backend.value = whichBackend ?: LOOPY
            keys.value = newKeys
            arrowMode.value = newArrowMode
            swapLR.value = swap
            disableCharacterIcons.value = disableCharacterIcons(whichBackend)
            undoEnabled.value = this@GamePlay.undoEnabled
            redoEnabled.value = this@GamePlay.redoEnabled
        }
    }

    private fun disableCharacterIcons(whichBackend: BackendName?): String {
        // Only Undead used this; it is not enabled in this build.
        return ""
    }

    private fun computeArrowMode(whichBackend: BackendName?): ArrowMode {
        val arrowPref = prefs.getBoolean(
            getArrowKeysPrefName(whichBackend, resources.configuration),
            getArrowKeysDefault(whichBackend, resources)
        )
        return if (arrowPref) lastArrowMode else ArrowMode.NO_ARROWS
    }

    private fun filterKeys(arrowMode: ArrowMode): String {
        var filtered = lastKeys
        startingBackend?.let {
            if (it === BRIDGES && !prefs.getBoolean(BRIDGES_SHOW_H_KEY, false)) {
                filtered = filtered.replace("H", "")
            }
            if (it.isLatin && !prefs.getBoolean(LATIN_SHOW_M_KEY, true)) {
                filtered = filtered.replace("M", "")
            }
        }
        if (arrowMode.hasArrows()) {
            filtered = lastKeysIfArrows + filtered
        } else if (filtered.length == 1 && filtered[0] == '\b') {
            filtered = ""
        }
        return filtered
    }

    private fun setStatusBarVisibility(visible: Boolean) {
        if (!visible) statusBar.text = ""
        mainLayout.updateViewLayout(statusBar, statusBar.layoutParams.apply {
            height = if (visible) RelativeLayout.LayoutParams.WRAP_CONTENT else 0
        })
        gameView.requestApplyInsets()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val statusBarText = statusBar.text
        val statusBarHeight = statusBar.layoutParams.height
        inflateContent()
        statusBar.text = statusBarText
        setStatusBarVisibility(statusBarHeight != 0)
        gameEngine.setViewCallbacks(gameView)
        setKeyboardVisibility(startingBackend, newConfig)
        refreshColours()
        val isNight = isNight(newConfig)
        if (isNight != _wasNight) {
            _wasNight = isNight
            statusBar.setTextColor(
                ResourcesCompat.getColor(resources, R.color.status_bar_text, theme)
            )
            statusBar.setBackgroundColor(
                ResourcesCompat.getColor(resources, R.color.game_background, theme)
            )
        }
        rethinkActionBarCapacity()
        invalidateOptionsMenu() // for orientation of presets in type menu
    }

    /** ActionBar's capacity (width) has probably changed, so work around
     * [Android issue 36933746](https://issuetracker.google.com/issues/36933746)
     * (invalidateOptionsMenu() does not help here)  */
    private fun rethinkActionBarCapacity() {
        menu?.apply {
            val dm = resources.displayMetrics
            val screenWidthDIP = (dm.widthPixels.toDouble() / dm.density).roundToInt()
            val state =
                if (screenWidthDIP >= 480) (SHOW_AS_ACTION_ALWAYS or SHOW_AS_ACTION_WITH_TEXT) else SHOW_AS_ACTION_ALWAYS
            for (i in listOf(R.id.game_menu, R.id.type_menu, R.id.help_menu)) {
                findItem(i).setShowAsAction(state)
            }
            val undoRedoKbd = prefs.getBoolean(UNDO_REDO_KBD_KEY, UNDO_REDO_KBD_DEFAULT)
            val undoItem = findItem(R.id.undo)
            val redoItem = findItem(R.id.redo)
            undoItem.isVisible = !undoRedoKbd
            redoItem.isVisible = !undoRedoKbd
            if (!undoRedoKbd) {
                undoItem.setShowAsAction(state)
                redoItem.setShowAsAction(state)
                updateUndoRedoEnabled()
            }
            // emulator at 598 dip looks bad with title+undo; GT-N7100 at 640dip looks good
            supportActionBar?.setDisplayShowTitleEnabled(screenWidthDIP > 620 || undoRedoKbd)
        }
    }

    private fun messageBox(title: String, msg: String?, returnToChooser: Boolean) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setOnCancelListener(if (returnToChooser) DialogInterface.OnCancelListener { startChooserAndFinish() } else null)
            .show()
    }

    @UsedByJNI
    override fun completed() {
        handler.sendMessageDelayed(handler.obtainMessage(MsgType.COMPLETED.ordinal), 0)
    }

    @UsedByJNI
    override fun inertiaFollow(isSolved: Boolean) {
        // Inertia is not enabled in this build, so this never hides the key.
        buttons.hidePrimary.value = false
    }

    /** Re-shows the completion dialog for the solved puzzle; null when there is none to show. */
    private var reshowCompletion: (() -> Unit)? = null

    private fun completedInternal() {
        everCompleted = true
        stopSolveTimer()
        // Daily puzzle: remember today's completion (first completion wins).
        dailyModeDate?.let { date ->
            solveFinalMillis?.let { final ->
                DailyPuzzle.recordCompletion(this, date, final)
            }
        }
        if (inPlaylistMode() && playlistIndex == playlistGames.size - 1) {
            reshowCompletion = ::showPlaylistCompleteDialog
            showPlaylistCompleteDialog()
            return
        }
        val copyStatusBar = currentBackend === MINES
        val titleText = if (copyStatusBar) statusBar.text else getString(R.string.COMPLETED)
        if (!prefs.getBoolean(COMPLETED_PROMPT_KEY, true)) {
            Toast.makeText(this@GamePlay, titleText, Toast.LENGTH_SHORT).show()
            return
        }
        reshowCompletion = ::showCompletedDialog
        showCompletedDialog()
    }

    /** The per-puzzle "COMPLETED!" dialog. Safe to call again to re-show it after dismissal. */
    private fun showCompletedDialog() {
        val copyStatusBar = currentBackend === MINES
        val titleText = if (copyStatusBar) statusBar.text else getString(R.string.COMPLETED)
        val d = Dialog(this, R.style.Dialog_Completed)
        val lp = d.window!!.attributes
        lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        d.window!!.attributes = lp
        val dialogBinding = CompletedDialogBinding.inflate(LayoutInflater.from(d.context))
        d.setContentView(dialogBinding.root)
        dialogBinding.completedTitle.text = solveFinalMillis?.let { final ->
            "$titleText · ${formatSolveTime(final)}"
        } ?: titleText
        // An accidental tap outside must not lose the dialog (and the Next button in playlists).
        d.setCanceledOnTouchOutside(false)
        darkenTopDrawable(dialogBinding.newGame)
        dialogBinding.newGame.setOnClickListener {
            d.dismiss()
            startNewGame()
        }
        darkenTopDrawable(dialogBinding.typeMenu)
        dialogBinding.typeMenu.setOnClickListener {
            d.dismiss()
            if (hackForSubmenus == null) openOptionsMenu()
            hackForSubmenus!!.performIdentifierAction(R.id.type_menu, 0)
        }
        val style = prefs.getString(CHOOSER_STYLE_KEY, "list")
        val useGrid = style == "grid"
        dialogBinding.otherGame.setCompoundDrawablesWithIntrinsicBounds(
            0,
            if (useGrid) R.drawable.ic_action_view_as_grid else R.drawable.ic_action_view_as_list,
            0,
            0
        )
        darkenTopDrawable(dialogBinding.otherGame)
        dialogBinding.otherGame.setOnClickListener {
            d.dismiss()
            // In a playlist, "Other game" exits the playlist back to the playlists list.
            if (inPlaylistMode()) finish() else startChooserAndFinish()
        }
        if (inPlaylistMode()) {
            dialogBinding.nextGame.visibility = View.VISIBLE
            darkenTopDrawable(dialogBinding.nextGame)
            dialogBinding.nextGame.setOnClickListener {
                d.dismiss()
                advancePlaylist()
            }
        }
        d.show()
    }

    /** Starts the next puzzle in the playlist; the solve timer resets via startGame. */
    private fun advancePlaylist() {
        val next = playlistIndex + 1
        if (next !in playlistGames.indices) return
        val backend = byLowerCase(playlistGames[next]) ?: return
        playlistIndex = next
        startGame(toGenerateFromChooser(backend))
    }

    private fun showPlaylistCompleteDialog() {
        val name = playlistName ?: getString(R.string.playlists)
        val time = solveFinalMillis?.let { "\n" + getString(R.string.playlist_final_time, formatSolveTime(it)) } ?: ""
        AlertDialog.Builder(this)
            .setTitle(R.string.playlist_complete)
            .setMessage(getString(R.string.playlist_complete_message, name) + time)
            .setPositiveButton(R.string.play_again) { _, _ ->
                val first = playlistGames.firstOrNull()?.let { byLowerCase(it) } ?: return@setPositiveButton
                playlistIndex = 0
                startGame(toGenerateFromChooser(first))
            }
            .setNegativeButton(R.string.done) { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun darkenTopDrawable(b: Button) {
        val drawable = DrawableCompat.wrap(b.compoundDrawables[1].mutate())
        DrawableCompat.setTint(drawable, Color.BLACK)
        b.setCompoundDrawables(null, drawable, null, null)
    }

    @UsedByJNI
    override fun setStatus(status: String) {
        statusBar.apply {
            text = status.ifEmpty { " " } // Ensure consistent height
            importantForAccessibility =
                if (status.isEmpty()) View.IMPORTANT_FOR_ACCESSIBILITY_NO else View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
    }

    @UsedByJNI
    override fun requestTimer(on: Boolean) {
        if (gameWantsTimer && on) return
        gameWantsTimer = on
        if (on) handler.sendMessageDelayed(handler.obtainMessage(MsgType.TIMER.ordinal), TIMER_INTERVAL.toLong())
        else handler.removeMessages(MsgType.TIMER.ordinal)
    }

    override fun customDialogError(error: String?) {
        dismissProgress()
        messageBox(getString(R.string.Error), error, false)
    }

    private var lastArrowMode = ArrowMode.NO_ARROWS

    private fun setKeys(result: KeysResult?) {
        if (result == null) return
        lastArrowMode = result.arrowMode ?: ArrowMode.ARROWS_LEFT_RIGHT_CLICK
        lastKeys = result.keys ?: ""
        lastKeysIfArrows = result.keysIfArrows ?: ""
        gameView.setHardwareKeys(lastKeys + lastKeysIfArrows)
        setKeyboardVisibility(startingBackend, resources.configuration)
    }

    override fun onSharedPreferenceChanged(p: SharedPreferences, key: String?) {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) return
        if (key == null) return
        val configuration = resources.configuration
        // = already started
        when (key) {
            currentBackend?.preferencesName -> {
                gameEngine.loadPrefs(this)
                if (currentBackend === PALISADE) {
                    // will also call setKeyboardVisibility
                    setKeys(gameEngine.requestKeys(PALISADE, gameEngine.currentParams, isPalisadeFullCursor()))
                } else {
                    // might have changed swapLR, or pictures vs letters in Undead
                    setKeyboardVisibility(startingBackend, configuration)
                }
                gameViewResized() // just for redraw
            }
            getArrowKeysPrefName(currentBackend, configuration) -> {
                setKeyboardVisibility(startingBackend, configuration)
                gameEngine.setCursorVisibility(computeArrowMode(startingBackend).hasArrows())
                gameViewResized() // cheat - we just want a redraw in case size unchanged
            }
            FULLSCREEN_KEY -> applyFullscreen()
            STAY_AWAKE_KEY -> applyStayAwake()
            LIMIT_DPI_KEY -> applyLimitDPI(true)
            ORIENTATION_KEY -> applyOrientation()
            UNDO_REDO_KBD_KEY -> applyUndoRedoKbd()
            BRIDGES_SHOW_H_KEY, LATIN_SHOW_M_KEY -> applyKeyboardFilters()
            LONG_PRESS_TIMEOUT -> applyLongPressTimeout()
            MOUSE_LONG_PRESS_KEY -> applyMouseLongPress()
            MOUSE_BACK_KEY -> applyMouseBackKey()
        }
    }

    private fun applyLongPressTimeout() {
        gameView.longPressTimeout = prefs.getInt(LONG_PRESS_TIMEOUT, ViewConfiguration.getLongPressTimeout())
    }

    private fun applyMouseLongPress() {
        val pref = prefs.getString(MOUSE_LONG_PRESS_KEY, "auto")
        gameView.alwaysLongPress = "always" == pref
        gameView.hasRightMouse = "never" == pref
    }

    private fun applyMouseBackKey() {
        gameView.mouseBackSupport = prefs.getBoolean(MOUSE_BACK_KEY, true)
    }

    private fun applyLimitDPI(alreadyStarted: Boolean) {
        val pref = prefs.getString(LIMIT_DPI_KEY, "auto")
        gameView.limitDpi =
            if ("auto" == pref) LIMIT_AUTO else if ("off" == pref) LIMIT_OFF else LIMIT_ON
        if (alreadyStarted) {
            gameView.rebuildBitmap()
        }
    }

    private fun applyFullscreen() {
        WindowInsetsControllerCompat(window, mainLayout).apply {
            if (prefs.getBoolean(FULLSCREEN_KEY, true)) {
                hide(systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                show(systemBars())
            }
        }
    }

    private fun applyStayAwake() {
        if (prefs.getBoolean(STAY_AWAKE_KEY, false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @SuppressLint("SourceLockedOrientationActivity") // This is only done at the user's explicit request
    private fun applyOrientation() {
        val orientationPref = prefs.getString(ORIENTATION_KEY, "unspecified")
        requestedOrientation = when (orientationPref) {
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    private fun refreshColours() {
        gameView.night = isNight(resources.configuration)
        currentBackend?.let {
            gameView.refreshColours(it, gameEngine.colours)
            gameView.clear()
            gameViewResized() // cheat - we just want a redraw
        }
    }

    private fun applyUndoRedoKbd() {
        val undoRedoKbd = prefs.getBoolean(UNDO_REDO_KBD_KEY, UNDO_REDO_KBD_DEFAULT)
        val wantKbd = if (undoRedoKbd) "UR" else ""
        if (wantKbd != maybeUndoRedo) {
            maybeUndoRedo = wantKbd
            startingBackend?.let {
                setKeyboardVisibility(it, resources.configuration)
            }
        }
        rethinkActionBarCapacity()
    }

    private fun applyKeyboardFilters() {
        setKeyboardVisibility(startingBackend, resources.configuration)
    }

    @UsedByJNI
    override fun changedState(canUndo: Boolean, canRedo: Boolean) {
        undoEnabled = canUndo || undoToGame != null
        undoIsLoadGame = !canUndo && undoToGame != null
        redoEnabled = canRedo || redoToGame != null
        redoIsLoadGame = !canRedo && redoToGame != null
        buttons.undoEnabled.value = undoEnabled
        buttons.redoEnabled.value = redoEnabled
        menu?.apply {
            findItem(R.id.undo)?.apply {
                isEnabled = undoEnabled
                setIcon(if (undoEnabled) R.drawable.ic_action_undo else R.drawable.ic_action_undo_disabled)
            }
            findItem(R.id.redo)?.apply {
                isEnabled = redoEnabled
                setIcon(if (redoEnabled) R.drawable.ic_action_redo else R.drawable.ic_action_redo_disabled)
            }
        }
    }

    @UsedByJNI
    override fun purgingStates() {
        redoToGame = null
    }

    @UsedByJNI
    override fun allowFlash(): Boolean {
        return prefs.getBoolean(VICTORY_FLASH_KEY, true)
    }

    @VisibleForTesting // specifically the screenshots test
    fun getGameView(): GameView {
        return _binding.gameView
    }

    companion object {
        private const val TAG = "GamePlay"
        const val OUR_SCHEME = "sgtpuzzles"
        const val MIME_TYPE = "text/prs.sgtatham.puzzles"
        /** Intent extras carrying playlist play-mode state from PlaylistsActivity. */
        const val EXTRA_PLAYLIST_NAME = "playlist_name"
        const val EXTRA_PLAYLIST_GAMES = "playlist_games"
        const val EXTRA_PLAYLIST_INDEX = "playlist_index"
        /** Intent extra carrying the daily puzzle date ("2026-10-21"); also puts GamePlay in daily mode. */
        const val EXTRA_DAILY_DATE = "daily_date"
        private val DIMENSIONS = Pattern.compile("(\\d+)( ?)x\\2(\\d+)(.*)")
        const val MAX_SAVE_SIZE: Long = 1000000 // 1MB; we only have 16MB of heap
        private const val TIMER_INTERVAL = 20
        private const val SOLVE_TIMER_INTERVAL = 500
        private const val PREVENT_BACK_AFTER_KEY_PRESS_MILLIS = 600

        /** Whether to show data-entry keys, as opposed to undo/redo/swap-L-R which are always shown.
         * We show data-entry if we either don't have a real hardware keyboard (we usually don't),
         * or we're on the Android SDK's emulator, which has the host's keyboard, but showing the full
         * keyboard is useful for UI development and screenshots.  */
        private fun shouldShowFullSoftKeyboard(c: Configuration): Boolean {
            return c.hardKeyboardHidden != HARDKEYBOARDHIDDEN_NO || c.keyboard == KEYBOARD_NOKEYS || isProbablyEmulator
        }

        private val isProbablyEmulator: Boolean
            get() = Build.MODEL.startsWith("sdk_") || Build.MODEL.startsWith("Android SDK")

        fun getArrowKeysPrefName(whichBackend: BackendName?, c: Configuration): String =
            (whichBackend.toString() + ARROW_KEYS_KEY_SUFFIX
                    + if (hasDpadOrTrackball(c)) "WithDpad" else "")

        fun getArrowKeysDefault(whichBackend: BackendName?, resources: Resources): Boolean =
            if (hasDpadOrTrackball(resources.configuration) && !isProbablyEmulator) false else whichBackend?.isArrowsVisibleByDefault ?: false

        private fun hasDpadOrTrackball(c: Configuration): Boolean =
            (c.navigation == NAVIGATION_DPAD || c.navigation == NAVIGATION_TRACKBALL) && c.navigationHidden != NAVIGATIONHIDDEN_YES

        init {
            System.loadLibrary("puzzles")
        }
    }
}
