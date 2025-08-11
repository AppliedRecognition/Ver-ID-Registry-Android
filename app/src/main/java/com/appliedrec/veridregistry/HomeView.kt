package com.appliedrec.veridregistry

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun HomeView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var activity by remember { mutableStateOf<String?>(null) }
    val userCountViewModel = viewModel<UserCountViewModel>()
    val userCount by userCountViewModel.userCount.collectAsStateWithLifecycle()
    val onRegister: () -> Unit = {
        navController.navigate("register")
    }
    val coroutineScope = rememberCoroutineScope()
    val componentActivity = LocalActivity.current as ComponentActivity
    val settingsViewModel: SettingsViewModel = viewModel()
    var error by remember { mutableStateOf<String?>(null) }

    HomeViewContent(
        userCount > 0,
        activity,
        modifier,
        onRegister,
        onIdentify = {
            activity = "Signing in"
            val useBackCamera = settingsViewModel.useBackCamera.value
            val enableSpoofDetection = settingsViewModel.enableSpoofDetection.value
            val identificationThreshold = settingsViewModel.identificationThreshold.value
            coroutineScope.captureAndIdentifyFace(
                activity = componentActivity,
                useBackCamera = useBackCamera,
                enableSpoofDetection = enableSpoofDetection,
                identificationThreshold = identificationThreshold
            ) { result ->
                try {
                    result?.getOrThrow()?.let { identificationResult ->
                        identificationResult.firstOrNull()?.taggedFaceTemplate?.identifier?.let { userName ->
                            navController.navigate("user/${Uri.encode(userName)}")
                        } ?: run {
                            error = "We were unable to indentify you"
                        }
                    }
                } catch (e: Exception) {
                    error = "Identification failed: ${e.localizedMessage}"
                } finally {
                    activity = null
                }
            }
        },
        onSettings = {
            navController.navigate("settings")
        }
    )
    if (error != null) {
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text("Error") },
            confirmButton = {
                TextButton(
                    onClick = { error = null }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeViewContent(
    hasUsers: Boolean,
    activity: String?,
    modifier: Modifier = Modifier,
    onRegister: () -> Unit,
    onIdentify: () -> Unit,
    onSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Ver-ID registry demo")
                },
                actions = {
                    IconButton(
                        onClick = onSettings
                    ) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier.fillMaxSize().padding(paddingValues)
        ) {
            Image(
                painter = painterResource(id = R.drawable.selfie),
                contentDescription = "Hand holding a mobile phone with a photo of a face",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.BottomEnd)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 100.dp)
                    .align(Alignment.Center)
            ) {
                when {
                    activity != null -> {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(activity)
                    }

                    !hasUsers -> {
                        Button(
                            onClick = onRegister
                        ) {
                            Text(
                                text = "Register",
                                fontSize = 20.sp
                            )
                        }
                    }

                    else -> {
                        Button(
                            onClick = onIdentify,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = "Sign in",
                                fontSize = 20.sp
                            )
                        }
                        TextButton(onClick = onRegister) {
                            Text(
                                text = "Register",
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ContentViewPreview() {
    val context = LocalContext.current
    HomeViewContent(
        true,
        null,
        Modifier,
        {},
        {},
        {}
    )
}