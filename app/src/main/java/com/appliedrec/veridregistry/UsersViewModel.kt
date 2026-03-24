package com.appliedrec.veridregistry

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UsersViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaggedFaceRepository(application)

    val users: StateFlow<List<TaggedFaceEntity>> =
        repository.getLatestUserFaces()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun deleteUser(userName: String) {
        viewModelScope.launch {
            repository.deleteUser(userName)
        }
    }
}
