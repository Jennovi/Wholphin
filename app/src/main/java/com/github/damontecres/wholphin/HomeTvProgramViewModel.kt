package com.github.damontecres.wholphin

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.wholphin.services.hilt.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeTvProgramUiState(
    val loading: Boolean = true,
    val date: String? = null,
    val channels: List<EpgChannel> = emptyList(),
    val error: String? = null,

    // Střed právě načteného 2D okna.
    val anchorChannelNumber: Int = 4,
    val anchorTime: String? = null,
)

private data class CachedEpgWindow(
    val response: HomeTvEpgWindowResponse,
    val cachedAt: Long,
)

@HiltViewModel
class HomeTvProgramViewModel @Inject constructor(
    private val homeTvApi: HomeTvApi,
    @IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    companion object {
        private const val TAG = "HomeTV-EPG"

        private const val CACHE_TTL_MS = 5 * 60 * 1000L

        private const val DEFAULT_CHANNEL = 4
        private const val CHANNEL_RADIUS = 3
        private const val PROGRAMME_RADIUS = 10

        private val HOME_TV_ZONE =
            ZoneId.of("Europe/Prague")

        private val TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm")
    }

    var uiState by mutableStateOf(HomeTvProgramUiState())
        private set

    /*
     * Cache už neobsahuje celé dny.
     *
     * Klíč:
     * datum | čas | prostřední kanál
     *
     * Např.:
     * 2026-10-06|03:15|4
     */
    private val windowCache =
        mutableMapOf<String, CachedEpgWindow>()

    private var loadingKey: String? = null

    private val prefetchingKeys =
        mutableSetOf<String>()

    init {
        initialLoad()
    }

    /*
     * Při otevření Programu načteme pouze jedno malé okno
     * kolem aktuálního času.
     *
     * Žádné:
     * - včera 180 kanálů
     * - dnes 180 kanálů
     * - zítra 180 kanálů
     */
    private fun initialLoad() {
        val now = java.time.ZonedDateTime.now(HOME_TV_ZONE)

        val date = now.toLocalDate().toString()
        val time = now.toLocalTime()
            .withSecond(0)
            .withNano(0)
            .format(TIME_FORMATTER)

        Log.d(
            TAG,
            "INITIAL WINDOW date=$date time=$time channel=$DEFAULT_CHANNEL",
        )

        loadWindow(
            date = date,
            time = time,
            channelNumber = DEFAULT_CHANNEL,
            showLoading = true,
        )
    }

    /*
     * Zachováváme load(date), protože ho zatím používá
     * současný HomeTvProgramScreen.
     *
     * Na rozdíl od staré verze ale nestahuje celý den.
     */
    fun load(date: String? = null) {
        val requestedDate =
            date ?: LocalDate.now(HOME_TV_ZONE).toString()

        val time =
            uiState.anchorTime ?: currentTimeString()

        val channel =
            uiState.anchorChannelNumber

        loadWindow(
            date = requestedDate,
            time = time,
            channelNumber = channel,
            showLoading = uiState.channels.isEmpty(),
        )
    }

    /*
     * Toto bude hlavní metoda pro nový ProgramScreen.
     *
     * Obrazovka jí může říct:
     * "potřebuji okolí kanálu 12 kolem 20:30".
     */
    fun moveWindow(
        channelNumber: Int,
        date: String,
        time: String,
    ) {
        loadWindow(
            date = date,
            time = time,
            channelNumber = channelNumber,
            showLoading = false,
        )
    }

    /*
     * Tiché obnovení aktuálního okna.
     *
     * Později ho použijeme pro pravidelnou aktualizaci.
     */
    fun refreshCurrentWindow() {
        val date = uiState.date ?: return
        val time = uiState.anchorTime ?: return
        val channel = uiState.anchorChannelNumber

        val key = cacheKey(
            date = date,
            time = time,
            channelNumber = channel,
        )

        windowCache.remove(key)

        loadWindow(
            date = date,
            time = time,
            channelNumber = channel,
            showLoading = false,
        )
    }

    /*
     * Dočasně zachováváme tlačítka předchozí/další den
     * kvůli kompatibilitě se současnou obrazovkou.
     *
     * Po úpravě ProgramScreen už přechod přes půlnoc
     * nebude řešen jako speciální operace.
     */
    fun previousDay() {
        val currentDate =
            uiState.date?.let(LocalDate::parse)
                ?: LocalDate.now(HOME_TV_ZONE)

        loadWindow(
            date = currentDate.minusDays(1).toString(),
            time = uiState.anchorTime ?: currentTimeString(),
            channelNumber = uiState.anchorChannelNumber,
            showLoading = false,
        )
    }

    fun nextDay() {
        val currentDate =
            uiState.date?.let(LocalDate::parse)
                ?: LocalDate.now(HOME_TV_ZONE)

        loadWindow(
            date = currentDate.plusDays(1).toString(),
            time = uiState.anchorTime ?: currentTimeString(),
            channelNumber = uiState.anchorChannelNumber,
            showLoading = false,
        )
    }

    fun today() {
        val now = java.time.ZonedDateTime.now(HOME_TV_ZONE)

        loadWindow(
            date = now.toLocalDate().toString(),
            time = now.toLocalTime()
                .withSecond(0)
                .withNano(0)
                .format(TIME_FORMATTER),
            channelNumber = uiState.anchorChannelNumber,
            showLoading = false,
        )
    }

    /*
     * Tiché přednačtení sousedního 2D okna.
     *
     * Použije se při pohybu kurzoru ještě dříve,
     * než uživatel skutečně dojede na okraj.
     *
     * Důležité:
     * - nemění zobrazený Program
     * - nezobrazuje loading
     * - pouze připraví data do windowCache
     */
    fun prefetchWindow(
        channelNumber: Int,
        date: String,
        time: String,
    ) {
        val key = cacheKey(
            date = date,
            time = time,
            channelNumber = channelNumber,
        )

        val cached = windowCache[key]

        if (cached != null && !isStale(cached)) {
            Log.d(TAG, "WINDOW PREFETCH HIT $key")
            return
        }

        if (!prefetchingKeys.add(key)) {
            Log.d(TAG, "WINDOW PREFETCH ALREADY RUNNING $key")
            return
        }

        Log.d(TAG, "WINDOW PREFETCH START $key")

        viewModelScope.launch {
            try {
                val response = withContext(ioDispatcher) {
                    homeTvApi.getEpgWindow(
                        channelNumber = channelNumber,
                        date = date,
                        time = time,
                        channelRadius = CHANNEL_RADIUS,
                        programmeRadius = PROGRAMME_RADIUS,
                    )
                }

                putInCache(
                    key = key,
                    response = response,
                )

                trimCache()

                Log.d(
                    TAG,
                    "WINDOW PREFETCH DONE $key -> " +
                        "${response.channels.size} channels",
                )
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "WINDOW PREFETCH ERROR $key",
                    exception,
                )
            } finally {
                prefetchingKeys.remove(key)
            }
        }
    }

    private fun loadWindow(
        date: String,
        time: String,
        channelNumber: Int,
        showLoading: Boolean,
    ) {
        val key = cacheKey(
            date = date,
            time = time,
            channelNumber = channelNumber,
        )

        val cached = windowCache[key]

        if (cached != null && !isStale(cached)) {
            Log.d(TAG, "WINDOW CACHE HIT $key")

            showWindow(cached.response)
            return
        }

        if (loadingKey == key) {
            Log.d(TAG, "WINDOW ALREADY LOADING $key")
            return
        }

        Log.d(TAG, "WINDOW FETCH $key")

        loadingKey = key

        viewModelScope.launch {
            if (showLoading) {
                uiState = uiState.copy(
                    loading = true,
                    error = null,
                )
            }

            try {
                val response = withContext(ioDispatcher) {
                    homeTvApi.getEpgWindow(
                        channelNumber = channelNumber,
                        date = date,
                        time = time,
                        channelRadius = CHANNEL_RADIUS,
                        programmeRadius = PROGRAMME_RADIUS,
                    )
                }

                Log.d(
                    TAG,
                    "WINDOW FETCH OK $key -> " +
                        "${response.channels.size} channels",
                )

                putInCache(
                    key = key,
                    response = response,
                )

                showWindow(response)

                trimCache()
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "WINDOW FETCH ERROR $key",
                    exception,
                )

                uiState = uiState.copy(
                    loading = false,
                    error = exception.message ?: "Neznámá chyba",
                )
            } finally {
                if (loadingKey == key) {
                    loadingKey = null
                }
            }
        }
    }

    /*
     * Window model převedeme na původní EpgChannel.
     *
     * Díky tomu nemusíme v tomto kroku rozbít
     * současný HomeTvProgramScreen.
     */
    private fun showWindow(
        response: HomeTvEpgWindowResponse,
    ) {
        val channels = response.channels.map { channel ->
            EpgChannel(
                provider = channel.provider,
                number = channel.number,
                name = channel.name,
                logo = channel.logo,
                catchupDays = channel.catchupDays,
                playUrl = channel.playUrl,
                programmes = channel.programmes,
            )
        }

        uiState = HomeTvProgramUiState(
            loading = false,
            date = response.anchor.date,
            channels = channels,
            error = null,
            anchorChannelNumber =
                response.anchor.channelNumber,
            anchorTime =
                response.anchor.time,
        )

        Log.d(
            TAG,
            "WINDOW SHOW " +
                "channel=${response.anchor.channelNumber} " +
                "date=${response.anchor.date} " +
                "time=${response.anchor.time} " +
                "channels=${channels.size}",
        )
    }

    private fun putInCache(
        key: String,
        response: HomeTvEpgWindowResponse,
    ) {
        windowCache[key] = CachedEpgWindow(
            response = response,
            cachedAt = System.currentTimeMillis(),
        )

        Log.d(
            TAG,
            "WINDOW CACHE STORE $key size=${windowCache.size}",
        )
    }

    /*
     * Zatím držíme maximálně 20 malých oken.
     *
     * I 20 těchto oken je nesrovnatelně menší objem
     * než několik kompletních 180kanálových dnů.
     */
    private fun trimCache() {
        if (windowCache.size <= 20) {
            return
        }

        val oldestKeys = windowCache.entries
            .sortedBy { it.value.cachedAt }
            .take(windowCache.size - 20)
            .map { it.key }

        oldestKeys.forEach { key ->
            windowCache.remove(key)
            Log.d(TAG, "WINDOW CACHE REMOVE $key")
        }
    }

    private fun cacheKey(
        date: String,
        time: String,
        channelNumber: Int,
    ): String {
        return "$date|$time|$channelNumber"
    }

    private fun isStale(
        cached: CachedEpgWindow,
    ): Boolean {
        return System.currentTimeMillis() - cached.cachedAt >
            CACHE_TTL_MS
    }

    private fun currentTimeString(): String {
        return LocalTime.now(HOME_TV_ZONE)
            .withSecond(0)
            .withNano(0)
            .format(TIME_FORMATTER)
    }
}



