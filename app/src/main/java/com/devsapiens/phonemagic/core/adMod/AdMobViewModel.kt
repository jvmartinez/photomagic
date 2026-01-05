package com.devsapiens.phonemagic.core.adMod

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsapiens.phonemagic.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdMobViewModel : ViewModel() {

    private val _adState = MutableStateFlow<AdState>(AdState.NotLoaded)
    val adState: StateFlow<AdState> = _adState

    private var interstitialAd: InterstitialAd? = null
    private val adUnitId = BuildConfig.entryScreenIntersticial

    sealed class AdState {
        object NotLoaded : AdState()
        object Loading : AdState()
        object Loaded : AdState()
        object Showing : AdState()
        object Failed : AdState()
        object Dismissed : AdState()
    }

    fun loadInterstitialAd(context: Context) {
        viewModelScope.launch {
            _adState.value = AdState.Loading

            val adRequest = AdRequest.Builder().build()

            InterstitialAd.load(
                context,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        _adState.value = AdState.Loaded

                        // Configurar callbacks
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                interstitialAd = null
                                _adState.value = AdState.Dismissed
//                                loadInterstitialAd(context) // Recargar después de cerrar
                            }

                            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                                interstitialAd = null
                                _adState.value = AdState.Failed
                            }

                            override fun onAdShowedFullScreenContent() {
                                _adState.value = AdState.Showing
                            }
                        }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                        _adState.value = AdState.Failed
                    }
                }
            )
        }
    }

    fun showInterstitialAd(activity: Activity) {
        viewModelScope.launch {
            if (interstitialAd != null) {
                interstitialAd?.show(activity)
            } else {
                _adState.value = AdState.NotLoaded
            }
        }
    }

    fun isAdLoaded(): Boolean {
        return interstitialAd != null
    }
}