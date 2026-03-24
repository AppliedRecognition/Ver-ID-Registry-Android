package com.appliedrec.veridregistry

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appliedrec.facerecognition.r300.cloud.FaceRecognitionR300
import com.appliedrec.facerecognition.r300.core.FaceTemplateR300
import com.appliedrec.facerecognition.r300.core.FaceTemplateVersionR300
import com.appliedrec.verid3.common.FaceTemplate
import com.appliedrec.verid3.common.use
import com.appliedrec.verid3.facecapture.CapturedFace
import com.appliedrec.verid3.facecapture.FaceCapture
import com.appliedrec.verid3.facecapture.FaceCaptureSessionResult
import com.appliedrec.verid3.facecapture.FaceCaptureSessionSettings
import com.appliedrec.verid3.facecapture.LivenessDetectionPlugin
import com.appliedrec.verid3.facecapture.ui.FaceCaptureConfiguration
import com.appliedrec.verid3.facecapture.ui.FaceCaptureViewConfiguration
import com.appliedrec.verid3.facedetection.retinaface.FaceDetectionRetinaFace
import com.appliedrec.verid3.facetemplateregistry.FaceTemplateRegistry
import com.appliedrec.verid3.facetemplateregistry.TaggedFaceTemplate
import com.appliedrec.verid3.spoofdevicedetection.cloud.SpoofDeviceDetection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class FaceSessionState {
    object Idle : FaceSessionState()
    object Capturing : FaceSessionState()
    object Registering : FaceSessionState()
    data class IdentificationComplete(val userName: String?) : FaceSessionState()
    data class RegistrationComplete(val userName: String) : FaceSessionState()
    data class RegistrationError(
        val capturedFaceImage: Bitmap?,
        val error: Throwable,
        val enteredName: String
    ) : FaceSessionState()
    data class CaptureError(val error: Throwable) : FaceSessionState()
}

class FaceSessionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaggedFaceRepository(application)

    // Holds the captured face across the registration intro → review flow.
    private val _capturedFace = MutableStateFlow<CapturedFace?>(null)
    val capturedFace: StateFlow<CapturedFace?> = _capturedFace.asStateFlow()

    private val _sessionState = MutableStateFlow<FaceSessionState>(FaceSessionState.Idle)
    val sessionState: StateFlow<FaceSessionState> = _sessionState.asStateFlow()

    // Called from RegistrationIntroView to capture a face before the review step.
    fun startCapture(
        activity: ComponentActivity,
        useBackCamera: Boolean = false,
        enableSpoofDetection: Boolean = true
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            _sessionState.value = FaceSessionState.Capturing
            try {
                when (val result = FaceCapture.captureFaces(
                    activity,
                    createFaceCaptureConfiguration(activity, useBackCamera, enableSpoofDetection)
                )) {
                    is FaceCaptureSessionResult.Success -> {
                        _capturedFace.value = result.capturedFaces.first()
                        _sessionState.value = FaceSessionState.Idle
                    }
                    is FaceCaptureSessionResult.Failure ->
                        _sessionState.value = FaceSessionState.CaptureError(result.error)
                    is FaceCaptureSessionResult.Cancelled ->
                        _sessionState.value = FaceSessionState.Idle
                }
            } catch (e: Exception) {
                _sessionState.value = FaceSessionState.CaptureError(e)
            }
        }
    }

    // Called from HomeView.
    fun identifyFace(
        activity: ComponentActivity,
        useBackCamera: Boolean = false,
        enableSpoofDetection: Boolean = true,
        identificationThreshold: Float = 0.5f
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            _sessionState.value = FaceSessionState.Capturing
            try {
                when (val captureResult = FaceCapture.captureFaces(
                    activity,
                    createFaceCaptureConfiguration(activity, useBackCamera, enableSpoofDetection)
                )) {
                    is FaceCaptureSessionResult.Success -> {
                        val capturedFace = captureResult.capturedFaces.first()
                        val results = createFaceTemplateRegistry(identificationThreshold) { registry ->
                            registry.identifyFace(capturedFace.face, capturedFace.image)
                        }
                        val userName = results.firstOrNull()?.taggedFaceTemplate?.identifier
                        _sessionState.value = FaceSessionState.IdentificationComplete(userName)
                    }
                    is FaceCaptureSessionResult.Failure ->
                        _sessionState.value = FaceSessionState.CaptureError(captureResult.error)
                    is FaceCaptureSessionResult.Cancelled ->
                        _sessionState.value = FaceSessionState.Idle
                }
            } catch (e: Exception) {
                _sessionState.value = FaceSessionState.CaptureError(e)
            }
        }
    }

    // Called from RegistrationReviewView after the user enters a name.
    fun registerCapturedFace(name: String) {
        val face = _capturedFace.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _sessionState.value = FaceSessionState.Registering
            val faceImage = try { ImageUtils.faceImageFromCapture(face) } catch (_: Exception) { null }
            try {
                val result = createFaceTemplateRegistry { registry ->
                    registry.registerFace(face.face, face.image, name)
                }
                repository.insertFace(result, name, faceImage)
                _capturedFace.value = null
                _sessionState.value = FaceSessionState.RegistrationComplete(name)
            } catch (e: Exception) {
                _sessionState.value = FaceSessionState.RegistrationError(faceImage, e, name)
            }
        }
    }

    // Called from UserView to add a new face to an existing user.
    fun captureAndRegisterFace(
        activity: ComponentActivity,
        userName: String,
        useBackCamera: Boolean = false,
        enableSpoofDetection: Boolean = true
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            _sessionState.value = FaceSessionState.Capturing
            try {
                when (val captureResult = FaceCapture.captureFaces(
                    activity,
                    createFaceCaptureConfiguration(activity, useBackCamera, enableSpoofDetection)
                )) {
                    is FaceCaptureSessionResult.Success -> {
                        val capturedFace = captureResult.capturedFaces.first()
                        _sessionState.value = FaceSessionState.Registering
                        val faceImage = try { ImageUtils.faceImageFromCapture(capturedFace) } catch (_: Exception) { null }
                        try {
                            val result = createFaceTemplateRegistry { registry ->
                                registry.registerFace(capturedFace.face, capturedFace.image, userName)
                            }
                            repository.insertFace(result, userName, faceImage)
                            _sessionState.value = FaceSessionState.RegistrationComplete(userName)
                        } catch (e: Exception) {
                            _sessionState.value = FaceSessionState.RegistrationError(faceImage, e, userName)
                        }
                    }
                    is FaceCaptureSessionResult.Failure ->
                        _sessionState.value = FaceSessionState.CaptureError(captureResult.error)
                    is FaceCaptureSessionResult.Cancelled ->
                        _sessionState.value = FaceSessionState.Idle
                }
            } catch (e: Exception) {
                _sessionState.value = FaceSessionState.CaptureError(e)
            }
        }
    }

    // Force-inserts a face template, bypassing the registry duplicate check.
    // Used from RegistrationErrorDialog when the user overrides a conflict.
    fun forceInsert(
        faceTemplate: FaceTemplate<FaceTemplateVersionR300, FloatArray>,
        userName: String,
        image: Bitmap?
    ) {
        viewModelScope.launch {
            repository.insertFace(faceTemplate, userName, image)
            _capturedFace.value = null
            _sessionState.value = FaceSessionState.Idle
        }
    }

    fun clearState() {
        _sessionState.value = FaceSessionState.Idle
    }

    fun clearCapturedFace() {
        _capturedFace.value = null
        _sessionState.value = FaceSessionState.Idle
    }

    private fun createFaceCaptureConfiguration(
        activity: ComponentActivity,
        useBackCamera: Boolean,
        enableSpoofDetection: Boolean
    ): FaceCaptureConfiguration {
        return FaceCaptureConfiguration(
            settings = FaceCaptureSessionSettings(),
            viewConfiguration = FaceCaptureViewConfiguration(activity, useBackCamera = useBackCamera),
            createFaceDetection = { FaceDetectionRetinaFace.create(activity) },
            createFaceTrackingPlugins = {
                if (enableSpoofDetection) {
                    listOf(LivenessDetectionPlugin(arrayOf(SpoofDeviceDetection(activity))))
                } else {
                    listOf()
                }
            }
        )
    }

    private suspend fun <T> createFaceTemplateRegistry(
        identificationThreshold: Float? = null,
        block: suspend (FaceTemplateRegistry<FaceTemplateVersionR300, FloatArray>) -> T
    ): T {
        val taggedFaces = repository.getAllFaces().first()
        val templates = taggedFaces.map {
            TaggedFaceTemplate(FaceTemplateR300(it.templateData), it.userName)
        }
        return FaceRecognitionR300(getApplication()).use { faceRecognition ->
            val config = FaceTemplateRegistry.Configuration(
                faceRecognition.defaultThreshold,
                identificationThreshold = identificationThreshold ?: faceRecognition.defaultThreshold,
                faceRecognition.defaultThreshold
            )
            FaceTemplateRegistry(faceRecognition, templates, config).use { registry ->
                block(registry)
            }
        }
    }
}
