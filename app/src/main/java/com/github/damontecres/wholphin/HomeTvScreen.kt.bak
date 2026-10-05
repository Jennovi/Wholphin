package com.github.damontecres.wholphin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun HomeTvScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp, vertical = 40.dp),
    ) {
        Text(
            text = "HomeTV",
            style = MaterialTheme.typography.headlineLarge,
        )

        Spacer(Modifier.height(28.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(onClick = {}) { Text("Domů") }
            Button(onClick = {}) { Text("Živá TV") }
            Button(onClick = {}) { Text("Filmy") }
            Button(onClick = {}) { Text("Seriály") }
            Button(onClick = {}) { Text("Žebříčky") }
            Button(onClick = {}) { Text("Hledat") }
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = "Pokračovat",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Právě v TV",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Doporučeno pro vás",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Moje knihovna",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Služby",
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

