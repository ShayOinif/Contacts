package com.shayo.contacts.ui.detail

import androidx.compose.foundation.ExperimentalFoundationApi
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

@OptIn(
    ExperimentalLifecycleComposeApi::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun ContactDetailScreen(
    lookupKey: String?,
    navigateBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    detailViewModel: ContactDetailViewModel = hiltViewModel(),
) {
    val state by detailViewModel.getDetailedContactFlow(lookupKey).collectAsStateWithLifecycle()

    // TODO: Break to stateless composable to avoid massive recomposition and recollecting
    when (state) {
        is ContactDetailUiState.Success -> {
            val detailedContact = (state as ContactDetailUiState.Success).detailedContact
            val editing = (state as ContactDetailUiState.Success).editing

            val appBarState = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

            Scaffold(
                modifier = modifier.nestedScroll(appBarState.nestedScrollConnection),
                topBar = {
                    LargeTopAppBar(
                        title = {
                            if (appBarState.state.collapsedFraction > 0.4F) {
                                Text(text = detailedContact.contact.displayName)
                            } else {
                                ContactInfo(contact = detailedContact.contact)
                            }
                        },
                        actions = {
                            if (!editing) {
                                IconButton(onClick = detailViewModel::edit) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit contact"
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
                                selected = false, onClick = detailViewModel::cancelEdit,
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
                                selected = false, onClick = detailViewModel::save,
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null
                                    )
                                },
                                label = {
                                    Text("Save")
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

                        stickyHeader {
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
                                        detailViewModel.updateDetail(phone.id, it, DetailType.PHONE)
                                    },
                                    enabled = editing,
                                    label = {
                                        Text(text = stringResource(id = phone.type))
                                    },
                                    placeholder = {
                                        Text(text = "Phone Number")
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

                        stickyHeader {
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
                                        detailViewModel.updateDetail(email.id, it, DetailType.EMAIL)
                                    },
                                    enabled = editing,
                                    label = {
                                        Text(text = stringResource(id = email.type))
                                    },
                                    placeholder = {
                                        Text(text = "Email address")
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