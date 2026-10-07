package com.github.damontecres.wholphin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HomeTvLiveResponse(
    val provider: String,
    @SerialName("generated_at")
    val generatedAt: String,
    val timezone: String,
    val count: Int,
    val channels: List<LiveChannel>,
)

@Serializable
data class LiveChannel(
    val provider: String,
    val number: Int,
    val name: String,
    val logo: String? = null,
    @SerialName("catchup_days")
    val catchupDays: Int = 0,
    @SerialName("play_url")
    val playUrl: String,
    val current: TvProgramme? = null,
    val next: TvProgramme? = null,
)

@Serializable
data class TvProgramme(
    val title: String,
    val description: String = "",
    val image: String? = null,
    val start: String,
    val end: String,
    @SerialName("start_iso")
    val startIso: String,
    @SerialName("end_iso")
    val endIso: String,
    @SerialName("progress_percent")
    val progressPercent: Float = 0f,
)

@Serializable
data class HomeTvEpgResponse(
    val provider: String,
    val date: String,
    @SerialName("generated_at")
    val generatedAt: String,
    val timezone: String,
    val count: Int,
    val channels: List<EpgChannel>,
)

@Serializable
data class EpgChannel(
    val provider: String,
    val number: Int,
    val name: String,
    val logo: String? = null,
    @SerialName("catchup_days")
    val catchupDays: Int = 0,
    @SerialName("play_url")
    val playUrl: String,
    val programmes: List<EpgProgramme> = emptyList(),
)

@Serializable
data class EpgProgramme(
    val title: String,
    val description: String = "",
    val image: String? = null,
    val start: String,
    val end: String,
    @SerialName("start_iso")
    val startIso: String,
    @SerialName("end_iso")
    val endIso: String,
    @SerialName("progress_percent")
    val progressPercent: Float = 0f,
    val state: String,
)

/*
 * Malé pohyblivé EPG okno.
 *
 * Backend vrací pouze několik kanálů kolem vybraného kanálu
 * a několik pořadů kolem zvoleného času.
 */

@Serializable
data class HomeTvEpgWindowResponse(
    val provider: String,
    @SerialName("generated_at")
    val generatedAt: String,
    val timezone: String,
    val anchor: EpgWindowAnchor,
    @SerialName("channel_radius")
    val channelRadius: Int,
    @SerialName("programme_radius")
    val programmeRadius: Int,
    @SerialName("center_channel_index")
    val centerChannelIndex: Int,
    val count: Int,
    val channels: List<EpgWindowChannel>,
)

@Serializable
data class EpgWindowAnchor(
    val date: String,
    val time: String,
    val datetime: String,
    @SerialName("channel_number")
    val channelNumber: Int,
)

@Serializable
data class EpgWindowChannel(
    val provider: String,
    val number: Int,
    val name: String,
    val logo: String? = null,
    @SerialName("catchup_days")
    val catchupDays: Int = 0,
    @SerialName("play_url")
    val playUrl: String,
    @SerialName("anchor_programme_index")
    val anchorProgrammeIndex: Int? = null,
    val programmes: List<EpgProgramme> = emptyList(),
)

data class HomeTvPlayback(
    val title: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)

@kotlinx.serialization.Serializable
data class HomeTvProviderPlaybackResponse(
    val provider: String,
    @SerialName("provider_item_id")
    val providerItemId: String,
    val protocol: String,
    @SerialName("manifest_url")
    val manifestUrl: String,
    @SerialName("manifest_headers")
    val manifestHeaders: Map<String, String> = emptyMap(),
)
