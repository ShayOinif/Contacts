package com.shayo.contacts.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shayo.contacts.ContactsApp
import com.shayo.contacts.data.ContactsRepository
import com.shayo.contacts.data.model.Contact
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

private const val TYPE_DEBOUNCE = 300L
private const val CONFIGURATION_CHANGE_TIMEOUT = 1_500L

class HomeViewModel(private val contactsRepository: ContactsRepository) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val contactsFlow = queryFlow
        .debounce(timeoutMillis = TYPE_DEBOUNCE)
        .flatMapLatest { newQuery ->
            contactsRepository.queryContacts(newQuery)
        }

    val stateFlow = combine(queryFlow, contactsFlow) { newQuery, contactsResult ->
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
        queryFlow.value = newQuery
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                HomeViewModel((get(APPLICATION_KEY) as ContactsApp).contactsRepo)
            }
        }
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