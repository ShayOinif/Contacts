package com.shayo.contacts.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shayo.contacts.data.model.Contact

private val imageModifier = Modifier
    .size(96.dp)
    .padding(all = 8.dp)
    .clip(shape = CircleShape)

@Composable
fun ContactInfo(
    contact: Contact,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        contact.photoUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = imageModifier
            )
        } ?: Text(
            text = contact.displayName.first().toString(),
            modifier = imageModifier.background(MaterialTheme.colorScheme.primary),
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayLarge,
        )

        Text(
            text = contact.displayName,
            modifier = Modifier
                .padding(all = 8.dp),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}