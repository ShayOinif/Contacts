package com.shayo.contacts.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.shayo.contacts.data.model.ContactDetail
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

    val appBarState = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(appBarState.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    if (state is ContactDetailUiState.Success) {
                        if (appBarState.state.collapsedFraction > EXPANDED_THRESHOLD) {
                            Text(text = state.detailedContact.contact.displayName)
                        } else {
                            ContactInfo(contact = state.detailedContact.contact)
                        }
                    }
                },
                actions = {
                    if (state is ContactDetailUiState.Success.ViewDetailedContact) {
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
            if (state is ContactDetailUiState.Success.EditDetailedContact) {
                NavigationBar {

                    val isValid = state.invalidDetailsMap.isEmpty()

                    NavigationBarItem(
                        selected = false, onClick = cancelEdit,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text(text = stringResource(id = R.string.cancel))
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
                        },
                        enabled = isValid
                    )
                }
            }
        }
    ) { paddingValues ->

        when (state) {
            is ContactDetailUiState.Success -> {

                val editing = state is ContactDetailUiState.Success.EditDetailedContact

                BackHandler(
                    enabled = editing,
                    onBack = cancelEdit
                )

                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues = paddingValues),
                    contentPadding = PaddingValues(8.dp),
                ) {
                    detailsColumn(
                        details = state.detailedContact.phones,
                        detailType = DetailType.PHONE,
                        updateDetail = updateDetail,
                        editing = editing,
                        hasNext = state.detailedContact.emails.isNotEmpty(),
                        invalidMap = if (editing) {
                            (state as ContactDetailUiState.Success.EditDetailedContact).invalidDetailsMap
                        } else {
                            emptyMap()
                        },
                    )

                    detailsColumn(
                        details = state.detailedContact.emails,
                        detailType = DetailType.EMAIL,
                        updateDetail = updateDetail,
                        editing = editing,
                        hasNext = false,
                        invalidMap = if (editing) {
                            (state as ContactDetailUiState.Success.EditDetailedContact).invalidDetailsMap
                        } else {
                            emptyMap()
                        }
                    )
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
}

@OptIn(ExperimentalMaterial3Api::class)
private fun LazyListScope.detailsColumn(
    details: List<ContactDetail>,
    detailType: DetailType,
    updateDetail: (detailId: Long, newValue: String, detailType: DetailType) -> Unit,
    editing: Boolean,
    hasNext: Boolean,
    invalidMap: Map<Long, Void?>
) {
    if (details.isNotEmpty()) {
        item {
            Divider()
        }

        val (header, placeholder, keyboardType) = when (detailType) {
            DetailType.EMAIL -> Triple(R.string.emails, R.string.email_address, KeyboardType.Email)
            DetailType.PHONE -> Triple(R.string.phones, R.string.phone_number, KeyboardType.Phone)
        }

        item {
            Text(
                text = stringResource(id = header),
                style = MaterialTheme.typography.headlineMedium
            )
        }

        itemsIndexed(
            items = details
        ) { index, detail ->
            val isValid = invalidMap.contains(detail.id)

            Row {
                Spacer(modifier = Modifier.size(8.dp))
                TextField(
                    value = detail.value,
                    onValueChange = {
                        updateDetail(detail.id, it, detailType)
                    },
                    enabled = editing,
                    label = {
                        Text(text = stringResource(id = detail.type))
                    },
                    placeholder = {
                        Text(text = stringResource(id = placeholder))
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = if (hasNext || index < details.size - 1)
                            ImeAction.Next
                        else
                            ImeAction.Done,
                        keyboardType = keyboardType,
                    ),
                    isError = isValid, // TODO: Add description of what is wrong
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}