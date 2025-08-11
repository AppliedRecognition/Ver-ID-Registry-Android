package com.appliedrec.veridregistry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.appliedrec.verid3.facecapture.CapturedFace

class CapturedFaceViewModel : ViewModel() {
    var capturedFace by mutableStateOf<CapturedFace?>(null)
}