package com.shayo.contacts.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.shayo.contacts.R
import com.shayo.contacts.ui.detail.ContactDetailScreen
import com.shayo.contacts.ui.home.HomeScreen

const val HomeGraphRoutePattern = "home"

fun NavGraphBuilder.homeGraph(
    navController: NavController
) {
    navigation(
        startDestination = homeRoutePattern,
        route = HomeGraphRoutePattern,
    ) {

        homeScreen { contactId: String ->
            navController.navigateToContactDetails(contactId)
        }

        contactDetailScreen {
            navController.popBackStack()
        }
    }
}

private const val homeRoutePattern = "homeScreen"

private fun NavGraphBuilder.homeScreen(
    onNavigateToContactDetail: (contactId: String) -> Unit,
) {
    composable(route = homeRoutePattern) {
        HomeScreen(
            onContactClick = onNavigateToContactDetail
        )
    }
}

private const val contactDetailRoute = "contact"
private const val contactDetailRouteParam = "contactId"

private fun NavController.navigateToContactDetails(contactId: String) {
    navigate("$contactDetailRoute/$contactId")
}

@OptIn(ExperimentalMaterial3Api::class)
private fun NavGraphBuilder.contactDetailScreen(
    navigateBack: () -> Unit,
) {
    composable("$contactDetailRoute/{$contactDetailRouteParam}") {
        val contactId = it.arguments?.getString(contactDetailRouteParam)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = navigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(id = R.string.navigate_back))
                        }
                    }
                )
            },
        ) { paddingValues ->
            ContactDetailScreen(
                contactId = contactId,
                modifier = Modifier.padding(
                    paddingValues = paddingValues
                )
            )
        }
    }
}