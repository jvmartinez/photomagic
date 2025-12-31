package com.devsapiens.phonemagic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import com.devsapiens.phonemagic.navigation.PhoneMagicApp
import com.devsapiens.phonemagic.ui.theme.PhoneMagicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhoneMagicTheme {
                PhoneMagicApp(modifier = Modifier)
            }
        }
    }
}
