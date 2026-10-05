package com.github.damontecres.wholphin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage

@Composable
fun HomeTvScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeTvViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = 64.dp,
                vertical = 32.dp,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "HomeTV",
                style = MaterialTheme.typography.headlineLarge,
            )

            Spacer(Modifier.width(40.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = {}) { Text("Domů") }
                Button(onClick = {}) { Text("Živá TV") }
                Button(onClick = {}) { Text("Filmy") }
                Button(onClick = {}) { Text("Seriály") }
                Button(onClick = {}) { Text("Žebříčky") }
                Button(onClick = {}) { Text("Hledat") }
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Právě v TV",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(Modifier.height(12.dp))

        when {
            state.loading -> {
                Text("Načítám televizní program…")
            }

            state.error != null -> {
                Column {
                    Text("HomeTV API není dostupné")
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = viewModel::refresh,
                    ) {
                        Text("Zkusit znovu")
                    }
                }
            }

            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(
                        items = state.channels,
                        key = { it.number },
                    ) { channel ->
                        LiveTvCard(channel)
                    }
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        Text(
            text = "Pokračovat",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Doporučeno pro vás",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Moje knihovna",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Služby",
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Composable
private fun LiveTvCard(
    channel: LiveChannel,
) {
    val programme = channel.current ?: return

    var focused by remember {
        mutableStateOf(false)
    }

    val shape = RoundedCornerShape(12.dp)

    Button(
        onClick = {
            // Přehrávání připojíme v dalším kroku.
        },
        modifier = Modifier
            .width(270.dp)
            .onFocusChanged {
                focused = it.isFocused
            }
            .then(
                if (focused) {
                    Modifier.border(
                        width = 2.dp,
                        color = Color.White,
                        shape = shape,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .clip(shape)
                    .background(Color.DarkGray),
            ) {
                AsyncImage(
                    model = programme.image ?: channel.logo,
                    contentDescription = programme.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = channel.name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )

            Text(
                text = programme.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = {
                    programme.progressPercent
                        .coerceIn(0f, 100f) / 100f
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "${programme.start}  •  ${programme.end}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
