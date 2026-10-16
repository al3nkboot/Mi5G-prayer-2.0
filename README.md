# Mi5G Prayer · مواقيتي

Simple offline prayer-times app (Android, Kotlin + Jetpack Compose + Material 3). Arabic (RTL) default, English strings.
No ads, no analytics, **no INTERNET permission**. Optional approximate location (ACCESS_COARSE_LOCATION, runtime).

- Calculation: [adhan-java](https://github.com/batoulapps/adhan-java) 1.2.1 (MIT, Batoul Apps), fully on-device.
- Default place: Al Buraimi, Oman (24.2508 N, 55.7931 E, Asia/Muscat).
- Default method **"Oman – Ministry of Awqaf"**: Fajr 18°, Isha 18°, plus Dhuhr/Asr/Maghrib +5 min.
  Verified against the Ministry of Endowments & Religious Affairs calendar (mara.gov.om): Al Buraimi all of 2026
  within ±1 min (see `docs/accuracy.md`).
  Umm al-Qura is *not* the default: the Ministry's Isha–Maghrib gap varies (≈73–82 min) so it is angle-based, not fixed 90 min.
- Other methods: Umm al-Qura, MWL, Dubai, Qatar, Kuwait, Egyptian, Karachi, ISNA. Asr Shafi/Hanafi, high-latitude rule, per-prayer ±minutes.
- Hijri: java.time `HijrahDate` (Umm al-Qura) with ±2 day adjustment.
- Offline city list: 60 Oman wilayats/towns (GeoNames coordinates) + Gulf & Arab capitals/major cities.
- M2 (0.2.0): per-prayer alert Off / Silent / Adhan, optional reminder before prayer, exact alarms with explainer +
  inexact fallback, rolling 2-day window re-armed on boot/time/zone/settings change; Hijri date advances at Maghrib.
- M3 (0.3.0): Qibla compass (rotation vector, fallback accel+magnetometer, declination via GeomagneticField, calibration
  hint, haptic when facing). Per-wilayat Ministry fits. Alert sound = generated chime; adhan recording deferred — see `CREDITS.md`.

Build: `./gradlew --no-daemon testDebugUnitTest assembleDebug` (JDK 17). The debug signing key lives outside the repo in `../prayer-times-keystore/`.

Roadmap: M4 widget + settings polish.
