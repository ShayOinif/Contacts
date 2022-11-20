package com.shayo.contacts.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.shayo.contacts.ui.detail.ContactDetailScreen
import com.shayo.contacts.ui.home.HomeScreen

const val HomeGraphRoutePattern = "home"

fun NavGraphBuilder.homeGraph(
    navController: NavController,
) {
    navigation(
        startDestination = homeRoutePattern,
        route = HomeGraphRoutePattern,
    ) {
        homeScreen { lookupKey: String ->
            navController.navigateToContactDetails(lookupKey)
        }

        contactDetailScreen {
            navController.popBackStack()
        }
    }
}

private const val homeRoutePattern = "homeScreen"

private fun NavGraphBuilder.homeScreen(
    onNavigateToContactDetail: (lookupKey: String) -> Unit,
) {
    composable(route = homeRoutePattern) {
        HomeScreen(
            onContactClick = onNavigateToContactDetail
        )
    }
}

private const val contactDetailRoute = "contact"
private const val contactDetailRouteParam = "lookupKey"

private fun NavController.navigateToContactDetails(lookupKey: String) {
    navigate("$contactDetailRoute/$lookupKey")
}

private fun NavGraphBuilder.contactDetailScreen(
    navigateBack: () -> Unit,
) {
    composable(route = "$contactDetailRoute/{$contactDetailRouteParam}",
    arguments = listOf(
        navArgument(
            name = contactDetailRouteParam,
        ) {
            type = NavType.StringType
        }
    )) {
        val lookupKey = it.arguments?.getString(contactDetailRouteParam) ?: "-1"

            ContactDetailScreen(
                lookupKey = lookupKey,
                navigateBack = navigateBack
            )
    }
}