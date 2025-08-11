package com.appliedrec.veridregistry

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appliedrec.verid3.common.FaceTemplate
import com.appliedrec.verid3.facerecognition.arcface.core.FaceTemplateVersionV24
import com.appliedrec.verid3.facetemplateregistry.FaceTemplateRegistryException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationErrorDialog(
    error: Throwable,
    enteredName: String,
    capturedFaceImage: Bitmap?,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val facesViewModel: TaggedFacesViewModel = viewModel(context as ComponentActivity)
    val capturedFaceViewModel: CapturedFaceViewModel = viewModel(context as ComponentActivity)
    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Registration failed", modifier = Modifier.padding(bottom = 16.dp))
                Text("${error!!.localizedMessage}")
                when (error) {
                    is FaceTemplateRegistryException.SimilarFaceAlreadyRegistered -> {
                        val err = error as FaceTemplateRegistryException.SimilarFaceAlreadyRegistered
                        TextButton(
                            onClick = {
                                facesViewModel.insert(
                                    err.faceTemplate as FaceTemplate<FaceTemplateVersionV24, FloatArray>,
                                    err.registeredIdentifier,
                                    capturedFaceImage
                                )
                                capturedFaceViewModel.capturedFace = null
                                onNavigate(err.registeredIdentifier)
                            }
                        ) {
                            Text("Add face to ${err.registeredIdentifier}")
                        }
                        TextButton(
                            onClick = {
                                facesViewModel.insert(
                                    err.faceTemplate as FaceTemplate<FaceTemplateVersionV24, FloatArray>,
                                    enteredName,
                                    capturedFaceImage
                                )
                                capturedFaceViewModel.capturedFace = null
                                onNavigate(enteredName)
                            }
                        ) {
                            Text("Save as $enteredName anyway")
                        }
                    }
                    is FaceTemplateRegistryException.FaceDoesNotMatchExisting -> {
                        val err = error as FaceTemplateRegistryException.FaceDoesNotMatchExisting
                        TextButton(
                            onClick = {
                                facesViewModel.insert(
                                    err.faceTemplate as FaceTemplate<FaceTemplateVersionV24, FloatArray>,
                                    enteredName,
                                    capturedFaceImage
                                )
                                capturedFaceViewModel.capturedFace = null
                                onNavigate(enteredName)
                            }
                        ) {
                            Text("Save anyway")
                        }
                    }
                    else -> {}
                }
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Dismiss")
                }
            }
        }
    }
}