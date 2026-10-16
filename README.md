# Mi5G Prayer · مواقيتي

Simple offline prayer-times app (Android, Kotlin + Jetpack Compose + Material 3). Arabic (RTL) default, English strings.
No ads, no analytics, **no INTERNET permission**. Optional approximate location (ACCESS_COARSE_LOCATION, runtime).

- Calculation: [adhan-java](https://github.com/batoulapps/adhan-java) 1.2.1 (MIT, Batoul Apps), fully on-device.
- Default place: Al Buraimi, Oman (24.2508 N, 55.7931 E, Asia/Muscat).
- Default method **"Oman – Ministry of Awqaf"**: Fajr 18°, Isha 18°, plus precaution minutes
  (sunrise +1, Dhuhr +5, Asr +5, Maghrib +5). Fitted against the Ministry of Endowments & Religious
  Affairs (mara.gov.om) published Muscat timetable; matches to ≤1 min on the tested dates (see unit tests).
  Umm al-Qura is *not* the default: the Ministry's Isha–Maghrib gap varies (≈73–82 min) so it is angle-based, not fixed 90 min.
- Other methods: Umm al-Qura, MWL, Dubai, Qatar, Kuwait, Egyptian, Karachi, ISNA. Asr Shafi/Hanafi, high-latitude rule, per-prayer ±minutes.
- Hijri: java.time `HijrahDate` (Umm al-Qura) with ±2 day adjustment.
- Offline city list: 60 Oman wilayats/towns + Gulf & Arab capitals/major cities (approximate coordinates).

Build: `./gradlew --no-daemon testDebugUnitTest assembleDebug` (JDK 17). The debug signing key lives outside the repo in `../prayer-times-keystore/`.

Roadmap: M2 adhan notifications (per-prayer mute, exact alarms) · M3 qibla compass · M4 widget + settings polish.
