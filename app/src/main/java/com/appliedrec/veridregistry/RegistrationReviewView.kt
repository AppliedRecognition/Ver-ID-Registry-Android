@file:Suppress("UNCHECKED_CAST")

package com.appliedrec.veridregistry

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.appliedrec.verid3.common.serialization.toBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationReviewView(
    navController: NavController
) {
    val context = LocalContext.current
    val capturedFaceViewModel: CapturedFaceViewModel = viewModel(context as ComponentActivity)
    val coroutineScope = rememberCoroutineScope()
    var error by remember { mutableStateOf<Throwable?>(null) }
    var enteredName: String by remember { mutableStateOf("") }
    var capturedFaceImage by remember { mutableStateOf<Bitmap?>(null) }
    var isRegistering by remember { mutableStateOf(false) }
    val initialName = remember { RandomNameGenerator.generateRandomName() }
    var refocusKey by remember { mutableStateOf(0) }
    if (isRegistering) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 100.dp)
                    .align(Alignment.Center)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text("Registering")
            }
        }
    } else {
        capturedFaceViewModel.capturedFace?.let { capturedFace ->
            RegistrationReviewContent(
                capturedFace.image.toBitmap(),
                initialName,
                refocusKey
            ) { name ->
                isRegistering = true
                coroutineScope.registerFace(
                    context.applicationContext,
                    capturedFace,
                    name
                ) { result ->
                    isRegistering = false
                    if (result.isSuccess) {
                        capturedFaceViewModel.capturedFace = null
                        navController.navigate("user/${Uri.encode(name)}") {
                            popUpTo("home") {
                                inclusive = false
                            }
                        }
                    } else {
                        enteredName = name
                        capturedFaceImage = capturedFace.image.toBitmap()
                        error = result.exceptionOrNull()
                    }
                }
            }
        } ?: run {
            Text("Failed to retrieve captured face")
        }
    }
    if (error != null) {
        RegistrationErrorDialog(
            error = error!!,
            enteredName = enteredName,
            capturedFaceImage = capturedFaceImage,
            onNavigate = { name ->
                error = null
                navController.navigate("user/${Uri.encode(name)}") {
                    popUpTo("home") {
                        inclusive = false
                    }
                }
            },
            onDismiss = {
                error = null
                refocusKey ++
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun RegistrationReviewContent(
    image: Bitmap,
    initialName: String,
    refocusKey: Int,
    onSubmit: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val trimmedName by remember { derivedStateOf { name.trim() } }
    val canSubmit by remember { derivedStateOf { trimmedName.isNotEmpty() } }

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(refocusKey) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                title = {},
                actions = {
                    Button(
                        onClick = { if (canSubmit) onSubmit(trimmedName) },
                        enabled = canSubmit
                    ) { Text("Save") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.Top
        ) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text("Your name", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Enter your name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (canSubmit) onSubmit(trimmedName) }
                    )
                )
            }
        }
    }
}

@Preview
@Composable
fun RegistrationViewPreview() {
    val sizePx = 800
    val imageBitmap = ImageBitmap(sizePx, sizePx)
    val canvas = Canvas(imageBitmap)
    val drawScope = CanvasDrawScope()
    val painter = rememberVectorPainter(Icons.Filled.Person)

    // Invoke draw with a proper DrawScope
    drawScope.draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = Size(sizePx.toFloat(), sizePx.toFloat())
    ) {
        with(painter) {
            draw(size = Size(sizePx.toFloat(), sizePx.toFloat()))
        }
    }
    val bitmap = imageBitmap.asAndroidBitmap()
    RegistrationReviewContent(
        bitmap,
        RandomNameGenerator.generateRandomName(),
        refocusKey = 0
    ) { }
}