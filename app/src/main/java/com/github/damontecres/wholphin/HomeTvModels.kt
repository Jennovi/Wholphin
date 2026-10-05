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
