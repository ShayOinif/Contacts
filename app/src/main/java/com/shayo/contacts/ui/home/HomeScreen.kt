package com.shayo.contacts.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.shayo.contacts.R
import com.shayo.contacts.ui.common.CenterAlignedBox
import com.shayo.contacts.ui.common.ContactCard
import com.shayo.contacts.ui.common.ErrorBox

@OptIn(ExperimentalLifecycleComposeApi::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onContactClick: (contactId: String) -> Unit,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {

    val contactsPermissionState = rememberPermissionState(
        android.Manifest.permission.READ_CONTACTS
    )

    var declined by rememberSaveable {
        mutableStateOf(false)
    }

    when {
        contactsPermissionState.status.isGranted -> {

            val listState = rememberLazyListState()

            val state by homeViewModel.stateFlow.collectAsStateWithLifecycle()

            HomeScreen(
                state = state,
                listState = listState,
                onNewQuery = homeViewModel::onInput,
                onContactClick = onContactClick,
                modifier = modifier
            )
        }
        declined -> {
            CenterAlignedBox(
                modifier = modifier,
            ) {
                Text(text = stringResource(id = R.string.no_permissions))
            }
        }
        contactsPermissionState.status.shouldShowRationale -> {
            AlertDialog(
                onDismissRequest = { declined = true },
                confirmButton = {
                    Text(
                        text = stringResource(id = R.string.approve),
                        modifier = Modifier.clickable { contactsPermissionState.launchPermissionRequest() }
                    )
                },
                dismissButton = {
                    Text(
                        text = stringResource(id = R.string.decline),
                        modifier = Modifier.clickable { declined = true }
                    )
                },
                text = {
                    Text(text = stringResource(id = R.string.permission_rationale))
                }
            )
        }
        else -> {
            LaunchedEffect(key1 = true) {
                contactsPermissionState.launchPermissionRequest()
            }

            CenterAlignedBox(
                modifier = modifier,
            ) {
                Text(text = stringResource(id = R.string.no_permissions))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: HomeScreenUiState,
    listState: LazyListState,
    onNewQuery: (query: String) -> Unit,
    onContactClick: (contactId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        HomeScreenUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        is HomeScreenUiState.Success -> {
            Column(
                modifier = modifier.fillMaxSize()
            ) {
                TextField(
                    value = state.searchQuery,
                    onValueChange = onNewQuery,
                    placeholder = { Text(text = stringResource(id = R.string.search)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                )

                if (state.contacts.isEmpty()) {
                    CenterAlignedBox(modifier = modifier) {
                        Text(text = stringResource(id = R.string.empty))
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(all = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        state = listState
                    ) {
                        items(
                            items = state.contacts,
                            key = { contact ->
                                contact.id
                            }
                        ) { contact ->
                            ContactCard(
                                contact = contact,
                                onContactClick = { onContactClick(contact.id) }
                            )
                        }
                    }
                }
            }
        }
        HomeScreenUiState.Error -> ErrorBox(modifier = modifier)
    }
}