package com.appliedrec.veridregistry

import android.graphics.Bitmap
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
import androidx.compose.ui.unit.dp
import com.appliedrec.facerecognition.r300.core.FaceTemplateVersionR300
import com.appliedrec.verid3.common.FaceTemplate
import com.appliedrec.verid3.facetemplateregistry.FaceTemplateRegistryException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationErrorDialog(
    error: Throwable,
    enteredName: String,
    capturedFaceImage: Bitmap?,
    onSaveAsUser: (faceTemplate: FaceTemplate<FaceTemplateVersionR300, FloatArray>, userName: String) -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
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
                Text("${error.localizedMessage}")
                @Suppress("UNCHECKED_CAST")
                when (error) {
                    is FaceTemplateRegistryException.SimilarFaceAlreadyRegistered -> {
                        TextButton(
                            onClick = {
                                onSaveAsUser(
                                    error.faceTemplate as FaceTemplate<FaceTemplateVersionR300, FloatArray>,
                                    error.registeredIdentifier
                                )
                            }
                        ) {
                            Text("Add face to ${error.registeredIdentifier}")
                        }
                        TextButton(
                            onClick = {
                                onSaveAsUser(
                                    error.faceTemplate as FaceTemplate<FaceTemplateVersionR300, FloatArray>,
                                    enteredName
                                )
                            }
                        ) {
                            Text("Save as $enteredName anyway")
                        }
                    }
                    is FaceTemplateRegistryException.FaceDoesNotMatchExisting -> {
                        TextButton(
                            onClick = {
                                onSaveAsUser(
                                    error.faceTemplate as FaceTemplate<FaceTemplateVersionR300, FloatArray>,
                                    enteredName
                                )
                            }
                        ) {
                            Text("Save anyway")
                        }
                    }
                    else -> {}
                }
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    }
}
