<div align="center">

<img src="docs/store/icon_512.png" width="112" alt="مواقيتي">

# مواقيتي · Mi5G Prayer

**مواقيت صلاة للمسلمين في أي مكان في العالم عبر GPS، مع القبلة والأذكار والودجت والصامت التلقائي. يعمل بدون إنترنت، بلا إعلانات ولا تتبع.**

[![Release](https://img.shields.io/github/v/release/al3nkboot/prayer-times)](https://github.com/al3nkboot/prayer-times/releases/latest)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)

[**⬇️ تحميل أحدث إصدار (APK)**](https://github.com/al3nkboot/prayer-times/releases/latest)

</div>

<p align="center">
<img src="docs/store/phone/02_home_ar_dark.png" width="23%">
<img src="docs/store/phone/03_qibla_facing_ar.png" width="23%">
<img src="docs/store/phone/05_widget_large_ar_light.png" width="23%">
<img src="docs/store/phone/04_settings_notifications_ar.png" width="23%">
</p>

<div dir="rtl">

## المميزات

- **في أي مكان:** حدّد موقعك عبر GPS لتحصل على المواقيت أينما كنت، مع اعتماد المنطقة الزمنية لجهازك.
- **9 طرق حساب:** أم القرى، رابطة العالم الإسلامي، دبي، قطر، الكويت، الهيئة المصرية، كراتشي، أمريكا الشمالية (ISNA)، وعُمان (وزارة الأوقاف)، مع المذهب الحنفي في العصر، وقواعد خطوط العرض العليا، وتعديل الدقائق لكل صلاة.
- طريقة عُمان مطابقة لتقويم وزارة الأوقاف لعام 2026 بفارق لا يتجاوز دقيقة واحدة.
- **قائمة مدن بدون GPS:** 89 مدينة تشمل العواصم العربية وتفاصيل مدن عُمان.
- **الأذان والتنبيهات:** أذان أو تنبيه أو صامت لكل صلاة، وتذكير قبل الأذان، وإمكانية اختيار صوت أذان من جهازك.
- **صامت تلقائي وقت الصلاة:** يحوّل الجوال إلى صامت أو اهتزاز لمدة تختارها، ثم يعيده إلى وضعه السابق.
- **أذكار الصباح والمساء:** من «حصن المسلم» مع عدّاد للتكرار وتذكير اختياري.
- **اتجاه القبلة:** بوصلة مع تصحيح الانحراف المغناطيسي.
- **ودجت للشاشة الرئيسية:** بثلاثة أحجام، مع عدّاد تنازلي للصلاة القادمة.
- **التاريخ الهجري** (أم القرى) مع تعديل ± يوم.
- **عربي وإنجليزي** مع وضع فاتح وداكن.
- **المنطقة الزمنية:** مع الموقع التلقائي تُستخدم المنطقة الزمنية للجهاز وتتحدّث تلقائياً عند تغيّرها؛ المدن المختارة يدوياً تحتفظ بمنطقتها الزمنية.
- **خصوصية تامة:** لا يطلب إذن الإنترنت، وتُحسب الأوقات على جهازك.

## التثبيت

1. حمّل ملف APK من [صفحة الإصدارات](https://github.com/al3nkboot/prayer-times/releases/latest).
2. افتحه واسمح بالتثبيت من هذا المصدر.
3. فعّل «المنبّهات الدقيقة» من داخل التطبيق ليصل الأذان في وقته بالضبط.

التحديثات الجديدة تُثبَّت فوق الإصدار السابق دون فقدان إعداداتك.

</div>

---

## English

**Mi5G Prayer** is an offline prayer-times app for Muslims anywhere, with a Qibla compass, adhkar, a home-screen widget and auto-silent: no ads, no tracking, no internet permission.

- **Anywhere via GPS:** prayer times for your current location, using your phone's time zone.
- **9 calculation methods:** Umm al-Qura, Muslim World League, Dubai, Qatar, Kuwait, Egyptian, Karachi, ISNA (North America) and Oman (Ministry of Awqaf), plus Hanafi Asr, high-latitude rules and per-prayer minute offsets.
- The Oman method matches the Ministry of Awqaf 2026 calendar within ±1 minute ([accuracy](docs/accuracy.md)).
- Manual city list (no GPS needed) of 89 cities: Arab capitals, plus detailed coverage of Oman.
- Adhan, alerts and pre-prayer reminders per prayer; custom adhan sound.
- Auto-silent during prayer, restoring your previous ringer mode afterwards.
- Morning and evening adhkar from Hisn al-Muslim with counters and reminders.
- Qibla compass, Hijri date (Umm al-Qura, ±day adjustment), and a responsive home-screen widget.
- Arabic (RTL) and English, light and dark themes.

**Download:** [latest release](https://github.com/al3nkboot/prayer-times/releases/latest) · Android 8.0+

### Build

```bash
./gradlew testDebugUnitTest assembleRelease   # JDK 17
```

Kotlin · Jetpack Compose · Material 3 · Glance · [adhan-java](https://github.com/batoulapps/adhan-java).
Signing keys are kept outside the repository. Developer notes: [docs/development-notes.md](docs/development-notes.md).

### Credits & privacy

Third-party sources and audio attribution: [CREDITS.md](CREDITS.md) · [Privacy policy](docs/privacy-policy.md)

Made by [@midroid5G](https://x.com/midroid5G).
