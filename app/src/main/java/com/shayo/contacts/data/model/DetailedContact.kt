package com.shayo.contacts.data.model

import androidx.annotation.StringRes

data class DetailedContact(
    val contact: Contact,
    /*val firstName: String?,
    val lastName: String?,*/
    val phones: List<ContactDetail>,
    val emails: List<ContactDetail>
)

data class ContactDetail(
    val id: Long,
    val value: String,
    @StringRes val type: Int,
)