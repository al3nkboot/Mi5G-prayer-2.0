# Play Console permission declarations — Mi5G Prayer 1.0.0

## SCHEDULE_EXACT_ALARM (Alarms & reminders)
**Core functionality:** The app alerts the user at the exact start time of each of the five daily Islamic prayers
(Fajr, Dhuhr, Asr, Maghrib, Isha), optionally with a reminder a few minutes before. Prayer start times are
religiously significant to the minute; an inexact alarm delayed by Doze can make the user miss the start of the prayer
window (e.g. Maghrib). Alarms are user-configured per prayer (Off / Silent / Sound) and only a rolling 2-day window is
scheduled. If the user does not grant the permission the app falls back to inexact alarms and shows a visible note.
_(USE_EXACT_ALARM is intentionally **not** used.)_

**Declaration text (EN):** "Mi5G Prayer is a prayer-time alarm app. It uses exact alarms so the user's chosen
prayer-time alerts (call-to-prayer sound or notification) fire precisely at the start of each daily prayer, which the
user sets up per prayer in the app. Without exact alarms alerts may be delayed by several minutes."

## FOREGROUND_SERVICE_MEDIA_PLAYBACK (foreground service type: mediaPlayback)
**Use:** When a prayer alert set to "Sound" fires, the app starts a short-lived foreground service that plays the
chosen alert sound (bundled chime, bundled adhan ~1.5 min, or the user's own file) with a visible notification
containing a **Stop** action. The service stops itself when playback ends, when Stop is pressed, or when audio focus is
lost (e.g. a call). It is not started in silent/vibrate or Do-Not-Disturb mode.

**Declaration text (EN):** "The foreground service plays the user-selected call-to-prayer / alert sound at prayer
time, started by the user-configured alarm. It shows an ongoing notification with a Stop button and ends when playback
finishes (typically under 2 minutes). Video demo: start-of-prayer alarm → notification with Stop → audio stops."
_(A short screen recording of this flow is required by Play; not yet produced.)_

## Other permissions
- ACCESS_COARSE_LOCATION — optional, runtime; nearest city for prayer times/Qibla; on-device only.
- POST_NOTIFICATIONS — prayer alerts.
- RECEIVE_BOOT_COMPLETED — re-arm alarms after reboot.
- WAKE_LOCK — keep the CPU awake while the alert sound plays.
- FOREGROUND_SERVICE — required base permission for the above service.
