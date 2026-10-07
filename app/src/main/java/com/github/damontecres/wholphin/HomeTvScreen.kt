package com.github.damontecres.wholphin

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage

@Composable
fun HomeTvScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeTvViewModel = hiltViewModel(),
) {
    val state = viewModel.uiState

    var showProgram by remember {
        mutableStateOf(false)
    }

    var playback by remember {
        mutableStateOf<HomeTvPlayback?>(null)
    }

    androidx.activity.compose.BackHandler(
        enabled = playback != null || showProgram,
    ) {
        when {
            playback != null -> {
                playback = null
            }

            showProgram -> {
                showProgram = false
            }
        }
    }

    playback?.let { item ->
        HomeTvPlayerScreen(
            playback = item,
            onBack = {
                playback = null
            },
        )
        return
    }

    if (showProgram) {
        HomeTvProgramScreen(
            onBack = {
                showProgram = false
            },
            onPlay = { item ->
                playback = item
            },
        )
        return
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = 56.dp,
                vertical = 28.dp,
            ),
    ) {
        Row {
            Text(
                text = "HomeTV",
                style = MaterialTheme.typography.headlineLarge,
            )

            Spacer(Modifier.width(36.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(onClick = {}) { Text("Domů") }
                Button(onClick = {}) { Text("Živá TV") }
                Button(
                    onClick = {
                        showProgram = true
                    },
                ) {
                    Text("Program")
                }

                Button(
                    onClick = {
                        viewModel.playPrimaPlusTest { item ->
                            playback = item
                        }
                    },
                ) {
                    Text("Prima+ test")
                }
                Button(onClick = {}) { Text("Seriály") }
                Button(onClick = {}) { Text("Žebříčky") }
                Button(onClick = {}) { Text("Hledat") }
            }
        }

        Spacer(Modifier.height(24.dp))

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
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(
                        items = state.channels,
                        key = { it.number },
                    ) { channel ->
                        LiveTvCard(
                            channel = channel,
                            onPlay = { item ->
                                playback = item
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        Text(
            text = "Pokračovat",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Doporučeno pro vás",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Moje knihovna",
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Služby",
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Composable
private fun LiveTvCard(
    channel: LiveChannel,
    onPlay: (HomeTvPlayback) -> Unit,
) {
    val programme = channel.current ?: return

    Card(
        onClick = {
            onPlay(
                HomeTvPlayback(
                    title = programme.title,
                    url = channel.playUrl,
                ),
            )
        },
        modifier = Modifier.width(210.dp),
        scale = CardDefaults.scale(
            focusedScale = 1.06f,
        ),
    ) {
        Column {
            AsyncImage(
                model = programme.image ?: channel.logo,
                contentDescription = programme.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp),
            )

            Column(
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 8.dp,
                ),
            ) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = programme.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(7.dp))

                LinearProgressIndicator(
                    progress = {
                        programme.progressPercent
                            .coerceIn(0f, 100f) / 100f
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = "${programme.start}  •  ${programme.end}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}







