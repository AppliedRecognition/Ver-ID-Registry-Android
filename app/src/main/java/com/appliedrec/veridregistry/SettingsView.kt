package com.appliedrec.veridregistry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.util.Locale
import kotlin.math.round

@Composable
fun SettingsView(
    navController: NavController
) {
    val userCountViewModel: UserCountViewModel = viewModel()
    val userCount by userCountViewModel.userCount.collectAsStateWithLifecycle()
    val settingsViewModel: SettingsViewModel = viewModel()
    val useBackCamera by settingsViewModel.useBackCamera.collectAsStateWithLifecycle()
    val enableSpoofDetection by settingsViewModel.enableSpoofDetection.collectAsStateWithLifecycle()
    val identificationThreshold by settingsViewModel.identificationThreshold.collectAsStateWithLifecycle()
    var showConfirmDialog by remember { mutableStateOf(false) }

    fun reset() {
        settingsViewModel.reset()
        userCountViewModel.deleteAll()
    }

    SettingsViewContent(
        userCount = userCount,
        useBackCamera = useBackCamera,
        enableSpoofDetection = enableSpoofDetection,
        identificationThreshold = identificationThreshold,
        onUseBackCameraChanged = {
            settingsViewModel.setUseBackCamera(it)
        },
        onEnableSpoofDetectionChanged = {
            settingsViewModel.setEnableSpoofDetection(it)
        },
        onShowUsers = {
            navController.navigate("users")
        },
        onIdentificationThresholdChanged = {
            settingsViewModel.setIdentificationThreshold(it)
        },
        onReset = {
            if (userCount > 0) {
                showConfirmDialog = true
            } else {
                reset()
            }
        }
    )
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Reset") },
            text = { Text("This will also delete all registered face templates.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        reset()
                    }
                ) { Text("Proceed anyway", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsViewContent(
    userCount: Int,
    useBackCamera: Boolean,
    enableSpoofDetection: Boolean,
    identificationThreshold: Float,
    onUseBackCameraChanged: (Boolean) -> Unit,
    onEnableSpoofDetectionChanged: (Boolean) -> Unit,
    onShowUsers: () -> Unit,
    onIdentificationThresholdChanged: (Float) -> Unit,
    onReset: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                actions = {
                    TextButton(
                        onClick = onReset
                    ) {
                        Text("Reset")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues
        ) {
            item {
                SectionHeader("About the app")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("This app shows how to use Ver-ID SDK to build a biometric sign-in system.")
                    Text("The app captures a face and registers it under a user's name. Once registered, the user can sign in to the app using their face.")
                }
                HorizontalDivider()
            }
            item {
                SectionHeader("Registration")
                ListItem(
                    headlineContent = {
                        Text(
                            when {
                                userCount == 0 -> "No registered users"
                                userCount == 1 -> "1 registered user"
                                else -> "$userCount registered users"
                            },
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    },
                    trailingContent = {
                        if (userCount > 0) Icon(Icons.Filled.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = userCount > 0) { onShowUsers() }
                        .padding(horizontal = 8.dp)
                )
                HorizontalDivider()
            }
            item {
                SectionHeader("Face capture")
                SettingToggleRow(
                    title = "Use back camera",
                    checked = useBackCamera,
                    onCheckedChange = onUseBackCameraChanged
                )
                SettingToggleRow(
                    title = "Enable spoof detection",
                    checked = enableSpoofDetection,
                    onCheckedChange = onEnableSpoofDetectionChanged
                )
                HorizontalDivider()
            }
            item {
                SectionHeader("Face recognition")
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Identification threshold", modifier = Modifier.weight(1f))
                        Text(String.format(Locale.ROOT, "%.1f", identificationThreshold))
                    }
                    Spacer(Modifier.height(8.dp))
                    var sliderValue by remember { mutableStateOf(identificationThreshold.coerceIn(0.1f, 0.9f)) }
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 0.1f..0.9f,
                        steps = 7,
                        onValueChangeFinished = {
                            val snapped = (round(sliderValue * 10f) / 10f).toFloat().coerceIn(0.1f, 0.9f)
                            sliderValue = snapped
                            if (snapped != identificationThreshold) onIdentificationThresholdChanged(snapped)
                        }
                    )
                }
                SectionFooter("The threshold will be used when comparing faces. Higher threshold makes the comparison stricter.")
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun SectionFooter(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
    HorizontalDivider()
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Preview
@Composable
fun SettingsViewPreview() {
    SettingsViewContent(
        userCount = 1,
        useBackCamera = true,
        enableSpoofDetection = true,
        identificationThreshold = 0.5f,
        onUseBackCameraChanged = { },
        onEnableSpoofDetectionChanged = { },
        onShowUsers = { },
        onIdentificationThresholdChanged = {},
        onReset = {}
    )
}