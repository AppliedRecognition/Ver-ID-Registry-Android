package com.appliedrec.veridregistry

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.appliedrec.verid3.common.use
import com.appliedrec.verid3.facerecognition.arcface.cloud.FaceRecognitionArcFace
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.appliedrec.verid3.facetemplateregistry.FaceTemplateRegistry
import kotlinx.coroutines.runBlocking

val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val defaults = runBlocking {
        FaceRecognitionArcFace(application).use { rec ->
            FaceTemplateRegistry.Configuration(
                rec.defaultThreshold,
                rec.defaultThreshold,
                rec.defaultThreshold
            )
        }
    }
    private val dataStore: DataStore<Preferences> = application.settingsDataStore

    val useBackCamera: StateFlow<Boolean> = dataStore.data.map {
            it[SettingKeys.USE_BACK_CAMERA] ?: false
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val enableSpoofDetection: StateFlow<Boolean> = dataStore.data.map {
            it[SettingKeys.ENABLE_SPOOF_DETECTION] ?: true
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val identificationThreshold: StateFlow<Float> = dataStore.data.map {
            it[SettingKeys.IDENTIFICATION_THRESHOLD] ?: defaults.identificationThreshold
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, defaults.identificationThreshold)

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
            it[SettingKeys.IDENTIFICATION_THRESHOLD] = defaults.identificationThreshold
        }
    }
}
