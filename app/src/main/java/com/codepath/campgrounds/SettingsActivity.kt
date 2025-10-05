package com.codepath.campgrounds

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var cachingSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        cachingSwitch = findViewById(R.id.caching_switch)

        // Load the current preferences
        val isCachingEnabled = getAppPreferences().getBoolean("enable_caching", true)
        cachingSwitch.isChecked = isCachingEnabled

        // Save the new preference
        cachingSwitch.setOnCheckedChangeListener { _, isChecked ->
            getAppPreferences().edit().putBoolean("enable_caching", isChecked).apply()
        }
    }

    private fun getAppPreferences(): SharedPreferences {
        return getSharedPreferences("CampgroundPrefs", Context.MODE_PRIVATE)
    }
}