package com.devbytes.vixplayer.app.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.db.entity.NetworkStream
import com.devbytes.vixplayer.app.data.repository.NetworkStreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkStreamViewModel @Inject constructor(
    private val repository: NetworkStreamRepository,
) : ViewModel() {

    val recents: StateFlow<List<NetworkStream>> = repository.observeRecents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Records a URL just handed to the player so it appears in recents. */
    fun onPlayed(url: String) {
        viewModelScope.launch { repository.record(url) }
    }

    fun remove(stream: NetworkStream) {
        viewModelScope.launch { repository.remove(stream) }
    }
}
