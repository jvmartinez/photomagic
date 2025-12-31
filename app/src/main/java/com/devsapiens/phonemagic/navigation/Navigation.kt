package com.devsapiens.phonemagic.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devsapiens.phonemagic.ui.export.ExportScreen
import com.devsapiens.phonemagic.ui.home.HomeScreen
import com.devsapiens.phonemagic.ui.editor.EditorScreen
import com.devsapiens.phonemagic.viewmodel.EditorViewModel

object Routes {
    const val HOME = "home"
    const val EDITOR = "editor"
    const val EXPORT = "export"
}

@Composable
fun PhoneMagicApp(modifier: Modifier = Modifier) {
    val navController: NavHostController = rememberNavController()
    val editorViewModel: EditorViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.HOME, modifier = modifier) {
        composable(Routes.HOME) {
            HomeScreen(onImageSelected = { uri ->
                editorViewModel.setImageUri(uri)
                navController.navigate(Routes.EDITOR)
            })
        }
        composable(Routes.EDITOR) {
            EditorScreen(onExport = {
                navController.navigate(Routes.EXPORT)
            }, onBack = { navController.popBackStack() }, viewModel = editorViewModel)
        }
        composable(Routes.EXPORT) {
            ExportScreen(onDone = {
                navController.popBackStack(Routes.HOME, inclusive = false)
            })
        }
    }
}
