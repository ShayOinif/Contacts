package com.shayo.contacts.ui.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shayo.contacts.R
import com.shayo.contacts.ui.common.CenterAlignedBox
import com.shayo.contacts.ui.common.ContactInfo
import com.shayo.contacts.ui.common.ErrorBox
import com.shayo.contacts.ui.common.LoadingBox

@OptIn(ExperimentalLifecycleComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun ContactDetailScreen(
    contactId: String?,
    modifier: Modifier = Modifier,
    detailViewModel: ContactDetailViewModel = hiltViewModel(),
    /* The old way to get the view model with the factory
    = viewModel(
        factory = ContactDetailViewModel.getFactory(contactId = contactId)
    ),*/
) {
    val state by detailViewModel.getDetailedContactFlow(contactId).collectAsStateWithLifecycle()

    when (state) {
        is ContactDetailUiState.Success -> {
            val detailedContact = (state as ContactDetailUiState.Success).detailedContact

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                item {
                    ContactInfo(contact = detailedContact.contact)
                }

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

                    items(
                        items = detailedContact.phones
                    ) { phone ->
                        Row {
                            Text(text = stringResource(id = phone.type) + ": ")
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(phone.value)
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

                    items(
                        items = detailedContact.emails
                    ) { email ->
                        Row {
                            Text(text = stringResource(id = email.type) + ": ")
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(email.value)
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