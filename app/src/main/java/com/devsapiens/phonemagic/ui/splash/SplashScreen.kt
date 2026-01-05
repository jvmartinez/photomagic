package com.devsapiens.phonemagic.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.devsapiens.phonemagic.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    text: String = "Magic mau",
    onTimeout: () -> Unit
) {
    var revealed by remember { mutableStateOf(0) }
    val scale by animateFloatAsState(targetValue = if (revealed > 0) 1f else 0.9f)

    LaunchedEffect(Unit) {
        while (revealed < text.length) {
            revealed++
            delay(150)
        }
        delay(1000)
        onTimeout()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo_spash),
            contentDescription = "Logo",
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale }
        )

        Text(
            text = text.take(revealed),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .graphicsLayer { alpha = (revealed.toFloat() / text.length).coerceIn(0f, 1f) }
        )
    }
}

