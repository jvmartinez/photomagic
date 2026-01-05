package com.devsapiens.galaxylu.components.admodView

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.devsapiens.phonemagic.extensions.toGetHeightInPixels
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String,
    adSize: AdSize = AdSize.BANNER
) {
    val context = LocalContext.current
    var adView by remember { mutableStateOf<AdView?>(null) }
    var isAdLoaded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adSize.toGetHeightInPixels(context).dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(adSize)
                    this.adUnitId = adUnitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            Log.d("AdMob", "Anuncio cargado exitosamente")
                            isAdLoaded = true
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Log.e("AdMob", "Error cargando anuncio: ${adError.message}")
                            isAdLoaded = false
                        }

                        override fun onAdOpened() {
                            Log.d("AdMob", "Anuncio abierto")
                        }

                        override fun onAdClicked() {
                            Log.d("AdMob", "Anuncio clickeado")
                        }

                        override fun onAdClosed() {
                            Log.d("AdMob", "Anuncio cerrado")
                        }
                    }
                    adView = this
                }
            },
            update = { view ->
                if (!isAdLoaded) {
                    view.loadAd(AdRequest.Builder().build())
                }
            }
        )
        if (!isAdLoaded) {
            CircularProgressIndicator()
        }
    }
}