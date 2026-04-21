package com.seniorproject.cupboardly.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seniorproject.cupboardly.R
import com.seniorproject.cupboardly.room.database.AppDatabase
import kotlinx.coroutines.launch
import java.io.File

val ingredientGold = Color(162, 119, 0)
val recipeBlue = Color(91, 177, 184)

@Composable
fun SettingsScreen(
    currentScreen: String,
    context: Context = LocalContext.current,
    onGoToIngredients: () -> Unit,
    onGoToRecipes: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val dbName = "AppDatabase"

    var showImportDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val cursor = db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")
                cursor.moveToFirst()
                cursor.close()

                val dbFile = context.getDatabasePath(dbName)
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    dbFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }

                Toast.makeText(context, "Export successful", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult

        scope.launch {
            try {
                AppDatabase.getDatabase(context).close()

                val dbFile = context.getDatabasePath(dbName)

                val walFile = File(dbFile.path + "-wal")
                val shmFile = File(dbFile.path + "-shm")
                if (walFile.exists()) walFile.delete()
                if (shmFile.exists()) shmFile.delete()

                context.contentResolver.openInputStream(uri)?.use { input ->
                    dbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                Toast.makeText(
                    context,
                    "Import successful. Reloading...",
                    Toast.LENGTH_LONG
                ).show()

                val intent = context.packageManager
                    .getLaunchIntentForPackage(context.packageName)

                intent?.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                )

                context.startActivity(intent)
                Runtime.getRuntime().exit(0)

            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        // Background
        Image(
            painter = painterResource(id = R.drawable.stripesettingsbg),
            contentDescription = "Settings Background",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            TopNavTabs(
                currentScreen = currentScreen,
                onGoToIngredients = onGoToIngredients,
                onGoToRecipes = onGoToRecipes,
                ingredientGold = ingredientGold,
                recipeBlue = recipeBlue
            )

            // --- Backup Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Text(
                        "Manage Data",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                type = "application/octet-stream"
                                putExtra(Intent.EXTRA_TITLE, "cupboardly_backup.db")
                            }
                            exportLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Export Data")
                    }

                    Button(
                        onClick = {
                            showImportDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Import Data")
                    }

                    // Warning block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "⚠ Import will overwrite current ingredient and recipe data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // --- IMPORT CONFIRMATION DIALOG ---
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = {
                    Text("Confirm Import")
                },
                text = {
                    Text(
                        "⚠ This will permanently replace all current ingredient and recipe data.\n\n" +
                                "This action cannot be undone.\n\n" +
                                "Do you want to continue?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showImportDialog = false

                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                type = "application/octet-stream"
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                            importLauncher.launch(intent)
                        }
                    ) {
                        Text(
                            "Import",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showImportDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}