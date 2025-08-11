package com.appliedrec.veridregistry

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appliedrec.verid3.common.FaceTemplate
import com.appliedrec.verid3.facerecognition.arcface.core.FaceTemplateVersionV24
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

class TaggedFacesViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: TaggedFaceDao = AppDatabaseProvider
        .getDatabase(application)
        .taggedFaceDao()

    val faceTemplates: StateFlow<List<TaggedFaceEntity>> =
        dao.getAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun insert(
        faceTemplate: FaceTemplate<FaceTemplateVersionV24, FloatArray>,
        userName: String,
        image: Bitmap?
    ) = viewModelScope.launch(Dispatchers.IO) {
        val entity = TaggedFaceEntity(
            dateAdded = Date(),
            userName = userName,
            templateData = faceTemplate.data
        )
        val id = dao.insert(entity)
        image?.let {
            ImageUtils.saveFaceImage(getApplication(), it, id)
        }
    }
}