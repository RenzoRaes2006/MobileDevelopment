package be.ap.laatsteronde.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import be.ap.laatsteronde.MapScreen
import be.ap.laatsteronde.ui.auth.RegisterScreen

/** Route names of all screens. */
object Routes {
    const val REGISTER = "register"
    const val MAP = "map"
}

/**
 * All screens of the app and how you move between them.
 * For now (A1) the app starts on the register screen; login and the
 * start screen per role follow in A2.
 */
@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.REGISTER,
        modifier = modifier
    ) {
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = {
                    // Remove the register screen from the back stack,
                    // so the back button does not return to it.
                    navController.navigate(Routes.MAP) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MAP) {
            MapScreen()
        }
    }
}
