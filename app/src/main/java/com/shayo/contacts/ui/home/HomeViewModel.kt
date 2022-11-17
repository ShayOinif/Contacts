package com.shayo.contacts.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shayo.contacts.data.ContactsRepository
import com.shayo.contacts.data.model.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

private const val TYPE_DEBOUNCE = 300L
private const val CONFIGURATION_CHANGE_TIMEOUT = 1_500L
private const val QUERY_KEY = "query"

/**
 * Here I used a hilt injected view model cause it's a bit easier to inject additional
 * dependencies alongside the saved state handle.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    contactsRepository: ContactsRepository,
) : ViewModel() {

    private val query =  savedStateHandle.getStateFlow(key = QUERY_KEY, initialValue = "")

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
}

sealed interface HomeScreenUiState {
    object Loading : HomeScreenUiState
    data class Success(
        val contacts: List<Contact>,
        val searchQuery: String,
    ) : HomeScreenUiState
    object Error: HomeScreenUiState
}