package com.shayo.contacts.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shayo.contacts.data.ContactsRepository
import com.shayo.contacts.data.model.DetailedContact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ContactDetailViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getDetailedContactFlow(contactId: String?) = contactsRepository.getContact(contactId)
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

    /* After the switch to hilt I don't need the factory in order to get a singleton repository
     * which the application holds.
    companion object {
        val Factory = viewModelFactory {
            initializer {
                ContactDetailViewModel(
                    contactsRepository= (get(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY) as ContactsApp).contactsRepo,
                )
            }
        }
    }*/
}

sealed interface ContactDetailUiState {
    object Loading : ContactDetailUiState
    data class Success(
        val detailedContact: DetailedContact,
    ) : ContactDetailUiState
    object ContactNa : ContactDetailUiState
    object Error : ContactDetailUiState
}