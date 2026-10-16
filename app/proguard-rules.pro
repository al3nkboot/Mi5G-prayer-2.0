# adhan-java: plain Java, enums referenced by name in persisted settings via our own enums only; keep to be safe.
-keep class com.batoulapps.adhan.** { *; }
# Our enums are persisted by name (SharedPreferences): keep names.
-keepclassmembers enum com.midroid.prayertimes.** { *; }
# Receivers / services / widget are referenced from the manifest (AAPT generates keeps); Glance ships consumer rules.
-keep class com.midroid.prayertimes.widget.PrayerWidgetReceiver { *; }
-dontwarn org.slf4j.**
