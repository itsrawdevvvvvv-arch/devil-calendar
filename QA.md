# QA checklist

Everything below compiles, passes lint and has unit tests where the logic allows
it. **None of it has been run on a physical device or emulator** - the build
sandbox has no Android runtime. Treat this as the list of things to check the
first time you install it.

## Highest risk first

1. **Room migrations.** The database is now at version 7 with five migrations
   (2→3 workout tables, 3→4 routine kind, 4→5 holds, 5→6 coach configs,
   6→7 moods). Room validates the schema when the database opens, so a mismatch
   crashes on first launch over an older build. Install over 1.8.0 and open the
   app before doing anything else.
2. **Camera coach rep counting.** Thresholds and smoothing are guesses. Test
   push-ups and squats first; they are the easiest to track.
3. **Dynamic island overlay.** Confirm the pill appears, that only the pill is
   touchable, and that it survives a reboot.
4. **Rest timer overtime.** The ring must not vanish at zero.

## Install

- [ ] Fresh install on Android 9 and Android 14+.
- [ ] Upgrade over the previous release; no crash on first open.
- [ ] No crash without the camera permission.

## Workout

- [ ] Create a routine: name, add an exercise by typing, set sets and rest.
- [ ] Autocomplete only offers names you typed before.
- [ ] Switch an exercise to HOLD and set seconds.
- [ ] Create a single workout; it hides the add field once it has one.
- [ ] Duplicate and delete a routine.

## Live session

- [ ] Start from a routine; the configured number of sets appears.
- [ ] Tick a set: ember burst, row morph, PR chip when it beats history.
- [ ] Rest starts automatically; let it hit zero and confirm overtime counts up.
- [ ] +15 / -15 work during rest, ignored during overtime.
- [ ] Swipe a set right to complete it.
- [ ] Ghost numbers show from the last session.
- [ ] Copy last session fills the sets.
- [ ] Add an exercise mid-session.
- [ ] Screen stays awake.
- [ ] Force-stop mid-session and reopen: the session is intact.

## AI

- [ ] Paste a key, TEST, expect "Connected".
- [ ] Paste fenced JSON; the preview parses.
- [ ] Paste `Push-ups 4x12 rest 60s`; the offline parser handles it.
- [ ] Generate with Sarvam with the key removed: a clear error, no crash.

## Island

- [ ] Grant all four permissions; the counter reads 4 of 4.
- [ ] Idle pill appears under the status bar.
- [ ] Rest timer takes over the pill; expand, use -15/+15/Next.
- [ ] Music shows the equaliser; double-tap pauses.
- [ ] A chat notification does **not** take over the pill.
- [ ] Tilt the phone; the rim light moves.
- [ ] Reboot: the island comes back.

## Camera coach

- [ ] Camera permission prompt appears once.
- [ ] Skeleton draws yellow when tracked, red when lost.
- [ ] Rep counter increments once per rep, not twice.
- [ ] -1/+1 corrects a miscount.
- [ ] Hold exercises count seconds only while the pose holds.

## History and streak

- [ ] Finished sessions appear with duration, sets and volume.
- [ ] Tapping a session shows the exercises and sets.
- [ ] Progress chart renders per exercise.
- [ ] Calendar dots brighten with volume.
- [ ] Mood row saves and shows in the trend.

## Performance

- [ ] Scroll every screen on a low-end device; no visible jank.
- [ ] Animations still run with "remove animations" enabled (they should not).
- [ ] Battery: the island and tilt sensor are the two things to watch.

## Known gaps

Deliberately not built, listed so they are not mistaken for bugs:

- Drag-to-reorder (up/down buttons instead).
- Shared-element transitions.
- Island: metaball merge, album-art glow, drag-to-reposition,
  hide-in-fullscreen.
- Coach: voice cues, post-set Sarvam feedback, auto-stop.
- Recap share image, workout widget, per-exercise notes UI, breathing ring,
  rep tempo mode, smart rest.
