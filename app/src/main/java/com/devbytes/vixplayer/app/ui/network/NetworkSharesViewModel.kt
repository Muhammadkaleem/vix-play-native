package com.devbytes.vixplayer.app.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devbytes.vixplayer.app.data.db.entity.NetworkServer
import com.devbytes.vixplayer.app.data.db.entity.ShareProtocol
import com.devbytes.vixplayer.app.data.network.NetworkServerRepository
import com.devbytes.vixplayer.app.data.network.ShareEntry
import com.devbytes.vixplayer.app.data.network.ShareException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SharesUi {
    data object ServerList : SharesUi
    data object Connecting : SharesUi
    data class Browsing(
        val server: NetworkServer,
        val path: String,
        val entries: List<ShareEntry>,
    ) : SharesUi
    data class Error(val message: String, val server: NetworkServer, val path: String) : SharesUi
}

@HiltViewModel
class NetworkSharesViewModel @Inject constructor(
    private val repository: NetworkServerRepository,
) : ViewModel() {

    val servers: StateFlow<List<NetworkServer>> = repository.observeServers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow<SharesUi>(SharesUi.ServerList)
    val ui: StateFlow<SharesUi> = _ui.asStateFlow()

    fun connect(server: NetworkServer) = browse(server, server.path.trim('/'))

    fun openDirectory(entry: ShareEntry) {
        val current = _ui.value as? SharesUi.Browsing ?: return
        val next = if (current.path.isEmpty()) entry.name else "${current.path}/${entry.name}"
        browse(current.server, next)
    }

    /** Up one level; at the share root, returns to the server list. */
    fun goUp() {
        val current = _ui.value as? SharesUi.Browsing ?: run { _ui.value = SharesUi.ServerList; return }
        val root = current.server.path.trim('/')
        if (current.path == root) {
            _ui.value = SharesUi.ServerList
        } else {
            browse(current.server, current.path.substringBeforeLast('/', ""))
        }
    }

    fun backToServers() { _ui.value = SharesUi.ServerList }

    fun retry() {
        val err = _ui.value as? SharesUi.Error ?: return
        browse(err.server, err.path)
    }

    private fun browse(server: NetworkServer, path: String) {
        _ui.value = SharesUi.Connecting
        viewModelScope.launch {
            runCatching { repository.list(server, path) }
                .onSuccess { _ui.value = SharesUi.Browsing(server, path, it) }
                .onFailure { e ->
                    val msg = (e as? ShareException)?.message ?: "Couldn't open this share."
                    _ui.value = SharesUi.Error(msg, server, path)
                }
        }
    }

    fun addServer(
        protocol: ShareProtocol,
        host: String,
        port: Int,
        share: String,
        path: String,
        isGuest: Boolean,
        username: String,
        password: String,
        domain: String,
    ) {
        val label = buildString {
            append(host.trim())
            if (protocol == ShareProtocol.SMB && share.isNotBlank()) append("/${share.trim()}")
        }
        val server = NetworkServer(
            protocol = protocol.name,
            host = host.trim(),
            port = port,
            share = share.trim(),
            path = path.trim().trim('/'),
            username = if (isGuest) "" else username.trim(),
            domain = if (isGuest) "" else domain.trim(),
            isGuest = isGuest,
            displayName = label,
            addedAt = System.currentTimeMillis(),
        )
        viewModelScope.launch { repository.add(server, if (isGuest) null else password) }
    }

    fun removeServer(server: NetworkServer) {
        viewModelScope.launch { repository.remove(server) }
    }
}
