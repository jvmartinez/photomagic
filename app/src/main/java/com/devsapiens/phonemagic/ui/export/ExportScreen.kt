@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.devsapiens.phonemagic.ui.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devsapiens.phonemagic.viewmodel.EditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.CircularProgressIndicator

@Composable
fun ExportScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val vm: EditorViewModel = viewModel()
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Exportar") }) }) { inner ->
        Column(modifier = Modifier
            .padding(inner)
            .fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Vista previa final")
            if (exporting) {
                CircularProgressIndicator()
                Button(onClick = {}, enabled = false, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Guardando...")
                }
            } else {
                Button(onClick = {
                    // Export and save in background
                    exporting = true
                    scope.launch {
                        val bmp = withContext(Dispatchers.Default) { vm.exportBitmap() }
                        bmp?.let { saved ->
                            val uri = saveBitmapToMediaStore(ctx, saved)
                            exporting = false
                            onDone()
                        } ?: run {
                            exporting = false
                            onDone()
                        }
                    }
                }, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Guardar y regresar")
                }
            }
        }
    }
}

private fun saveBitmapToMediaStore(context: Context, bitmap: android.graphics.Bitmap): android.net.Uri? {
    val filename = "phonemagic_${System.currentTimeMillis()}.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhoneMagic")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val item = resolver.insert(collection, contentValues) ?: return null

    resolver.openOutputStream(item).use { out ->
        out?.let { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, it) }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(item, contentValues, null, null)
    }
    return item
}
