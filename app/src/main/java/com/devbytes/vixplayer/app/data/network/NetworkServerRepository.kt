package com.devbytes.vixplayer.app.data.network

import com.devbytes.vixplayer.app.data.db.dao.NetworkServerDao
import com.devbytes.vixplayer.app.data.db.entity.NetworkServer
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkServerRepository @Inject constructor(
    private val dao: NetworkServerDao,
    private val credentials: CredentialStore,
    private val browser: NetworkShareBrowser,
) {
    fun observeServers(): Flow<List<NetworkServer>> = dao.observeServers()

    /** Saves a server; stores the password (if any) encrypted, never in Room. */
    suspend fun add(server: NetworkServer, password: String?) {
        val id = dao.insert(server)
        if (!server.isGuest && !password.isNullOrEmpty()) credentials.setPassword(id, password)
    }

    suspend fun remove(server: NetworkServer) {
        dao.delete(server)
        credentials.removePassword(server.id)
    }

    fun passwordFor(server: NetworkServer): String? =
        if (server.isGuest) null else credentials.getPassword(server.id)

    /** Lists a share path, resolving the stored password internally. */
    suspend fun list(server: NetworkServer, path: String): List<ShareEntry> =
        browser.list(server, passwordFor(server), path)
}
