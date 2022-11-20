package com.shayo.contacts.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shayo.contacts.R
import com.shayo.contacts.ui.common.CenterAlignedBox
import com.shayo.contacts.ui.common.ContactInfo
import com.shayo.contacts.ui.common.ErrorBox
import com.shayo.contacts.ui.common.LoadingBox

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun ContactDetailScreen(
    lookupKey: String,
    navigateBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    detailViewModel: ContactDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(key1 = detailViewModel) {
        detailViewModel.setLookupKey(lookupKey)
    }

    val state by detailViewModel.detailedContactFlow.collectAsStateWithLifecycle()

    ContactDetailScreen(
        navigateBack = navigateBack,
        edit = detailViewModel::edit,
        cancelEdit = detailViewModel::cancelEdit,
        save = detailViewModel::save,
        updateDetail = detailViewModel::updateDetail,
        state = state,
        modifier = modifier,
    )
}

private const val EXPANDED_THRESHOLD = 0.4F

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDetailScreen(
    navigateBack: (() -> Unit)?,
    edit: () -> Unit,
    cancelEdit: () -> Unit,
    save: () -> Unit,
    updateDetail: (detailId: Long, newValue: String, detailType: DetailType) -> Unit,
    state: ContactDetailUiState,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ContactDetailUiState.Success -> {
            val detailedContact = state.detailedContact
            val editing = state.editing

            val appBarState = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

            BackHandler(
                enabled = state.editing,
                onBack = cancelEdit
            )

            Scaffold(
                modifier = modifier.nestedScroll(appBarState.nestedScrollConnection),
                topBar = {
                    LargeTopAppBar(
                        title = {
                            if (appBarState.state.collapsedFraction > EXPANDED_THRESHOLD) {
                                Text(text = detailedContact.contact.displayName)
                            } else {
                                ContactInfo(contact = detailedContact.contact)
                            }
                        },
                        actions = {
                            if (!editing) {
                                IconButton(onClick = edit) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(id = R.string.edit_contact)
                                    )
                                }
                            }
                        },
                        scrollBehavior = appBarState,
                        navigationIcon = {
                            navigateBack?.let {
                                IconButton(onClick = navigateBack) {
                                    Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = stringResource(id = R.string.navigate_back)
                                    )
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (editing) {
                        NavigationBar {
                            NavigationBarItem(
                                selected = false, onClick = cancelEdit,
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null
                                    )
                                },
                                label = {
                                    Text("Cancel")
                                }
                            )

                            NavigationBarItem(
                                selected = false, onClick = save,
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null
                                    )
                                },
                                label = {
                                    Text(text = stringResource(id = R.string.save))
                                }
                            )
                        }
                    }
                }
            ) { paddingValues ->

                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues = paddingValues),
                    contentPadding = PaddingValues(8.dp),
                ) {
                    if (detailedContact.phones.isNotEmpty()) {
                        item {
                            Divider()
                        }

                        item {
                            Text(
                                text = stringResource(id = R.string.phones),
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }

                        itemsIndexed(
                            items = detailedContact.phones
                        ) { index, phone ->
                            Row {
                                Spacer(modifier = Modifier.size(8.dp))
                                TextField(
                                    value = phone.value,
                                    onValueChange = {
                                        updateDetail(phone.id, it, DetailType.PHONE)
                                    },
                                    enabled = editing,
                                    label = {
                                        Text(text = stringResource(id = phone.type))
                                    },
                                    placeholder = {
                                        Text(text = stringResource(R.string.phone_number))
                                    },
                                    keyboardOptions = KeyboardOptions.Default.copy(
                                        imeAction =
                                        if (detailedContact.emails.isNotEmpty() || index < detailedContact.phones.size - 1)
                                            ImeAction.Next
                                        else
                                            ImeAction.Done,
                                        keyboardType = KeyboardType.Number,
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    if (detailedContact.emails.isNotEmpty()) {
                        item {
                            Divider()
                        }

                        item {
                            Text(
                                text = stringResource(id = R.string.emails),
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }

                        itemsIndexed(
                            items = detailedContact.emails
                        ) { index, email ->
                            Row {
                                Spacer(modifier = Modifier.size(8.dp))
                                TextField(
                                    value = email.value,
                                    onValueChange = {
                                        updateDetail(email.id, it, DetailType.EMAIL)
                                    },
                                    enabled = editing,
                                    label = {
                                        Text(text = stringResource(id = email.type))
                                    },
                                    placeholder = {
                                        Text(text = stringResource(id = R.string.email_address))
                                    },
                                    keyboardOptions = KeyboardOptions.Default.copy(
                                        imeAction = if (index < detailedContact.emails.size - 1)
                                            ImeAction.Next
                                        else
                                            ImeAction.Done,
                                        keyboardType = KeyboardType.Email,
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
        ContactDetailUiState.Loading -> LoadingBox(
            modifier = modifier,
        )
        ContactDetailUiState.ContactNa -> CenterAlignedBox(modifier = modifier) {
            Text(text = stringResource(id = R.string.contact_na))
        }
        ContactDetailUiState.Error -> ErrorBox(modifier = modifier)
    }
}