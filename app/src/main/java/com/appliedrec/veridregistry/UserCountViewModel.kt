package com.appliedrec.veridregistry

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserCountViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaggedFaceRepository(application)

    val userCount: StateFlow<Int> =
        repository.getUserCount()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    fun deleteAll() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }
}
