package com.appliedrec.veridregistry

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.navigation.NavController
import java.util.Date

@Composable
fun UsersView(
    navController: NavController
) {
    val userFacesViewModel: UsersViewModel = viewModel()
    val taggedFaces by userFacesViewModel.users.collectAsStateWithLifecycle()
    val context = LocalContext.current.applicationContext
    var userToDelete by remember { mutableStateOf<String?>(null) }
    UsersViewContent(
        taggedFaces = taggedFaces,
        onDelete = { name -> userToDelete = name },
        faceImagePainter = { faceId ->
            ImageUtils.getFaceImage(context, faceId)?.asImageBitmap()?.let { bitmap ->
                BitmapPainter(bitmap)
            }
        }
    ) { name ->
        navController.navigate("user/${Uri.encode(name)}?editable=false")
    }
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                userToDelete = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        userToDelete?.let { userName ->
                            userFacesViewModel.deleteUser(userName)
                        }
                        userToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        userToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            title = {
                Text("Delete $userToDelete?")
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsersViewContent(
    taggedFaces: List<TaggedFaceEntity>,
    onDelete: (String) -> Unit,
    faceImagePainter: @Composable (Long) -> Painter?,
    onSelect: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Users") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
        ) {
            items(items = taggedFaces, key = { it.id }) { face ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onDelete(face.userName)
                            false
                        } else {
                            false
                        }
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromEndToStart = true,
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
                            .clickable {
                                onSelect(face.userName)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        faceImagePainter(face.id)?.let { painter ->
                            Image(
                                painter = painter,
                                contentDescription = "Image of ${face.userName}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = face.userName,
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

@Preview
@Composable
fun UsersViewPreview() {
    UsersViewContent(
        listOf(
            TaggedFaceEntity(
                id = 1,
                dateAdded = Date(),
                userName = "Happy Koala",
                templateData = floatArrayOf()
            ),
            TaggedFaceEntity(
                id = 2,
                dateAdded = Date(),
                userName = "Grumpy Giraffe",
                templateData = floatArrayOf()
            )
        ),
        onDelete = {},
        faceImagePainter = { rememberVectorPainter(Icons.Default.Person) }
    )
}