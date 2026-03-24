package com.appliedrec.veridregistry

import android.content.Context
import android.graphics.Bitmap
import com.appliedrec.facerecognition.r300.core.FaceTemplateVersionR300
import com.appliedrec.verid3.common.FaceTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Date

class TaggedFaceRepository(private val context: Context) {

    private val dao: TaggedFaceDao = AppDatabaseProvider
        .getDatabase(context)
        .taggedFaceDao()

    fun getAllFaces(): Flow<List<TaggedFaceEntity>> = dao.getAll()

    fun getLatestUserFaces(): Flow<List<TaggedFaceEntity>> = dao.getLatestUserFaces()

    fun getUserFaces(userName: String): Flow<List<TaggedFaceEntity>> = dao.getUserFaces(userName)

    fun getUserCount(): Flow<Int> = dao.getUserCount()

    suspend fun insertFace(
        faceTemplate: FaceTemplate<FaceTemplateVersionR300, FloatArray>,
        userName: String,
        image: Bitmap?
    ): Long = withContext(Dispatchers.IO) {
        val entity = TaggedFaceEntity(
            dateAdded = Date(),
            userName = userName,
            templateData = faceTemplate.data
        )
        val id = dao.insert(entity)
        image?.let { ImageUtils.saveFaceImage(context, it, id) }
        id
    }

    suspend fun deleteFace(id: Long) = withContext(Dispatchers.IO) {
        dao.delete(id)
        ImageUtils.deleteFaceImage(context, id)
    }

    suspend fun deleteUser(userName: String) = withContext(Dispatchers.IO) {
        val faces = dao.getUserFaces(userName).first()
        dao.deleteUser(userName)
        faces.forEach { ImageUtils.deleteFaceImage(context, it.id) }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        val faces = dao.getAll().first()
        dao.deleteAll()
        faces.forEach { ImageUtils.deleteFaceImage(context, it.id) }
    }

    suspend fun getFaceImage(id: Long): Bitmap? = withContext(Dispatchers.IO) {
        ImageUtils.getFaceImage(context, id)
    }
}
