package com.shayo.contacts

import android.app.Application
import com.shayo.contacts.data.ContactsRepository

class ContactsApp : Application() {
    val contactsRepo by lazy {
        ContactsRepository(applicationContext)
    }
}