package com.midroid.prayertimes.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import com.midroid.prayertimes.domain.Place
import java.util.concurrent.Executors

/** Approximate (coarse) location via the platform LocationManager. No Play Services, no network calls by the app. */
object Locator {
    @SuppressLint("MissingPermission")
    fun request(context: Context, onResult: (Location?) -> Unit) {
        val lm = context.getSystemService(LocationManager::class.java) ?: return onResult(null)
        val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        val last = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        if (provider == null) return onResult(last)
        if (Build.VERSION.SDK_INT >= 30) {
            val main = context.mainExecutor
            runCatching {
                lm.getCurrentLocation(provider, CancellationSignal(), Executors.newSingleThreadExecutor()) { loc ->
                    main.execute { onResult(loc ?: last) }
                }
            }.onFailure { onResult(last) }
        } else onResult(last)
    }

    /** Turn coordinates into a Place: named after the nearest bundled city (timezone taken from it). */
    fun toPlace(lat: Double, lon: Double): Place {
        val (near, km) = Cities.nearest(lat, lon)
        val ar = if (km < 25) near.nameAr else "قرب ${near.nameAr}"
        val en = if (km < 25) near.nameEn else "Near ${near.nameEn}"
        return Place("gps", ar, en, lat, lon, near.zone, near.countryAr, near.countryEn)
    }
}
