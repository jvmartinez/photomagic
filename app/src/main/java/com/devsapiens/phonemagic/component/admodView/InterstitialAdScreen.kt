package com.devsapiens.galaxylu.components.admodView

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.devsapiens.phonemagic.component.button.ButtonComponent
import com.devsapiens.phonemagic.component.button.ConfigButton
import com.devsapiens.phonemagic.core.adMod.AdMobViewModel


@Composable
fun InterstitialAdScreen(
    viewModel: AdMobViewModel,
    navigateToNextScreen: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val adState by viewModel.adState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadInterstitialAd(context)
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            when (adState) {
                is AdMobViewModel.AdState.Loading -> {
                    CircularProgressIndicator()
                }

                is AdMobViewModel.AdState.Loaded -> {
                    activity?.let {
                        viewModel.showInterstitialAd(it)
                    }
                }

                is AdMobViewModel.AdState.Failed -> {
                    ButtonComponent(
                        config = ConfigButton(
                            title = "Seleccionar imagen",
                            onClick = {
                                viewModel.loadInterstitialAd(context)
                            }
                        )
                    )
                }

                is AdMobViewModel.AdState.Dismissed -> {
                    navigateToNextScreen()
                }

                else -> {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
