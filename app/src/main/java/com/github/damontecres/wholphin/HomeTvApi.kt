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
        val request = Request.Builder()
            .url("$BASE_URL/api/v1/live/now")
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HomeTV API HTTP ${response.code}")
            }

            val body = response.body.string()

            return json.decodeFromString<HomeTvLiveResponse>(body)
        }
    }

    companion object {
        private const val BASE_URL = "http://192.168.0.175:8090"
    }
}
