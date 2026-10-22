# Phase 1 notes — trim to 10 puzzles (2026-10-21)

## What changed
Kept exactly 10 puzzles: bridges, dominosa, loopy, mines, mosaic, net,
palisade, pattern, pearl, tents.

1. `app/src/main/jni/CMakeLists.txt`
   - Commented out the `puzzle(...)` blocks for the other 31 puzzles
     (blackbox, cube, fifteen, filling, flip, flood, galaxies, guess,
     inertia, keen, lightup, magnets, map, netslide, nullgame, pegs,
     range, rect, samegame, signpost, singles, sixteen, slant, solo,
     towers, tracks, twiddle, undead, unequal, unruly, untangle).
   - Also commented their `solver(...)` / `cliprogram(...)` /
     `guiprogram(...)` helper lines. Helpers for kept puzzles
     (e.g. `solver(tents)`, `cliprogram(patternpicture ...)`) were left alone.
   - Commenting (not deleting) so re-enabling is just uncommenting.

2. Kotlin UI code (`app/src/main/kotlin/...`) — required, see "why" below.
   - `backend/BackendName.kt`: arrow-key defaults, Latin-square flag, and
     drag-behavior table now cover only the 10 kept puzzles.
   - `buttons/Buttons.kt`: removed per-game button/icon tables entries for
     removed puzzles; default backend is now LOOPY; removed the two
     Solo-only `@Preview` design functions.
   - `GameChooser.kt`: default starred list is now just NET.
   - `GamePlay.kt`: removed per-game special cases (Inertia cursor follow,
     Solo orientation hack, Guess digit keys, Undead/Unqual keyboard tweaks,
     Flood/SameGame status-bar copy, etc.).
   - `PrefsActivity.kt`, `config/CustomDialogBuilder.kt`,
     `config/ConfigPreferencesBuilder.kt`: removed per-game preferences,
     notes, and dialog hacks for removed puzzles.

## Why the Kotlin changes were needed
The app's puzzle list comes from TWO places, and both ultimately read
`CMakeLists.txt`:
- The native game engine compiles only the puzzles whose `puzzle(...)`
  blocks are active (via `puzzle_sources` and the generated
  `generated-games.h`).
- The on-screen chooser list is generated at build time by
  `buildSrc/.../GenerateBackendsTask.kt`, which regex-scans
  `CMakeLists.txt` for `puzzle(... DISPLAYNAME ...)` lines and writes one
  Kotlin `object` per match into `BackendNames.kt`. Commented-out lines do
  not match, so only the 10 are generated.
- BUT several Kotlin files had hardcoded special cases naming the removed
  puzzles (e.g. "if this is Inertia, do X"). With those puzzle objects no
  longer generated, the app would not compile — so every hardcoded
  reference had to be pruned too.

## Verification (this build)
- `gradle assembleDebug`: BUILD SUCCESSFUL in 3m 31s.
- Generated `BackendNames.kt` contains exactly 10 objects
  (BRIDGES, DOMINOSA, LOOPY, MINES, MOSAIC, NET, PALISADE, PATTERN, PEARL,
  TENTS) — this is what the in-app chooser shows.
- Native `gamelist.txt` and `generated-games.h` list exactly the same 10 —
  this is what the game engine can actually run.
- Fresh APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Known leftovers (intentional, harmless for now)
- `app/src/androidTest/...` still imports removed backends (e.g. BLACKBOX).
  Instrumented tests are not compiled by `assembleDebug`; they will need
  updating before anyone runs the on-device test suite.
- Unused string/drawable resources for removed puzzles remain in `res/`;
  harmless, just unused.
- Changes are uncommitted in the working tree (`git diff` to review).
