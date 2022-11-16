package com.shayo.contacts.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shayo.contacts.ContactsApp
import com.shayo.contacts.data.ContactsRepository
import com.shayo.contacts.data.model.DetailedContact
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

class ContactDetailViewModel(
    contactId: String?,
    contactsRepository: ContactsRepository,
    ) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val detailedContactFlow = contactsRepository.getContact(contactId)
        .mapLatest { result ->
            result.fold(
                onSuccess = { detailedContact ->
                    detailedContact?.run {
                        ContactDetailUiState.Success(
                            detailedContact = detailedContact
                        )
                    } ?: ContactDetailUiState.ContactNa

                },
                onFailure = {
                    ContactDetailUiState.Error
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1_500),
            initialValue = ContactDetailUiState.Loading
        )

    companion object {

        fun getFactory(contactId: String?) = viewModelFactory {
            initializer {
                ContactDetailViewModel(
                    contactId =  contactId,
                    contactsRepository= (get(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY) as ContactsApp).contactsRepo,
                )
            }
        }
    }
}

sealed interface ContactDetailUiState {
    object Loading : ContactDetailUiState
    data class Success(
        val detailedContact: DetailedContact,
    ) : ContactDetailUiState
    object ContactNa : ContactDetailUiState
    object Error : ContactDetailUiState
}