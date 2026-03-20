package com.seniorproject.cupboardly.classes

import android.net.Uri
import android.os.Environment
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import java.io.File
import androidx.appcompat.app.AppCompatActivity

class Camera(private val activity: AppCompatActivity) {

    private lateinit var photoUri: Uri

    private val cameraLauncher = activity.registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            // You need a callback here instead of imageView
        }
    }

    fun openCamera() {
        val file = File(
            activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "photo.jpg"
        )

        photoUri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.provider",
            file
        )

        cameraLauncher.launch(photoUri)
    }
}