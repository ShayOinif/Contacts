package com.shayo.contacts.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.shayo.contacts.R
import com.shayo.contacts.ui.common.CenterAlignedBox
import com.shayo.contacts.ui.common.ContactCard
import com.shayo.contacts.ui.common.ErrorBox

@OptIn(
    ExperimentalLifecycleComposeApi::class, ExperimentalPermissionsApi::class,
)
@Composable
fun HomeScreen(
    onContactClick: (lookupKey: String) -> Unit,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {

    val contactsPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.WRITE_CONTACTS)
    )

    var declined by rememberSaveable {
        mutableStateOf(false)
    }

    when {
        contactsPermissionState.allPermissionsGranted -> {

            val listState = rememberLazyListState()

            val state by homeViewModel.stateFlow.collectAsStateWithLifecycle()

            HomeScreen(
                state = state,
                listState = listState,
                onContactClick = onContactClick,
                onNewQuery = homeViewModel::onInput,
                closeSearch = homeViewModel::closeSearch,
                clearSearch = homeViewModel::clearSearch,
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
        contactsPermissionState.shouldShowRationale -> {
            AlertDialog(
                onDismissRequest = { declined = true },
                confirmButton = {
                    Text(
                        text = stringResource(id = R.string.approve),
                        modifier = Modifier.clickable { contactsPermissionState.launchMultiplePermissionRequest() }
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
                contactsPermissionState.launchMultiplePermissionRequest()
            }

            CenterAlignedBox(
                modifier = modifier,
            ) {
                Text(text = stringResource(id = R.string.no_permissions))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun HomeScreen(
    state: HomeScreenUiState,
    listState: LazyListState,
    onContactClick: (lookupKey: String) -> Unit,
    onNewQuery: (String) -> Unit,
    closeSearch: () -> Unit,
    clearSearch: () -> Unit,
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

            val appBarState = TopAppBarDefaults.enterAlwaysScrollBehavior()
            val scaffoldModifier =
                state.searchQuery?.let { Modifier }
                    ?: Modifier.nestedScroll(appBarState.nestedScrollConnection)

            // TODO: Centralize the scaffold management
            Scaffold(
                modifier = scaffoldModifier,
                topBar = {
                    TopAppBar(
                        title = { Text(text = stringResource(id = R.string.home_header)) },
                        scrollBehavior = appBarState,
                        actions = {
                            val query = state.searchQuery

                            query?.let {

                                val focusRequester = remember { FocusRequester() }

                                LaunchedEffect(Unit) {
                                    focusRequester.requestFocus()
                                }

                                val keyboardController = LocalSoftwareKeyboardController.current

                                TextField(
                                    value = query,
                                    onValueChange = onNewQuery,
                                    placeholder = { Text(text = stringResource(id = R.string.search)) },
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onAny = { keyboardController?.hide() }),
                                    trailingIcon = {
                                        IconButton(
                                            onClick = if (query.isEmpty())
                                                closeSearch
                                            else
                                                clearSearch
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                "Close search"
                                            ) // TODO: Change desc
                                        }
                                    }
                                )
                            } ?: run {
                                IconButton(onClick = { onNewQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search contacts"
                                    )
                                }
                            }
                        }
                    )
                }
            ) { paddingValues ->

                BackHandler(
                    enabled = state.searchQuery != null,
                    onBack = closeSearch
                )

                if (state.contacts.isEmpty()) {
                    CenterAlignedBox(
                        modifier = modifier
                            .fillMaxSize()
                            .padding(paddingValues = paddingValues)
                    ) {
                        Text(text = stringResource(id = R.string.empty))
                    }
                } else {
                    LazyColumn(
                        modifier = modifier.padding(paddingValues = paddingValues),
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
                                onContactClick = { onContactClick(contact.lookupKey) }
                            )
                        }
                    }
                }
            }
        }
        HomeScreenUiState.Error -> ErrorBox(modifier = modifier)
    }
}