package com.shayo.contacts.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.shayo.contacts.R

@Composable
fun CenterAlignedBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun LoadingBox(
    modifier: Modifier = Modifier,
) {
    CenterAlignedBox(
        modifier = modifier
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorBox(
    modifier: Modifier,
) {
    CenterAlignedBox(modifier = modifier) {
        Text(text = stringResource(id = R.string.error))
    }
}