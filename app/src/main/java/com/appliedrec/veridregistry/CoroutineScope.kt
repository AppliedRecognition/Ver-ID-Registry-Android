package com.appliedrec.veridregistry

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import com.appliedrec.verid3.common.FaceTemplate
import com.appliedrec.verid3.common.serialization.toBitmap
import com.appliedrec.verid3.common.use
import com.appliedrec.verid3.facecapture.CapturedFace
import com.appliedrec.verid3.facecapture.FaceCapture
import com.appliedrec.verid3.facecapture.FaceCaptureSessionResult
import com.appliedrec.verid3.facecapture.FaceCaptureSessionSettings
import com.appliedrec.verid3.facecapture.FaceTrackingPlugin
import com.appliedrec.verid3.facecapture.LivenessDetectionPlugin
import com.appliedrec.verid3.facecapture.ui.FaceCaptureConfiguration
import com.appliedrec.verid3.facecapture.ui.FaceCaptureViewConfiguration
import com.appliedrec.verid3.facedetection.retinaface.FaceDetectionRetinaFace
import com.appliedrec.verid3.facerecognition.arcface.cloud.FaceRecognitionArcFace
import com.appliedrec.verid3.facerecognition.arcface.core.FaceTemplateArcFace
import com.appliedrec.verid3.facerecognition.arcface.core.FaceTemplateVersionV24
import com.appliedrec.verid3.facetemplateregistry.FaceTemplateRegistry
import com.appliedrec.verid3.facetemplateregistry.IdentificationResult
import com.appliedrec.verid3.facetemplateregistry.TaggedFaceTemplate
import com.appliedrec.verid3.spoofdevicedetection.cloud.SpoofDeviceDetection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

private fun createFaceCaptureConfiguration(
    activity: ComponentActivity,
    useBackCamera: Boolean = false,
    enableSpoofDetection: Boolean = true
): FaceCaptureConfiguration {
    return FaceCaptureConfiguration(
        settings = FaceCaptureSessionSettings(),
        viewConfiguration = FaceCaptureViewConfiguration(
            activity,
            useBackCamera = useBackCamera
        ),
        createFaceDetection = {
            FaceDetectionRetinaFace.create(activity)
        },
        createFaceTrackingPlugins = {
            (if (enableSpoofDetection) {
                val spoofDetection = SpoofDeviceDetection(activity)
                listOf(
                    LivenessDetectionPlugin(arrayOf(spoofDetection)) as FaceTrackingPlugin<Any>
                )
            } else {
                listOf()
            })
        }
    )
}

fun CoroutineScope.captureFace(
    activity: ComponentActivity,
    useBackCamera: Boolean = false,
    enableSpoofDetection: Boolean = true,
    onFaceCaptured: (Result<CapturedFace>) -> Unit
) {
    launch(Dispatchers.Default) {
        try {
            val captureResult = FaceCapture.captureFaces(
                activity,
                createFaceCaptureConfiguration(activity, useBackCamera, enableSpoofDetection)
            )
            when (captureResult) {
                is FaceCaptureSessionResult.Success ->
                    Result.success(captureResult.capturedFaces.first())
                is FaceCaptureSessionResult.Failure ->
                    Result.failure(captureResult.error)
                is FaceCaptureSessionResult.Cancelled ->
                    null
            }?.let {
                withContext(Dispatchers.Main) {
                    onFaceCaptured(it)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onFaceCaptured(Result.failure(e))
            }
        }
    }
}

fun CoroutineScope.captureAndIdentifyFace(
    activity: ComponentActivity,
    useBackCamera: Boolean = false,
    enableSpoofDetection: Boolean = true,
    identificationThreshold: Float = 0.5f,
    onIdentification: (Result<List<IdentificationResult<FaceTemplateVersionV24, FloatArray>>>?) -> Unit
) {
    launch(Dispatchers.Default) {
        try {
            val captureResult = FaceCapture.captureFaces(
                activity,
                createFaceCaptureConfiguration(activity, useBackCamera, enableSpoofDetection)
            )
            val identificationResult = when (captureResult) {
                is FaceCaptureSessionResult.Success -> {
                    val capturedFace = captureResult.capturedFaces.first()
                    val result = createFaceTemplateRegistry(activity) { registry, dao ->
                        registry.identifyFace(capturedFace.face, capturedFace.image)
                    }
                    Result.success(result)
                }
                is FaceCaptureSessionResult.Failure ->
                    Result.failure(captureResult.error)
                is FaceCaptureSessionResult.Cancelled ->
                    null
            }
            withContext(Dispatchers.Main) {
                onIdentification(identificationResult)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onIdentification(Result.failure(e))
            }
        }
    }
}

fun CoroutineScope.captureAndRegisterFace(
    activity: ComponentActivity,
    userName: String,
    useBackCamera: Boolean = false,
    enableSpoofDetection: Boolean = true,
    onResult: (Result<FaceTemplate<FaceTemplateVersionV24, FloatArray>>?, Bitmap?) -> Unit
) {
    launch(Dispatchers.Default) {
        try {
            val captureResult = FaceCapture.captureFaces(
                activity,
                createFaceCaptureConfiguration(activity, useBackCamera, enableSpoofDetection)
            )
            val capturedFaceImage = captureResult.capturedFaces.firstOrNull()?.image?.toBitmap()
            try {
                val result = when (captureResult) {
                    is FaceCaptureSessionResult.Success -> {
                        val capturedFace = captureResult.capturedFaces.first()
                        createFaceTemplateRegistry(activity.applicationContext) { registry, dao ->
                            registerFace(
                                activity.applicationContext,
                                registry,
                                dao,
                                capturedFace,
                                userName
                            )
                        }
                    }

                    is FaceCaptureSessionResult.Failure -> {
                        throw captureResult.error
                    }

                    is FaceCaptureSessionResult.Cancelled -> {
                        null
                    }
                }?.let { success ->
                    Result.success(success)
                }
                withContext(Dispatchers.Main) {
                    onResult(result, capturedFaceImage)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(e), capturedFaceImage)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(Result.failure(e), null)
            }
        }
    }
}

fun CoroutineScope.registerFace(
    context: Context,
    capturedFace: CapturedFace,
    userName: String,
    onResult: (Result<FaceTemplate<FaceTemplateVersionV24, FloatArray>>) -> Unit
) {
    launch(Dispatchers.Default) {
        try {
            val result = createFaceTemplateRegistry(context) { registry, dao ->
                registerFace(context, registry, dao, capturedFace, userName)
            }
            withContext(Dispatchers.Main) {
                onResult(Result.success(result))
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(Result.failure(e))
            }
        }
    }
}

private suspend fun registerFace(
    context: Context,
    registry: FaceTemplateRegistry<FaceTemplateVersionV24, FloatArray>,
    dao: TaggedFaceDao,
    capturedFace: CapturedFace,
    userName: String
): FaceTemplate<FaceTemplateVersionV24, FloatArray> {
    val results = registry.registerFace(capturedFace.face, capturedFace.image, userName)
    val templateId = dao.insert(TaggedFaceEntity(
        templateData = results.data,
        dateAdded = Date(),
        userName = userName
    ))
    try {
        val image = ImageUtils.faceImageFromCapture(capturedFace)
        ImageUtils.saveFaceImage(context, image, templateId)
    } catch (e: Exception) {
        dao.delete(templateId)
        throw e
    }
    return results
}

private suspend fun <T> createFaceTemplateRegistry(
    context: Context,
    identificationThreshold: Float = 0.5f,
    block: suspend (FaceTemplateRegistry<FaceTemplateVersionV24, FloatArray>, dao: TaggedFaceDao) -> T
): T {
    val dao: TaggedFaceDao = AppDatabaseProvider
        .getDatabase(context)
        .taggedFaceDao()
    val taggedFaces = dao.getAll().first()
    val templates = taggedFaces.map {
        TaggedFaceTemplate(
            FaceTemplateArcFace(it.templateData),
            it.userName
        )
    }
    return FaceRecognitionArcFace(context).use { faceRecognition ->
        val config = FaceTemplateRegistry
            .Configuration(identificationThreshold = identificationThreshold)
        FaceTemplateRegistry(
            faceRecognition,
            templates,
            config
        ).use { registry ->
            block(registry, dao)
        }
    }
}