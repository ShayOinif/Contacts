package com.shayo.contacts.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shayo.contacts.data.ContactsRepository
import com.shayo.contacts.data.model.DetailedContact
import com.shayo.contacts.utils.CONFIGURATION_CHANGE_TIMEOUT
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactDetailViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
) : ViewModel() {

    private val lookupKey = MutableStateFlow<String?>(null)

    private var editingFlow = MutableStateFlow<Pair<Boolean, DetailedContact?>>(Pair(false, null))

    @OptIn(ExperimentalCoroutinesApi::class)
    val detailedContactFlow =
        lookupKey.flatMapLatest { currentKey ->
            currentKey?.let { key ->
                combine(editingFlow, contactsRepository.getContact(key)) { editing, result ->
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
                }




            } ?: flow { ContactDetailUiState.Loading }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(CONFIGURATION_CHANGE_TIMEOUT),
            initialValue = ContactDetailUiState.Loading
        )

    fun setLookupKey(lookupKey: String) {
        this.lookupKey.value = lookupKey
    }

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