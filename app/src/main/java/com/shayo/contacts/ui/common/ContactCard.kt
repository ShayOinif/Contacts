package com.shayo.contacts.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shayo.contacts.data.model.Contact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactCard(
    contact: Contact,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onContactClick,
    ) {
        ContactInfo(
            contact = contact,
            modifier = modifier,
        )
    }
}