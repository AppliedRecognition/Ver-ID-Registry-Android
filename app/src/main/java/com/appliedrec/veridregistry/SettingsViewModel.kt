package com.appliedrec.veridregistry

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appliedrec.facerecognition.r300.cloud.FaceRecognitionR300
import com.appliedrec.verid3.common.use
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore: DataStore<Preferences> = application.settingsDataStore

    // Loaded asynchronously to avoid blocking the main thread.
    // Falls back to 0.5f until the SDK reports its actual default.
    private val _defaultThreshold = MutableStateFlow(0.5f)

    init {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                _defaultThreshold.value = FaceRecognitionR300(application).use { it.defaultThreshold }
            } catch (_: Exception) {
                // keep fallback value
            }
        }
    }

    val useBackCamera: StateFlow<Boolean> = dataStore.data
        .map { it[SettingKeys.USE_BACK_CAMERA] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val enableSpoofDetection: StateFlow<Boolean> = dataStore.data
        .map { it[SettingKeys.ENABLE_SPOOF_DETECTION] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val identificationThreshold: StateFlow<Float> = combine(
        dataStore.data.map { it[SettingKeys.IDENTIFICATION_THRESHOLD] },
        _defaultThreshold
    ) { stored, default -> stored ?: default }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _defaultThreshold.value)

    fun setUseBackCamera(value: Boolean) = viewModelScope.launch {
        dataStore.edit { it[SettingKeys.USE_BACK_CAMERA] = value }
    }

    fun setEnableSpoofDetection(value: Boolean) = viewModelScope.launch {
        dataStore.edit { it[SettingKeys.ENABLE_SPOOF_DETECTION] = value }
    }

    fun setIdentificationThreshold(value: Float) = viewModelScope.launch {
        dataStore.edit { it[SettingKeys.IDENTIFICATION_THRESHOLD] = value }
    }

    fun reset() = viewModelScope.launch {
        dataStore.edit {
            it[SettingKeys.USE_BACK_CAMERA] = false
            it[SettingKeys.ENABLE_SPOOF_DETECTION] = true
            it[SettingKeys.IDENTIFICATION_THRESHOLD] = _defaultThreshold.value
        }
    }
}
