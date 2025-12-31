package com.devsapiens.phonemagic.ui.home

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.devsapiens.phonemagic.ui.theme.Navy800
import com.devsapiens.phonemagic.ui.theme.Navy700
import com.devsapiens.phonemagic.ui.theme.Primary
import com.devsapiens.phonemagic.ui.theme.Secondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onImageSelected: (Uri?) -> Unit) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            selectedUri = uri
            onImageSelected(uri)
        }
    )

    // Permission launcher for multiple permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { perms ->
            val allGranted = perms.values.all { it }
            if (allGranted) {
                pickImage.launch("image/*")
            } else {
                Toast.makeText(context, "Permissions denied. Can't open gallery.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    fun hasPermission(ctx: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(ctx, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun requestRequiredPermissions() {
        val perms = mutableListOf<String>()
        // Always ask camera permission as well (app may use camera later)
        perms.add(Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        // Filter already granted
        val toRequest = perms.filter { !hasPermission(context, it) }.toTypedArray()
        if (toRequest.isEmpty()) {
            // already granted
            pickImage.launch("image/*")
        } else {
            permissionLauncher.launch(toRequest)
        }
    }

    val gradient = Brush.verticalGradient(listOf(Navy800, Navy700))

    Scaffold(topBar = { TopAppBar(title = { Text("PhoneMagic") }) }, content = { inner ->
        Box(modifier = Modifier
            .fillMaxSize()
            .background(brush = gradient)
            .padding(inner)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .size(320.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.06f)
                ) {
                    if (selectedUri != null) {
                        AsyncImage(model = selectedUri, contentDescription = "Preview", modifier = Modifier.size(320.dp))
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text("No image selected", color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { requestRequiredPermissions() },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Elegir imagen", color = Color.White)
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { onImageSelected(selectedUri) },
                        colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Editar", color = Color.Black)
                    }
                }
            }
        }
    })
}
