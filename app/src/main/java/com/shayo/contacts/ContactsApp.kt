package com.shayo.contacts

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ContactsApp : Application() {

    /* Before hilt we held a lazy instantiated singleton repository in the application
    val contactsRepo by lazy {
        ContactsRepository(applicationContext)
    }*/
}