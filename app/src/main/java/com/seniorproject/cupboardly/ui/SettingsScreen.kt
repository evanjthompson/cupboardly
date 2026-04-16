package com.seniorproject.cupboardly.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seniorproject.cupboardly.R
import com.seniorproject.cupboardly.room.database.AppDatabase
import kotlinx.coroutines.launch
import java.io.File

val dullrecipeBlue = Color(101, 154, 166)
val dullingredientGold = Color(179, 133, 43)

@Composable
fun SettingsScreen(
    context: Context = LocalContext.current,
    onGoToIngredients: () -> Unit,
    onGoToRecipes: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val dbName = "AppDatabase"

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

                Toast.makeText(context, "Import successful. Restarting...", Toast.LENGTH_LONG)
                    .show()

                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
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
            contentDescription = "Ingredient Background",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- Navigation Tab Row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onGoToIngredients,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = dullingredientGold,
                        contentColor = Color.White
                    )
                ) {
                    Text("Ingredients", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onGoToRecipes,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = dullrecipeBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("Recipes", fontSize = 18.sp)
                }
                Button(
                    onClick = {},
                    modifier = Modifier.weight(.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.DarkGray,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderCopy,
                        contentDescription = "Settings"
                    )
                }
            }

            // --- Database Settings Content ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Database Backup", style = MaterialTheme.typography.headlineSmall)

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
                    Text("Export Database")
                }

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            type = "application/octet-stream"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        importLauncher.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Import Database")
                }

                Text(
                    text = "⚠ Import will overwrite current data and restart the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}