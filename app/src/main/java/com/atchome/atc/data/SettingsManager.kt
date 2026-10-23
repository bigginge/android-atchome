package com.atchome.atc.data

import android.content.Context
import android.content.SharedPreferences

data class AppSettings(
    val homeLat: Double = 52.289514,
    val homeLon: Double = -0.865019,
    val radiusNm: Int = 25,
    val refreshIntervalSeconds: Int = 15,
    val keepScreenOn: Boolean = true,
    val showTrails: Boolean = true,
    val showRangeRings: Boolean = true,
    val showAirports: Boolean = true,
    val showSmallAirports: Boolean = false,
    val showMediumAirports: Boolean = true,
    val showLargeAirports: Boolean = true,
    val showSweep: Boolean = true,
    val showVelocityVectors: Boolean = true,
    val dataTagMode: String = "COMPACT", // "SELECTED", "COMPACT", "FULL"
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val reduceMotion: Boolean = false
)

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("atc_home_settings", Context.MODE_PRIVATE)

    fun loadSettings(): AppSettings {
        return AppSettings(
            homeLat = prefs.getString(KEY_LAT, "52.289514")?.toDoubleOrNull() ?: 52.289514,
            homeLon = prefs.getString(KEY_LON, "-0.865019")?.toDoubleOrNull() ?: -0.865019,
            radiusNm = prefs.getInt(KEY_RADIUS, 25),
            refreshIntervalSeconds = prefs.getInt(KEY_INTERVAL, 15),
            keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true),
            showTrails = prefs.getBoolean(KEY_SHOW_TRAILS, true),
            showRangeRings = prefs.getBoolean(KEY_SHOW_RINGS, true),
            showAirports = prefs.getBoolean(KEY_SHOW_AIRPORTS, true),
            showSmallAirports = prefs.getBoolean(KEY_SHOW_SMALL_AIRPORTS, false),
            showMediumAirports = prefs.getBoolean(KEY_SHOW_MEDIUM_AIRPORTS, true),
            showLargeAirports = prefs.getBoolean(KEY_SHOW_LARGE_AIRPORTS, true),
            showSweep = prefs.getBoolean(KEY_SHOW_SWEEP, true),
            showVelocityVectors = prefs.getBoolean(KEY_SHOW_VELOCITY_VECTORS, true),
            dataTagMode = prefs.getString(KEY_DATA_TAG_MODE, "COMPACT") ?: "COMPACT",
            themeMode = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM",
            reduceMotion = prefs.getBoolean(KEY_REDUCE_MOTION, false)
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_LAT, settings.homeLat.toString())
            .putString(KEY_LON, settings.homeLon.toString())
            .putInt(KEY_RADIUS, settings.radiusNm)
            .putInt(KEY_INTERVAL, settings.refreshIntervalSeconds)
            .putBoolean(KEY_KEEP_SCREEN_ON, settings.keepScreenOn)
            .putBoolean(KEY_SHOW_TRAILS, settings.showTrails)
            .putBoolean(KEY_SHOW_RINGS, settings.showRangeRings)
            .putBoolean(KEY_SHOW_AIRPORTS, settings.showAirports)
            .putBoolean(KEY_SHOW_SMALL_AIRPORTS, settings.showSmallAirports)
            .putBoolean(KEY_SHOW_MEDIUM_AIRPORTS, settings.showMediumAirports)
            .putBoolean(KEY_SHOW_LARGE_AIRPORTS, settings.showLargeAirports)
            .putBoolean(KEY_SHOW_SWEEP, settings.showSweep)
            .putBoolean(KEY_SHOW_VELOCITY_VECTORS, settings.showVelocityVectors)
            .putString(KEY_DATA_TAG_MODE, settings.dataTagMode)
            .putString(KEY_THEME_MODE, settings.themeMode)
            .putBoolean(KEY_REDUCE_MOTION, settings.reduceMotion)
            .apply()
    }

    companion object {
        private const val KEY_LAT = "home_latitude"
        private const val KEY_LON = "home_longitude"
        private const val KEY_RADIUS = "search_radius_nm"
        private const val KEY_INTERVAL = "refresh_interval_sec"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_SHOW_TRAILS = "show_trails"
        private const val KEY_SHOW_RINGS = "show_rings"
        private const val KEY_SHOW_AIRPORTS = "show_airports"
        private const val KEY_SHOW_SMALL_AIRPORTS = "show_small_airports"
        private const val KEY_SHOW_MEDIUM_AIRPORTS = "show_medium_airports"
        private const val KEY_SHOW_LARGE_AIRPORTS = "show_large_airports"
        private const val KEY_SHOW_SWEEP = "show_sweep"
        private const val KEY_SHOW_VELOCITY_VECTORS = "show_velocity_vectors"
        private const val KEY_DATA_TAG_MODE = "data_tag_mode"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_REDUCE_MOTION = "reduce_motion"
    }
}
