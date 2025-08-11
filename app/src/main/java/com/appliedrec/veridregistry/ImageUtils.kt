package com.appliedrec.veridregistry

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import com.appliedrec.verid3.common.Face
import com.appliedrec.verid3.common.IImage
import com.appliedrec.verid3.common.serialization.toBitmap
import com.appliedrec.verid3.facecapture.CapturedFace
import java.io.File
import kotlin.math.max
import kotlin.math.min

object ImageUtils {

    fun faceImageFromCapture(capturedFace: CapturedFace): Bitmap {
        val image = capturedFace.image.toBitmap()
        val faceBounds = Rect()
        capturedFace.face.bounds.round(faceBounds)
        val longerDim = max(faceBounds.width(), faceBounds.height())
        val cropRect = Rect(
            faceBounds.centerX() - longerDim / 2,
            faceBounds.centerY() - longerDim / 2,
            faceBounds.centerX() + longerDim / 2,
            faceBounds.centerY() + longerDim / 2
        )
        return Bitmap.createBitmap(
            image,
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height()
        )
    }

    fun cropImageToFace(image: IImage, face: Face): Bitmap {
        val faceBounds = Rect()
        face.bounds.round(faceBounds)
        val minX = min(faceBounds.centerX(), image.width - faceBounds.centerX())
        val minY = min(faceBounds.centerY(), image.height - faceBounds.centerY())
        val minDistanceToEdge = min(minX, minY)
        val cropRect = Rect(
            faceBounds.centerX() - minDistanceToEdge,
            faceBounds.centerY() - minDistanceToEdge,
            faceBounds.centerX() + minDistanceToEdge,
            faceBounds.centerY() + minDistanceToEdge
        )
        return Bitmap.createBitmap(
            image.toBitmap(),
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height()
        )
    }

    private fun getImagesDir(context: Context): File {
        val imagesDir = File(context.filesDir, "images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        return imagesDir
    }

    private fun getFaceImageFile(context: Context, templateId: Long): File {
        return File(getImagesDir(context), "$templateId.jpg")
    }

    fun saveFaceImage(context: Context, image: Bitmap, templateId: Long) {
        getFaceImageFile(context, templateId).outputStream().use { out ->
            image.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
    }

    fun getFaceImage(context: Context, templateId: Long): Bitmap? {
        val file = getFaceImageFile(context, templateId)
        if (file.exists()) {
            return file.inputStream().use {
                BitmapFactory.decodeStream(it)
            }
        }
        return null
    }

    fun deleteFaceImage(context: Context, templateId: Long) {
        try {
            getFaceImageFile(context, templateId).delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}