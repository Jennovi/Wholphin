@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.github.damontecres.wholphin

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.ui.PlayerView
import com.github.damontecres.wholphin.services.WholphinRenderersFactory

@Composable
fun HomeTvPlayerScreen(
    playback: HomeTvPlayback,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    val player = remember(playback.headers) {
        val extractorsFactory =
            DefaultExtractorsFactory()
                .setConstantBitrateSeekingEnabled(true)
                .setConstantBitrateSeekingAlwaysEnabled(true)

        val httpDataSourceFactory =
            DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(playback.headers)

        val mediaSourceFactory =
            DefaultMediaSourceFactory(
                DefaultDataSource.Factory(
                    context,
                    httpDataSourceFactory,
                ),
                extractorsFactory,
            )

        val renderersFactory =
            WholphinRenderersFactory(
                context = context,
                av1Enabled = false,
            )
                .setEnableDecoderFallback(true)
                .setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON,
                )

        val trackSelector =
            DefaultTrackSelector(context).apply {
                setParameters(
                    buildUponParameters()
                        .setAudioOffloadPreferences(
                            AudioOffloadPreferences.Builder()
                                .setAudioOffloadMode(
                                    AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED,
                                )
                                .build(),
                        ),
                )
            }

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true,
                )
            }
    }

    val playerView = remember {
        PlayerView(context).apply {
            this.player = player
            useController = true
        }
    }

    DisposableEffect(playback.url) {
        val mediaItem =
            MediaItem.Builder()
                .setUri(playback.url)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        onDispose {
            player.playWhenReady = false
            player.stop()
            player.clearMediaItems()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playerView.player = null

            player.clearVideoSurface()
            player.stop()
            player.clearMediaItems()
            player.release()
        }
    }

    AndroidView(
        factory = {
            playerView
        },
        modifier = Modifier.fillMaxSize(),
    )
}
