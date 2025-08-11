package com.appliedrec.veridregistry

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey

// Preference keys
object SettingKeys {
    val USE_BACK_CAMERA = booleanPreferencesKey("useBackCamera")
    val ENABLE_SPOOF_DETECTION = booleanPreferencesKey("enableSpoofDetection")
    val IDENTIFICATION_THRESHOLD = floatPreferencesKey("identificationThreshold")
}
