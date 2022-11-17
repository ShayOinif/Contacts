package com.shayo.contacts.data

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.database.getStringOrNull
import com.shayo.contacts.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

private const val CONNECTION_TIMEOUT = 1_500L

private enum class DetailType { PHONE, EMAIL }

@Singleton
class ContactsRepository @Inject constructor(
    @ApplicationContext
    private val context: Context,
) {

    private val coroutineScope = CoroutineScope(context = SupervisorJob())

    private val contactsFlow = callbackFlow {
        trySend(context.queryAllContacts())

        val contentObserver = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(context.queryAllContacts())
            }
        }

        context.contentResolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI,
            false,
            contentObserver
        )

        awaitClose {
            context.contentResolver.unregisterContentObserver(contentObserver)
        }
    }.shareIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(CONNECTION_TIMEOUT),
        replay = 1,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    fun queryContacts(query: String) =
        contactsFlow.mapLatest { result ->
            result.map { contactList ->
                contactList.filter { contact ->
                    contact.displayName.contains(
                        other = query,
                        ignoreCase = true,
                    )
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getContact(contactId: String?) =
        contactsFlow.mapLatest { result ->
            result.fold(
                onSuccess = { contactsList ->
                    contactsList.find { contact ->
                        contact.id == contactId
                    }?.let { contact ->
                        val selectionArgs = arrayOf(contactId)

                        val phonesResult = context.getDetails(
                            projection = PHONE_PROJECTION,
                            selection = PHONE_SELECTION,
                            selectionArgs = selectionArgs,
                            sortOrder = PHONE_SORT_ORDER,
                            detailType = DetailType.PHONE
                        )

                        phonesResult.fold(
                            onSuccess = { phones ->
                                val emailsResult = context.getDetails(
                                    projection = EMAIL_PROJECTION,
                                    selection = EMAIL_SELECTION,
                                    selectionArgs = selectionArgs,
                                    sortOrder = EMAIL_SORT_ORDER,
                                    detailType = DetailType.EMAIL
                                )

                                emailsResult.fold(
                                    onSuccess = { emails ->
                                        Result.success(
                                            DetailedContact(
                                                contact = contact,
                                                phones = phones,
                                                emails = emails,
                                            )
                                        )
                                    },
                                    onFailure = { Result.failure(it) }
                                )
                            },
                            onFailure = { Result.failure(it) }
                        )
                    } ?: Result.success(null)
                },
                onFailure = {
                    Result.failure(it)
                }
            )
        }
}

private const val ILLEGAL_INDEX = -1
private const val COLUMN_ERROR = "Column doesn't exist"
private const val PROVIDER_ERROR = "Provider returned null"

private fun Context.queryAllContacts(): Result<List<Contact>> {

    var cursor: Cursor? = null

    return try {
        val contacts = mutableListOf<Contact>()

        cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            PROJECTION,
            null, null,
            ContactsContract.Contacts.DISPLAY_NAME
        )

        cursor?.let {
            val lookupKeyIndex = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
            val displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val thumbnailUri =
                cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)

            if (lookupKeyIndex == ILLEGAL_INDEX || displayNameIndex == ILLEGAL_INDEX || thumbnailUri == ILLEGAL_INDEX)
                Result.failure(Exception(COLUMN_ERROR))
            else {
                while (cursor.moveToNext()) {
                    contacts.add(
                        with(cursor) {
                            Contact(
                                id = getString(lookupKeyIndex),
                                displayName = getString(displayNameIndex),
                                photoUri = getStringOrNull(thumbnailUri),
                            )
                        }
                    )
                }

                Result.success(contacts)
            }
        } ?: Result.failure(Exception(PROVIDER_ERROR))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    } finally {
        cursor?.close()
    }
}

private fun Context.getDetails(
    projection: Array<out String>,
    selection: String,
    selectionArgs: Array<String?>,
    sortOrder: String,
    detailType: DetailType
): Result<List<ContactDetail>> {
    var cursor: Cursor? = null

    return try {
        val details = mutableListOf<ContactDetail>()

        cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.let {
            val valueIndex = cursor.getColumnIndex(projection[0])
            val typeIndex = cursor.getColumnIndex(projection[1])

            if (valueIndex == ILLEGAL_INDEX || typeIndex == ILLEGAL_INDEX)
                Result.failure<List<ContactDetail>>(Exception(COLUMN_ERROR))
            else {
                while (cursor.moveToNext()) {
                    details.add(
                        ContactDetail(
                            value = cursor.getString(valueIndex),
                            type = when (detailType) {
                                DetailType.EMAIL -> ContactsContract.CommonDataKinds.Email.getTypeLabelResource(
                                    cursor.getInt(typeIndex)
                                )
                                DetailType.PHONE -> ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(
                                    cursor.getInt(typeIndex)
                                )
                            }
                        )
                    )
                }
            }

            Result.success(details)
        } ?: Result.failure(Exception(PROVIDER_ERROR))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    } finally {
        cursor?.close()
    }
}

private val PROJECTION: Array<out String> = arrayOf(
    ContactsContract.Contacts.LOOKUP_KEY,
    ContactsContract.Contacts.DISPLAY_NAME,
    ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
)

private val EMAIL_PROJECTION: Array<out String> = arrayOf(
    ContactsContract.CommonDataKinds.Email.ADDRESS,
    ContactsContract.CommonDataKinds.Email.TYPE,
)

private const val EMAIL_SELECTION: String =
    "${ContactsContract.Data.LOOKUP_KEY} = ? AND " +
            "${ContactsContract.Data.MIMETYPE} = '${ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE}'"

private const val EMAIL_SORT_ORDER: String =
    "${ContactsContract.CommonDataKinds.Email.TYPE} ASC"

private val PHONE_PROJECTION: Array<out String> = arrayOf(
    ContactsContract.CommonDataKinds.Phone.NUMBER,
    ContactsContract.CommonDataKinds.Phone.TYPE,
)

private const val PHONE_SELECTION: String =
    "${ContactsContract.Data.LOOKUP_KEY} = ? AND " +
            "${ContactsContract.Data.MIMETYPE} = '${ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE}'"

private const val PHONE_SORT_ORDER: String =
    "${ContactsContract.CommonDataKinds.Phone.TYPE} ASC"