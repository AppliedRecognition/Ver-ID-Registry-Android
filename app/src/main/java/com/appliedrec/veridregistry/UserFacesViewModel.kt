package com.appliedrec.veridregistry

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserFacesViewModel(
    application: Application,
    private val userName: String
) : AndroidViewModel(application) {

    private val dao: TaggedFaceDao = AppDatabaseProvider
        .getDatabase(application)
        .taggedFaceDao()

    val taggedFaces: StateFlow<List<TaggedFaceEntity>> =
        dao.getUserFaces(userName)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    fun deleteFace(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(id)
        }
    }
}

class UserFacesViewModelFactory(
    private val application: Application,
    private val userName: String
) : ViewModelProvider.AndroidViewModelFactory(application) {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserFacesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserFacesViewModel(application, userName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}