package com.appliedrec.veridregistry

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun RegistrationIntroView(navController: NavController) {
    val activity = LocalActivity.current as ComponentActivity
    val faceSessionViewModel: FaceSessionViewModel = viewModel(activity)
    val settingsViewModel: SettingsViewModel = viewModel()
    val capturedFace by faceSessionViewModel.capturedFace.collectAsStateWithLifecycle()
    val sessionState by faceSessionViewModel.sessionState.collectAsStateWithLifecycle()
    var captureError by remember { mutableStateOf<String?>(null) }

    // Navigate to review as soon as a face is captured; pop this screen off the
    // back stack so pressing Back from review goes straight to Home.
    LaunchedEffect(capturedFace) {
        if (capturedFace != null) {
            navController.navigate("registration_review") {
                popUpTo("register") { inclusive = true }
            }
        }
    }

    LaunchedEffect(sessionState) {
        if (sessionState is FaceSessionState.CaptureError) {
            captureError = "Face capture failed: ${(sessionState as FaceSessionState.CaptureError).error.localizedMessage}"
            faceSessionViewModel.clearState()
        }
    }

    RegistrationIntroViewContent {
        faceSessionViewModel.startCapture(
            activity = activity,
            useBackCamera = settingsViewModel.useBackCamera.value,
            enableSpoofDetection = settingsViewModel.enableSpoofDetection.value
        )
    }

    if (captureError != null) {
        AlertDialog(
            onDismissRequest = { captureError = null },
            confirmButton = {
                TextButton(onClick = { captureError = null }) { Text("OK") }
            },
            title = { Text("Error") },
            text = { Text(captureError!!) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationIntroViewContent(onStartCapture: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Registration") })
        }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(paddingValues).padding(horizontal = 16.dp)
        ) {
            Text(
                text = "The app will capture your face and generate a biometric face template.",
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "The face template will then be used to sign you in to the app.",
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(onClick = onStartCapture) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Capture face",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Capture face")
            }
        }
    }
}

@Preview
@Composable
fun RegistrationIntroViewPreview() {
    RegistrationIntroViewContent { }
}
