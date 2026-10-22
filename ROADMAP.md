# ST Puzzle Relay — Roadmap

Where the app is headed. Nothing here is a promise, just the plan.

## Shipped in v0.2.0
- Full puzzle set restored (all 40 upstream puzzles)
- "Show solution" now brings up the solved menu, stops the timer, and
  advances playlists / records daily completion like a manual solve
- Bottom action bar: no more dead space, submenus open upward
- Solve timer resets on restart / new game

## Up next
- **Daily puzzle integrity** — using "Show solution" on the daily no
  longer marks it complete (no streak credit for a cheated solve).
  Daily **streaks** for legit solves.
- **Timer options** — a setting to turn the solve timer on or off
  (some people like it, some don't), plus an opt-in to show your
  previous/best time on a puzzle so you can compare against it.
- **Stats page** — visual stats (bars and the like): completed vs.
  cheated solves and more. The cheated stat is already tracked by the
  engine, so it's just a matter of surfacing it.

## Back burner (maybe someday)
- **Pattern picture pack** — a daily picture set for Pattern. Nonogram
  apps already do this well, so it's not a priority.
- **Playlist sharing/export** — sounds cool, not urgent.

## Exploring → Planned: TalkBack / accessibility
Phased approach, after the "Up next" items ship:
- **Phase 1** — "describe board": a text dump of the puzzle state for
  TalkBack to read. Read-only, works across all puzzles.
- **Phase 2** — virtual-view grids (`ExploreByTouchHelper`) for the
  tractable puzzles, starting with Solo: full move entry via TalkBack.
  (Some puzzles, e.g. Untangle, may never be fully playable
  non-visually — that's fine.)

**v1.0 milestone:** the "Up next" items plus TalkBack support. Real
screen-reader support is something most puzzle apps don't have —
that's when this becomes a 1.0.

## Not planned
- Notifications (no daily reminders — the app stays quiet)
- Google Play signing (GitHub releases + unknown-sources approval is
  fine for now)

## Update log
- **v0.2.0** — full 40-puzzle restore; "Show solution" completion fix;
  bottom-bar fixes; roadmap created.
