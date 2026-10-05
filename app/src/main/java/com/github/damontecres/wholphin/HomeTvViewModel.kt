package com.github.damontecres.wholphin

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
import javax.inject.Inject

data class HomeTvUiState(
    val loading: Boolean = true,
    val channels: List<LiveChannel> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class HomeTvViewModel @Inject constructor(
    private val homeTvApi: HomeTvApi,
    @IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    var uiState by mutableStateOf(HomeTvUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(
                loading = true,
                error = null,
            )

            try {
                val response = withContext(ioDispatcher) {
                    homeTvApi.getLiveNow()
                }

                uiState = HomeTvUiState(
                    loading = false,
                    channels = response.channels
                        .filter { it.current != null }
                        .take(15),
                )
            } catch (exception: Exception) {
                uiState = HomeTvUiState(
                    loading = false,
                    error = exception.message ?: "Neznámá chyba",
                )
            }
        }
    }
}
