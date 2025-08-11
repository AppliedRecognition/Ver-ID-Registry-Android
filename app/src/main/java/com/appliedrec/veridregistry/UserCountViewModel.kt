package com.appliedrec.veridregistry

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserCountViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: TaggedFaceDao = AppDatabaseProvider
        .getDatabase(application)
        .taggedFaceDao()

    val userCount: StateFlow<Int> =
        dao.getUserCount()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    fun deleteAll() {
        viewModelScope.launch {
            dao.deleteAll()
        }
    }
}