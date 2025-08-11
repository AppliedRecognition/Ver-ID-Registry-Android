package com.appliedrec.veridregistry

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date

private val dateFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        .withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserView(
    userName: String,
    editable: Boolean = true
) {
    val application = LocalContext.current.applicationContext as Application
    val userFacesViewModel: UserFacesViewModel = viewModel(
        factory = UserFacesViewModelFactory(application, userName)
    )
    val userFaces by userFacesViewModel.taggedFaces.collectAsStateWithLifecycle()
    var faceToDelete by remember { mutableStateOf<Long?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val activity = LocalActivity.current as ComponentActivity
    val settingsViewModel: SettingsViewModel = viewModel()
    var isRegistering by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var capturedFaceImage by remember { mutableStateOf<Bitmap?>(null) }
    if (isRegistering) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = userName)
                    },
                    actions = {
                        IconButton(
                            onClick = {},
                            enabled = false
                        ) {
                            Icon(Icons.Filled.Add, "Register new face")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
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
        }
    } else {
        val onAddFace = if (editable) {
            {
                val useBackCamera = settingsViewModel.useBackCamera.value
                val enableSpoofDetection = settingsViewModel.enableSpoofDetection.value
                isRegistering = true
                coroutineScope.captureAndRegisterFace(
                    activity,
                    userName,
                    useBackCamera,
                    enableSpoofDetection
                ) { captureResult, image ->
                    isRegistering = false
                    capturedFaceImage = image
                    error = captureResult?.exceptionOrNull()
                }
            }
        } else null
        val onDelete: ((Long) -> Unit)? = if (editable) {
            { faceId ->
                faceToDelete = faceId
            }
        } else null
        UserViewContent(
            userName = userName,
            faces = userFaces,
            onDelete = onDelete,
            onAddFace = onAddFace,
            faceImagePainter = { faceId ->
                ImageUtils.getFaceImage(application, faceId)?.asImageBitmap()?.let { bitmap ->
                    BitmapPainter(bitmap)
                }
            }
        )
    }
    if (faceToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                faceToDelete = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        faceToDelete?.let { faceId ->
                            userFacesViewModel.deleteFace(faceId)
                        }
                        faceToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        faceToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            title = {
                Text("Delete face?")
            }
        )
    }
    if (error != null) {
        RegistrationErrorDialog(
            error = error!!,
            enteredName = userName,
            capturedFaceImage = capturedFaceImage,
            onNavigate = { name ->
                error = null
            },
            onDismiss = { error = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun UserViewContent(
    userName: String,
    faces: List<TaggedFaceEntity>,
    onDelete: ((Long) -> Unit)? = null,
    onAddFace: (() -> Unit)? = null,
    faceImagePainter: @Composable (Long) -> Painter?
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent),
        endY = 250f
    )
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                title = {
                    Text(
                        text =userName,
                        color = Color.White
                    )
                },
                actions = {
                    onAddFace?.let { onAdd ->
                        IconButton(
                            onClick = onAdd
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Register new face",
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .background(gradientBrush),
            verticalArrangement = Arrangement.Top
        ) {
            LazyColumn {
                item(key = "header") {
                    faces.firstOrNull()?.id?.let { faceId ->
                        faceImagePainter(faceId)?.let { painter ->
                            Image(
                                painter = painter,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
                items(items = faces, key = { it.id }) { face ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                onDelete?.let { it(face.id) }
                                false
                            } else {
                                false
                            }
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromEndToStart = onDelete != null,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Red)
                                    .padding(horizontal = 32.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White
                                )
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            faceImagePainter(face.id)?.let { painter ->
                                Image(
                                    painter = painter,
                                    contentDescription = "Image of face",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Face added on ${dateFormatter.format(face.dateAdded.toInstant())}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview
@Composable
fun UserViewPreview() {
    UserViewContent(
        userName = "Friendly Koala",
        faces = listOf(TaggedFaceEntity(1, Date(), floatArrayOf(), "Test")),
        onDelete = {},
        onAddFace = {},
        faceImagePainter = { rememberVectorPainter(Icons.Default.Person) }
    )
}