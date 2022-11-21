package com.shayo.contacts.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shayo.contacts.data.ContactsRepository
import com.shayo.contacts.data.model.DetailedContact
import com.shayo.contacts.utils.CONFIGURATION_CHANGE_TIMEOUT
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactDetailViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
) : ViewModel() {

    private val lookupKey = MutableStateFlow<String?>(null)

    private var editingFlow =
        MutableStateFlow<ContactDetailUiState.Success.EditDetailedContact?>(null)

    private lateinit var contactSnapshot: DetailedContact

    @OptIn(ExperimentalCoroutinesApi::class)
    val detailedContactFlow =
        lookupKey.flatMapLatest { currentKey ->
            currentKey?.let { key ->
                combine(
                    editingFlow,
                    contactsRepository.getContact(key)
                ) { editState, detailedContactResult ->
                    detailedContactResult.fold(
                        onSuccess = { detailedContact ->

                            detailedContact?.run {
                                if (editState == null)
                                    contactSnapshot = detailedContact

                                editState ?: ContactDetailUiState.Success.ViewDetailedContact(
                                    detailedContact = detailedContact
                                )
                            } ?: ContactDetailUiState.ContactNa
                        },
                        onFailure = {
                            ContactDetailUiState.Error
                        }
                    )
                }
            } ?: emptyFlow()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(CONFIGURATION_CHANGE_TIMEOUT),
            initialValue = ContactDetailUiState.Loading
        )

    fun setLookupKey(lookupKey: String) {
        this.lookupKey.value = lookupKey
    }

    fun edit() {
        editingFlow.value = ContactDetailUiState.Success.EditDetailedContact(
            detailedContact = contactSnapshot,
        )
    }

    fun cancelEdit() {
        editingFlow.value = null
    }

    // TODO: Delegate to work manager cause we want the change to persist
    fun save() {
        viewModelScope.launch(Dispatchers.Default) {
            editingFlow.value?.let { editState ->
                if (editState.invalidDetailsMap.isEmpty()) {
                    contactSnapshot.let { originalContact ->
                        val changedPhones =
                            editState.detailedContact.phones.minus(originalContact.phones.toSet())
                        val changedEmails =
                            editState.detailedContact.emails.minus(originalContact.emails.toSet())

                        contactsRepository.updateDetails(changedPhones.plus(changedEmails))

                        editingFlow.value = null
                    }
                }
            }
        }
    }

    fun updateDetail(
        detailId: Long,
        newValue: String,
        type: DetailType,
    ) {
        editingFlow.update { editState ->
            editState?.let {
                val updatedContact = when (type) {
                    DetailType.PHONE -> {
                        editState.detailedContact.copy(
                            phones = editState.detailedContact.phones.map { phone ->
                                if (phone.id == detailId) {
                                    phone.copy(value = newValue)
                                } else {
                                    phone
                                }
                            }
                        )
                    }
                    DetailType.EMAIL -> {
                        editState.detailedContact.copy(
                            emails = editState.detailedContact.emails.map { email ->
                                if (email.id == detailId) {
                                    email.copy(value = newValue)
                                } else {
                                    email
                                }
                            }
                        )
                    }
                }

                val updatedMap = editState.invalidDetailsMap.toMutableMap().apply {
                    if (newValue.isEmpty()) {
                        put(detailId, null)
                    } else {
                        remove(detailId)
                    }
                }

                ContactDetailUiState.Success.EditDetailedContact(
                    detailedContact = updatedContact,
                    invalidDetailsMap = updatedMap
                )
            }
        }
    }
}

enum class DetailType { PHONE, EMAIL }

sealed interface ContactDetailUiState {
    object Loading : ContactDetailUiState
    sealed class Success(
        val detailedContact: DetailedContact,
    ) : ContactDetailUiState {
        class ViewDetailedContact(
            detailedContact: DetailedContact,
        ) : Success(detailedContact = detailedContact)

        class EditDetailedContact(
            detailedContact: DetailedContact,
            val invalidDetailsMap: Map<Long, Void?> = emptyMap()
        ) : Success(detailedContact = detailedContact)
    }

    object ContactNa : ContactDetailUiState
    object Error : ContactDetailUiState
}