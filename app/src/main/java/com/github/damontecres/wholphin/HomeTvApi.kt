package com.github.damontecres.wholphin

import com.github.damontecres.wholphin.services.hilt.StandardOkHttpClient
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeTvApi @Inject constructor(
    @StandardOkHttpClient
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun getLiveNow(): HomeTvLiveResponse {
        val body = get("$BASE_URL/api/v1/live/now")
        return json.decodeFromString<HomeTvLiveResponse>(body)
    }

    /*
     * Původní celodenní EPG.
     * Zatím ho ponecháváme kvůli současné obrazovce Program.
     */
    fun getEpg(date: String? = null): HomeTvEpgResponse {
        val url = if (date == null) {
            "$BASE_URL/api/v1/epg"
        } else {
            "$BASE_URL/api/v1/epg?date=$date"
        }

        val body = get(url)
        return json.decodeFromString<HomeTvEpgResponse>(body)
    }

    /*
     * Nové malé 2D EPG okno.
     *
     * Standardně:
     *   3 kanály nad
     *   vybraný kanál
     *   3 kanály pod
     *
     * a až:
     *   3 pořady před
     *   vybraný pořad
     *   3 pořady po
     */
    fun getEpgWindow(
        channelNumber: Int,
        date: String,
        time: String,
        channelRadius: Int = 3,
        programmeRadius: Int = 3,
    ): HomeTvEpgWindowResponse {
        val url =
            "$BASE_URL/api/v1/epg/window" +
                "?channel_number=$channelNumber" +
                "&date=$date" +
                "&time=$time" +
                "&channel_radius=$channelRadius" +
                "&programme_radius=$programmeRadius"

        val body = get(url)

        return json.decodeFromString<HomeTvEpgWindowResponse>(body)
    }
    fun getProviderPlayback(
        provider: String,
        providerItemId: String,
    ): HomeTvProviderPlaybackResponse {
        val request =
            Request.Builder()
                .url("$BASE_URL/api/v1/playback/$provider/$providerItemId")
                .post(okhttp3.RequestBody.create(null, ByteArray(0)))
                .header("Authorization", "Bearer ${BuildConfig.HOMETV_API_TOKEN}")
                .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HomeTV playback API HTTP ${response.code}")
            }

            return json.decodeFromString<HomeTvProviderPlaybackResponse>(
                response.body.string(),
            )
        }
    }
    private fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HomeTV API HTTP ${response.code}")
            }

            return response.body.string()
        }
    }

    companion object {
        private const val BASE_URL = "http://192.168.0.175:8090"
    }
}
