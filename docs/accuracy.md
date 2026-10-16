# Accuracy notes (v0.2.0)

## Reference source
Oman **Ministry of Endowments and Religious Affairs** monthly calendar, https://www.mara.gov.om/calendar_page2.asp
(form: year / month / CityID; Al Buraimi = CityID 5). Fetched 2026-10-16. Data: `docs/mara_2026_reference.csv`
(Al Buraimi every day of 2026; June + December 2026 for 52 other Ministry cities matched to our list).
The site's PM times (Asr/Maghrib/Isha) are printed 12-hour; converted to 24h.

> `calendar_page3.asp` (the "regions" table on the same site) looks mis-labelled: on 16 Oct 2026 its "البريمي" row
> (Dhuhr 11:52) is earlier than Muscat although Buraimi is ~2.6° further west; its "صحار" row equals the Buraimi
> monthly calendar. We therefore use only the per-city monthly calendar (`calendar_page2.asp`).

## Method derived from the Ministry data
Fajr **18°**, Isha **18°**, Asr Shafi'i (shadow 1), then **+5 min** to Dhuhr, Asr and Maghrib; Fajr, Sunrise, Isha unchanged.
- v0.1.0 also added +1 to Sunrise (fit on Muscat with a slightly-off longitude). With GeoNames coordinates the full-year
  Buraimi data shows Sunrise +0 is right (v0.1 was 1 min late on 350/365 days) → fixed in 0.2.0. Fajr unchanged.
- Al Buraimi, all 365 days × 6 times: **every value within ±1 min, 77.8 % exact** (Fajr 93 % exact). The ±1 residuals are
  symmetric (rounding/their exact solar model), so no extra offset helps.
- 52 other wilayats (Jun+Dec): all within ±1 min except Al Hamra, Rakhyut, Al Jazir, Mahout (≤3 min) whose
  Ministry reference point differs from the GeoNames town. **No per-region offsets are needed.**

## Al Buraimi: ours vs Ministry vs Salatuk
| Date | Source | Fajr | Sunrise | Dhuhr | Asr | Maghrib | Isha |
|---|---|---|---|---|---|---|---|
| 2026-01-01 | **Mi5G 0.2.0** | 05:39 | 07:00 | 12:25 | 15:26 | 17:45 | 19:01 |
| | Ministry | 05:39 | 07:00 | 12:25 | 15:25 | 17:45 | 19:01 |
| 2026-03-21 | **Mi5G 0.2.0** | 05:05 | 06:20 | 12:29 | 15:56 | 18:33 | 19:44 |
| | Ministry | 05:05 | 06:20 | 12:29 | 15:55 | 18:33 | 19:44 |
| 2026-06-21 | **Mi5G 0.2.0** | 04:01 | 05:29 | 12:24 | 15:43 | 19:13 | 20:36 |
| | Ministry | 04:01 | 05:29 | 12:24 | 15:43 | 19:13 | 20:36 |
| 2026-10-01 | **Mi5G 0.2.0** | 04:53 | 06:09 | 12:12 | 15:35 | 18:09 | 19:19 |
| | Ministry | 04:53 | 06:09 | 12:12 | 15:36 | 18:10 | 19:20 |
| 2026-10-16 | **Mi5G 0.2.0** | 04:59 | 06:15 | 12:07 | 15:26 | 17:55 | 19:05 |
| | Ministry | 04:59 | 06:15 | 12:08 | 15:27 | 17:55 | 19:06 |
| | Salatuk (screenshot) | 04:53 | 06:13 | 12:04 | 15:24 | 17:51 | 19:05 |
| 2026-12-21 | **Mi5G 0.2.0** | 05:35 | 06:56 | 12:20 | 15:19 | 17:39 | 18:55 |
| | Ministry | 05:35 | 06:56 | 12:20 | 15:19 | 17:39 | 18:55 |

### Why Salatuk differs
Salatuk's 16 Oct values are reproduced by Fajr **19.5°** / Isha **17.5°** (the Egyptian General Authority angles) plus its
own offsets (≈ Dhuhr +2, Asr +3, Maghrib +1, Isha +2, Sunrise −2). Its Fajr is 6 min earlier than the Ministry because of
the deeper 19.5° Fajr angle, not because of location. (Inferred from one day of screenshots; not confirmed with Salatuk.)

## Coordinates
Omani cities/wilayats use GeoNames (https://www.geonames.org, CC BY 4.0) – `OM.zip` dump fetched 2026-10-16 – see
`docs/sources.md`. Dima wa At Ta'iyin and Al Jazir use the GeoNames ADM2 (wilayat) centroid because no town point was found.
