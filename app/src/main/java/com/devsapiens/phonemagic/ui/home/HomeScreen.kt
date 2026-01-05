package com.devsapiens.phonemagic.ui.home

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.devsapiens.galaxylu.components.admodView.AdMobBanner
import com.devsapiens.phonemagic.BuildConfig
import com.devsapiens.phonemagic.R
import com.devsapiens.phonemagic.component.button.ButtonComponent
import com.devsapiens.phonemagic.component.button.ConfigButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onImageSelected: (Uri?) -> Unit
) {
    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            onImageSelected(uri)
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { perms ->
            val allGranted = perms.values.all { it }
            if (allGranted) {
                pickImage.launch("image/*")
            } else {
                Toast.makeText(
                    context,
                    "Permissions denied. Can't open gallery.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )

    fun hasPermission(ctx: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx,
            permission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun requestRequiredPermissions() {
        val perms = mutableListOf<String>()
        perms.add(Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val toRequest = perms.filter {
            !hasPermission(context, it)
        }.toTypedArray()
        if (toRequest.isEmpty()) {
            pickImage.launch("image/*")
        } else {
            permissionLauncher.launch(toRequest)
        }
    }

    Scaffold(
        content = { inner ->
            Box(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .background(White)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_spash),
                    contentDescription = "Background",
                    modifier = Modifier.fillMaxSize()
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.weight(1f))
                    ButtonComponent(
                        config = ConfigButton(
                            title = "Seleccionar imagen",
                            onClick = {
                                requestRequiredPermissions()
                            }
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    AdMobBanner(
                        modifier = Modifier.fillMaxWidth(),
                        adUnitId = BuildConfig.adHomeBanner
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    )
}


@Preview
@Composable
fun HomeScreenPreview() {
    HomeScreen(onImageSelected = {})
}