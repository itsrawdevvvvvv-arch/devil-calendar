# Devil Calendar

A habit tracker where the calendar is the hero. Tap a date to cross it out with a
red slash; tap again to undo. Below the calendar sit three tools — **Timer**,
**Alarm** and **Focus**. When any of them finishes, a full-screen takeover appears
over everything, even if you are in another app. Bold, hell-themed, but clean and
modern.

Built with Kotlin and Jetpack Compose (Material 3).

---

## Screenshots

> Add your own captures here. Suggested set:
>
> | Home | Stats | Profile | Timer | Takeover |
> |------|-------|---------|-------|----------|
> | `docs/home.png` | `docs/stats.png` | `docs/profile.png` | `docs/timer.png` | `docs/takeover.png` |

---

## Requirements

| | |
|---|---|
| **Minimum Android** | 9.0 (API 28) |
| **Target Android** | 15 (API 35) |
| **JDK** | 17 |
| **Gradle** | 8.11.1 (wrapper included) |
| **Android Gradle Plugin** | 8.7.3 |
| **Kotlin** | 2.0.21 |

The `minSdk` is 28 (Android 9) rather than 26: the takeover, exact-alarm and
notification-policy APIs this app leans on behave consistently from API 28
onward, and it keeps the animation and foreground-service code on one code path.

## Build and run

```bash
# Debug APK
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk

# Unit tests
./gradlew testDebugUnitTest

# Lint
./gradlew lint
```

Open the project in Android Studio (Ladybug or newer) and run the `app`
configuration on a device or emulator running Android 9+.

On first launch the app walks through each permission it needs. Grant at least
**notifications**, **exact alarms** and **display over other apps** for the
takeover to work reliably.

---

## Architecture

MVVM, single-activity, Compose. Packages:

```
com.devil.calendar
├── data
│   ├── local        Room: day_log (crossed days), alarms
│   ├── prefs        DataStore: profile + settings
│   └── repository   DayRepository, AlarmRepository, ProfileRepository
├── domain           StatsCalculator (pure streak / week / month logic)
├── ui               theme, components, home, stats, profile, countdown,
│                    alarm, onboarding, takeover, nav
├── service          AlarmScheduler, receivers, AlertService,
│                    CountdownService, OverlayController, FocusSessionController
└── widget           Glance home-screen widget
```

- **Room** stores crossed days (`day_log`, keyed by `yyyy-MM-dd`) and saved alarms.
- **DataStore** stores the profile (name, goal, avatar path) and settings.
- **Hilt** provides the database, DAOs, DataStore and repositories.
- **Coroutines/Flow** carry state to the ViewModels; Compose collects with
  `collectAsStateWithLifecycle`.

## Design system

Colours, radii and type come straight from the prototype and live in
`ui/theme/`:

| Token | Value |
|---|---|
| ink | `#141414` |
| card | `#242424` |
| red | `#F2552C` |
| red-text | `#FF6A45` |
| yellow | `#F2D14B` |
| grey | `#E4E4E4` |
| takeover red | `#E8321A` |
| muted | `#9A9A9A` |

**Font.** `ProductSans-Bold.ttf` is the only font in the app. It is bundled at
`app/src/main/res/font/product_sans_bold.ttf`, exposed as a single `FontFamily`
(`ProductSans`) and used by every `TextStyle` in `ui/theme/Type.kt`. Every style
is `FontWeight.Bold` because the file has one weight, and font synthesis is
disabled so the platform can never fake another cut. Shapes use very large radii
(cards 28–32dp, pills fully rounded); there are no gradients and no drop shadows.

## Animations and performance

The app is animation-heavy but tuned so it stays fluid on low-end hardware:

- **Everything animates through `graphicsLayer`** where possible (fade + rise
  entrances, press scales, the day-cell slash). That runs on the render thread
  and skips recomposition and layout.
- **A single motion language** lives in `ui/theme/Motion.kt`: short springs and
  160–340 ms tweens, no long chained animations.
- **A device-aware motion budget.** `rememberMotionConfig()` reads
  `ActivityManager.isLowRamDevice`. On a constrained device it shortens
  durations to ~70%, drops the stagger between cards, and skips decorative
  motion. Behavioural feedback — a day crossing out, a button confirming — is
  always kept.
- **The ring and bars are drawn on a single `Canvas`** and animated with a float,
  so the sweep is smooth without recomposing the screen each frame.
- **Countdowns never tick the CPU.** The ongoing notification uses a system
  chronometer, and the trigger is an absolute `AlarmManager` timestamp — the UI
  ticker only runs while a screen is visible.

## Notifications and alarms

Two channels are created at startup:

| Channel | Importance | Used for |
|---|---|---|
| `timer_running` | LOW | the ongoing countdown notification |
| `alarm_alert` | HIGH, `CATEGORY_ALARM`, alarm audio attributes, bypasses DND where allowed | the alert that fires the takeover |

Every alert is scheduled with `AlarmManager.setAlarmClock(...)` from an **absolute
end timestamp** — never a ticking loop — so it survives Doze, a force-close and a
reboot. `canScheduleExactAlarms()` is checked and, when false, the app sends you
to `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. Alarms are persisted in Room
and re-armed by a `BOOT_COMPLETED` receiver.

## The takeover (over any app)

Android will not let a normal app draw over other apps, so the app uses three
layers together:

1. **Window-manager overlay** — from `AlertService`, a full-screen Compose view is
   added with `TYPE_APPLICATION_OVERLAY`. The overlay permission also allows the
   activity to be launched from the background.
2. **Full-screen intent** — covers a locked or off screen. On Android 14+ the app
   checks `NotificationManager.canUseFullScreenIntent()` and, if false, sends you
   to `Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`.
3. **`TakeoverActivity`** — `setShowWhenLocked(true)`, `setTurnScreenOn(true)`,
   `FLAG_KEEP_SCREEN_ON`, immersive sticky system bars, back button disabled,
   `excludeFromRecents`, `singleInstance`.

The takeover is a solid `#E8321A` screen with a huge `00:00`, a label
(`TIMER FINISHED` / `ALARM` / `FOCUS SESSION COMPLETE`) and a **HOLD TO DISMISS**
button that fills while held and dismisses after 1.2 seconds. A looping alarm
sound plays at alarm volume and the device vibrates in a pattern until dismissal,
which stops the sound, removes the overlay, cancels the notification and stops
the service.

## Focus lock

A focus session runs in a foreground service with a live countdown notification.
During a session the app calls `startLockTask()` (screen pinning) so you cannot
leave, and — if notification-policy access is granted — sets
`setInterruptionFilter(INTERRUPTION_FILTER_NONE)`, saving and restoring the
previous filter. **END SESSION** requires a 3-second hold to confirm, then calls
`stopLockTask()`, restores the filter and fires the takeover.

## Home-screen widget

A Jetpack Glance widget (`GlanceAppWidget` + `GlanceAppWidgetReceiver`),
resizable, minimum 4x4. It shows the current month on a dark rounded background
with the same slash and today styling as the app, the month title and the streak,
and reads from the same Room database. Tapping a day toggles its slash via
`actionRunCallback` and then refreshes every instance; tapping the header opens
the app. It refreshes at midnight via an exact alarm and after any profile change.

**Widget limitations (platform constraints).** Glance renders through
`RemoteViews`, which is more restricted than Compose:

- **Custom fonts are not reliably supported.** Glance's text styling does not
  expose a dependable way to load a bundled font in a widget, so the widget uses
  the system bold font rather than Product Sans. This is the one place the font
  differs, and it is a platform limitation rather than a choice. The in-app UI is
  unaffected.
- **Rotation is not available**, so the crossed-day slash is drawn as a vector
  drawable (`ic_widget_slash.xml`) rather than a rotated line. It reads the same
  at widget size.
- Day-cell corner radii are approximated with `cornerRadius` on the whole cell.

Everything else — the month grid, today's filled cell, the streak, tap-to-toggle —
matches the app.


### The widget is missing from the picker

The widget is registered with a 4x4 cell hint, a preview layout and a
monochrome adaptive launcher icon. If "Devil Calendar" still does not appear in
your launcher's widget list:

- **Xiaomi / Redmi (MIUI, HyperOS):** the launcher caches the widget list
  aggressively. Clear the launcher's cache (Settings -> Apps -> Manage apps ->
  your launcher -> Clear cache), or simply reinstall the app. The app must be
  launched at least once after install before the widget list refreshes.
- **Samsung (One UI):** the widget lives in the "Devil Calendar" group in the
  widget drawer; scroll the group list rather than searching the top level.
- **Any launcher:** a widget that fails to load is hidden. Confirm the app is
  installed for the current user and that it has been opened once.
- Widgets are only refreshed by Glance on change and by an exact alarm at
  midnight; `updatePeriodMillis` is deliberately `0`.

---

## Permissions guide

| Permission | Why it is needed | How to grant |
|---|---|---|
| `POST_NOTIFICATIONS` | A timer or alarm has to tell you it is done | Onboarding step, or Settings → Apps → Devil Calendar → Notifications (Android 13+) |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Fire at the precise second, even in Doze | Onboarding step, or Settings → Apps → Special access → Alarms & reminders |
| `USE_FULL_SCREEN_INTENT` | Cover the lock screen and wake the display | Onboarding step, or Settings → Apps → Special access → Full screen notifications (Android 14+) |
| `SYSTEM_ALERT_WINDOW` | Draw the takeover over other apps | Onboarding step, or Settings → Apps → Special access → Display over other apps |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Keep a running timer/focus session alive | Granted automatically; the subtype is declared in the manifest |
| `WAKE_LOCK` | Keep the screen on for the takeover | Automatic |
| `VIBRATE` | Vibrate the alarm pattern | Automatic |
| `RECEIVE_BOOT_COMPLETED` | Reschedule saved alarms after a reboot | Automatic |
| `ACCESS_NOTIFICATION_POLICY` | Do Not Disturb during a focus session | Onboarding / Settings → Apps → Special access → Do Not Disturb access |

The onboarding screen shows the live status of each of these and one button per
step. You can change any of them later from Settings.

### OEM battery optimisation (important)

Aggressive battery managers on some manufacturers kill background work and can
stop an alarm from firing. To keep alarms reliable, exclude Devil Calendar from
battery optimisation:

- **Xiaomi (MIUI/HyperOS)** — Settings → Apps → Manage apps → Devil Calendar →
  Battery saver → **No restrictions**. Also enable **Autostart**, and in Recents
  lock the app (pull down on its card). MIUI also has
  Settings → Battery & performance → App battery saver.
- **Oppo (ColorOS)** — Settings → Battery → App battery management (or Energy
  saving) → Devil Calendar → **Allow background activity**. Enable **Auto launch**
  under Settings → Apps → Auto-start.
- **Vivo (Funtouch/OriginOS)** — Settings → Battery → Background power
  consumption management → **Allow** for Devil Calendar. Enable **Auto-start**
  under Settings → Apps → Autostart, and allow **High background power
  consumption**.
- **Samsung (One UI)** — Settings → Battery → Background usage limits → make sure
  Devil Calendar is **not** in "Sleeping apps"; add it to "Never sleeping apps".
  Also Settings → Apps → Devil Calendar → Battery → **Unrestricted**.

The app also offers a **battery optimisation exemption** step in onboarding
(`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).

---

## Continuous integration

`.github/workflows/android.yml`:

- **On every push and pull request** — sets up JDK 17, runs
  `./gradlew assembleDebug testDebugUnitTest lint`, and uploads the debug APK as
  a build artifact.
- **On a tag matching `v*`** — builds a **signed** release APK and AAB using
  GitHub Secrets and attaches them to a GitHub Release.

### Release signing secrets

Add these repository secrets (Settings → Secrets and variables → Actions):

| Secret | Meaning |
|---|---|
| `KEYSTORE_BASE64` | your keystore, base64-encoded (`base64 -w0 release.jks`) |
| `KEY_ALIAS` | the key alias inside the keystore |
| `KEYSTORE_PASSWORD` | the keystore password |
| `KEY_PASSWORD` | the key password |

The release build stays unsigned when these are absent, so pull-request builds
never depend on secrets.

## Known OS limitations

- A normal app cannot truly block every other app; the takeover is the closest
  allowed behaviour and is achieved with the three layers described above.
- `startLockTask()` performs screen pinning, which the user can always exit by
  holding Back (Android reserves this escape hatch).
- Do Not Disturb during a focus session requires notification-policy access; if
  it is not granted, the session still runs and still locks the screen, but the
  interruption filter is left untouched.
- Custom fonts in the Glance widget (see above).
- Exact alarms are subject to the platform's exact-alarm permission from Android
  12; if it is revoked, alerts fall back to inexact delivery.

## Changelog

### Unreleased

- **Fixed:** edge-to-edge insets on every screen. Titles no longer collide with
  the status bar, and the bottom nav clears the navigation bar. Status bar icon
  colour follows the screen (dark icons on the white Stats screen).
- **Fixed:** the Stats screen no longer shows a black band behind the nav pill.
  There is now a single root background that animates between ink and white;
  no screen paints a background of its own.
- **Fixed:** the widget is registered correctly (preview layout, 4x4 cell hint,
  monochrome launcher icon) so it appears in the launcher's widget picker.
- **Fixed:** alarms can be set for AM/PM, repeat on chosen weekdays, carry a
  label, and ring. Adds an alarm list with per-alarm switches and swipe-to-delete
  with undo, plus a "Test alarm in 5 seconds" action in Profile that uses the
  production code path.
- **Added:** a reusable wheel picker. The timer is now set in hours, minutes and
  seconds with preset chips, and supports pause/resume/reset. Focus uses the same
  picker with hours and minutes only (default 25 minutes).
- **Added:** a 12/24-hour clock switch in Profile, defaulting to the system
  setting.
- **Added:** animation across the app - press feedback on every tap, a sliding
  nav pill, a slash that draws itself on the calendar, staggered stats bars,
  counting numbers, an animated takeover, and a single motion budget that
  shortens or disables animation on low-RAM devices or when the system
  animation scale is 0.
- **Added:** a starter baseline profile (`app/src/main/baseline-prof.txt`).
  A fuller profile covering scroll needs a device-generated run via
  Macrobenchmark, which is not possible in CI.

## License

MIT — see [LICENSE](LICENSE).
