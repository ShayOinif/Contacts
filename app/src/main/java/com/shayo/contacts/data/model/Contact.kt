package com.shayo.contacts.data.model

data class Contact(
    val id: Long,
    val displayName: String,
    val photoUri: String?,
    val lookupKey: String,
)