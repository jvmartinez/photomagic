package com.devsapiens.phonemagic.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devsapiens.galaxylu.components.admodView.InterstitialAdScreen
import com.devsapiens.phonemagic.navigation.Routes.SplashScreen
import com.devsapiens.phonemagic.ui.export.ExportScreen
import com.devsapiens.phonemagic.ui.home.HomeScreen
import com.devsapiens.phonemagic.ui.editor.EditorScreen
import com.devsapiens.phonemagic.ui.splash.SplashScreen
import com.devsapiens.phonemagic.viewmodel.EditorViewModel

object Routes {
    const val HOME = "home"
    const val EDITOR = "editor"
    const val EXPORT = "export"
    const val InterstitialAdScreen = "ad_screen"
    const val SplashScreen = "splash_screen"
}

@Composable
fun PhoneMagicApp(modifier: Modifier = Modifier) {
    val navController: NavHostController = rememberNavController()
    val editorViewModel: EditorViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.SplashScreen, modifier = modifier) {

        composable(SplashScreen) {
            SplashScreen(
                onTimeout = {
                    navController.popBackStack()
                    navController.navigate(Routes.HOME)
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(onImageSelected = { uri ->
                editorViewModel.setImageUri(uri)
                navController.navigate(Routes.InterstitialAdScreen)
            })
        }
        composable(Routes.EDITOR) {
            EditorScreen(
                onBack = {
                    navController.popBackStack()
                }, viewModel = editorViewModel
            )
        }
        composable(Routes.InterstitialAdScreen) {
            InterstitialAdScreen(
                viewModel = viewModel()
            ) {
                navController.popBackStack()
                navController.navigate(Routes.EDITOR)
            }
        }
        composable(Routes.EXPORT) {
            ExportScreen(onDone = {
                navController.popBackStack(Routes.HOME, inclusive = false)
            })
        }
    }
}
