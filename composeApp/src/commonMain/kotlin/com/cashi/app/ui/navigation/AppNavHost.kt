package com.cashi.app.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cashi.app.ui.screens.addpayment.AddPaymentScreen
import com.cashi.app.ui.screens.payments.PaymentsScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = PaymentsRoute,
        enterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { it / 4 } + fadeOut() },
    ) {
        composable<PaymentsRoute> {
            PaymentsScreen(onAddPayment = { navController.navigate(AddPaymentRoute) { launchSingleTop = true } })
        }
        composable<AddPaymentRoute> {
            AddPaymentScreen(onBack = { navController.navigateUp() })
        }
    }
}
