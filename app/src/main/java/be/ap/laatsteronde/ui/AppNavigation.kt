package be.ap.laatsteronde.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import be.ap.laatsteronde.ui.auth.LoginScreen
import be.ap.laatsteronde.ui.auth.RegisterScreen
import be.ap.laatsteronde.ui.customer.CustomerHomeScreen
import be.ap.laatsteronde.ui.merchant.MerchantHomeScreen
import be.ap.laatsteronde.ui.merchant.NewPackageScreen

/** Route names of all screens. */
object Routes {
    const val START = "start"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CUSTOMER_HOME = "customer_home"
    const val MERCHANT_HOME = "merchant_home"
    const val NEW_PACKAGE = "new_package"
}

/**
 * All screens of the app and how you move between them.
 * The app opens on [Routes.START], which checks who is signed in and
 * forwards to the login screen or to the home screen of the user's role.
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    sessionViewModel: SessionViewModel = viewModel()
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.START,
        modifier = modifier
    ) {
        composable(Routes.START) {
            val startRoute by sessionViewModel.startRoute.collectAsStateWithLifecycle()
            // Leave the start screen once we know where to go, and remove it from the back stack.
            LaunchedEffect(startRoute) {
                startRoute?.let { navController.navigateAndClear(it, from = Routes.START) }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = { role ->
                    navController.navigateAndClear(homeRouteFor(role), from = Routes.LOGIN)
                },
                onRegisterClick = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = { role ->
                    // Remove the register screen from the back stack,
                    // so the back button does not return to it.
                    navController.navigateAndClear(homeRouteFor(role), from = Routes.REGISTER)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.CUSTOMER_HOME) {
            CustomerHomeScreen(onLogout = { logout(navController, sessionViewModel) })
        }
        composable(Routes.MERCHANT_HOME) {
            MerchantHomeScreen(
                onNewPackage = { navController.navigate(Routes.NEW_PACKAGE) },
                onLogout = { logout(navController, sessionViewModel) }
            )
        }
        composable(Routes.NEW_PACKAGE) {
            // The cross and a successful save both go back to "Vandaag", which then reads the list again.
            NewPackageScreen(onDone = { navController.popBackStack() })
        }
    }
}

private fun logout(navController: NavHostController, sessionViewModel: SessionViewModel) {
    sessionViewModel.logout()
    // Clear the whole back stack, so the back button cannot return to a screen of the old user.
    navController.navigate(Routes.LOGIN) {
        popUpTo(navController.graph.id) { inclusive = true }
    }
}

private fun NavHostController.navigateAndClear(route: String, from: String) {
    navigate(route) { popUpTo(from) { inclusive = true } }
}
