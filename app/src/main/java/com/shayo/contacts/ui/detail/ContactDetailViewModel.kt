package com.shayo.contacts.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shayo.contacts.data.ContactsRepository
import com.shayo.contacts.data.model.DetailedContact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactDetailViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
) : ViewModel() {
    private var editingFlow = MutableStateFlow<Pair<Boolean, DetailedContact?>>(Pair(false, null))

    fun getDetailedContactFlow(lookupKey: String?) =
        combine(editingFlow, contactsRepository.getContact(lookupKey)) { editing, result ->
            result.fold(
                onSuccess = { detailedContact ->
                    detailedContact?.let {
                        if (!editing.first)
                            editingFlow.update {
                                it.copy(second = detailedContact)
                            }
                    }

                    detailedContact?.run {
                        if (editing.first) {
                            ContactDetailUiState.Success(
                                detailedContact = editing.second!!, // TODO
                                editing = true
                            )
                        } else {
                            ContactDetailUiState.Success(
                                detailedContact = detailedContact
                            )
                        }
                    } ?: ContactDetailUiState.ContactNa
                },
                onFailure = {
                    ContactDetailUiState.Error
                }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1_500),
            initialValue = ContactDetailUiState.Loading
        )

    fun edit() {
        editingFlow.update {
            it.copy(first = true)
        }
    }

    fun cancelEdit() {
        editingFlow.value = Pair(false, null)
    }

    fun save() {
        viewModelScope.launch {
            contactsRepository.updateContact(editingFlow.value.second!!)

            editingFlow.value = Pair(false, null)
        }
    }

    fun updateDetail(
        detailId: Long,
        newValue: String,
        type: DetailType,
    ) {
        editingFlow.update {
            it.copy(
                second = if (type == DetailType.PHONE) {
                    it.second!!.copy(
                        phones = it.second!!.phones.map {
                            if (it.id == detailId) {
                                it.copy(value = newValue)
                            } else
                                it
                        }
                    )

                } else {
                    it.second!!.copy(
                        emails = it.second!!.emails.map {
                            if (it.id == detailId) {
                                it.copy(value = newValue)
                            } else
                                it
                        }
                    )
                }
            )
        }
    }
}

enum class DetailType { PHONE, EMAIL }

sealed interface ContactDetailUiState {
    object Loading : ContactDetailUiState
    data class Success(
        val detailedContact: DetailedContact,
        val editing: Boolean = false
    ) : ContactDetailUiState

    object ContactNa : ContactDetailUiState
    object Error : ContactDetailUiState
}