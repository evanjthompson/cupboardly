package com.seniorproject.cupboardly.classes

import android.net.Uri
import android.os.Environment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import androidx.appcompat.app.AppCompatActivity

class Camera(
    private val activity: AppCompatActivity,
    private val onPhotoTaken: (Uri) -> Unit
) {

    private lateinit var photoUri: Uri

    private val cameraLauncher = activity.registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            onPhotoTaken(photoUri)
        }
    }

    fun openCamera() {
        val file = File(
            activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "photo.jpg"
        )

        photoUri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider", // ✅ matches manifest
            file
        )

        cameraLauncher.launch(photoUri)
    }
}