package com.shayo.contacts.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shayo.contacts.data.ContactsRepository
import com.shayo.contacts.data.model.Contact
import com.shayo.contacts.utils.CONFIGURATION_CHANGE_TIMEOUT
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

private const val TYPE_DEBOUNCE = 300L
private const val QUERY_KEY = "query"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    contactsRepository: ContactsRepository,
) : ViewModel() {

    private val query = savedStateHandle.getStateFlow<String?>(key = QUERY_KEY, initialValue = null)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val contactsFlow = query
        .debounce(timeoutMillis = TYPE_DEBOUNCE)
        .flatMapLatest { newQuery ->
            contactsRepository.queryContacts(newQuery)
        }

    val stateFlow = combine(query, contactsFlow) { newQuery, contactsResult ->
        contactsResult.fold(
            onSuccess = { contactsList ->
                HomeScreenUiState.Success(
                    contacts = contactsList,
                    searchQuery = newQuery,
                )
            },
            onFailure = {
                HomeScreenUiState.Error
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = CONFIGURATION_CHANGE_TIMEOUT),
        initialValue = HomeScreenUiState.Loading,
    )

    fun onInput(newQuery: String) {
        savedStateHandle[QUERY_KEY] = newQuery
    }

    fun clearSearch() {
        onInput(newQuery = "")
    }

    fun closeSearch() {
        savedStateHandle[QUERY_KEY] = null
    }
}

sealed interface HomeScreenUiState {
    object Loading : HomeScreenUiState
    data class Success(
        val contacts: List<Contact>,
        val searchQuery: String?,
    ) : HomeScreenUiState

    object Error : HomeScreenUiState
}