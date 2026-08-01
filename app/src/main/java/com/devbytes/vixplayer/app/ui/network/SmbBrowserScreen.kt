package com.devbytes.vixplayer.app.ui.network

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.data.db.entity.NetworkServer
import com.devbytes.vixplayer.app.data.db.entity.ShareProtocol
import com.devbytes.vixplayer.app.data.network.ShareEntry
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmbBrowserScreen(
    initialPath: String? = null,
    onFileClick: (Uri) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: NetworkSharesViewModel = hiltViewModel(),
) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    val inBrowse = ui !is SharesUi.ServerList
    BackHandler(enabled = inBrowse) { viewModel.goUp() }

    val title = when (val s = ui) {
        is SharesUi.Browsing -> s.path.substringAfterLast('/').ifEmpty { s.server.displayName }
        is SharesUi.Connecting -> "Connecting…"
        is SharesUi.Error -> s.server.displayName
        SharesUi.ServerList -> "Network shares"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { if (inBrowse) viewModel.goUp() else onBack() }) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Back")
                    }
                },
                actions = {
                    if (ui is SharesUi.ServerList) {
                        TextButton(onClick = { showAdd = true }) { Text("Add") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        val mod = Modifier.padding(padding)
        when (val s = ui) {
            SharesUi.ServerList ->
                ServerList(servers, mod, onConnect = viewModel::connect, onRemove = viewModel::removeServer, onAdd = { showAdd = true })

            SharesUi.Connecting ->
                Box(mod.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

            is SharesUi.Browsing -> EntryList(s.entries, mod) { entry ->
                if (entry.isDirectory) viewModel.openDirectory(entry)
                else entry.playUri?.let { onFileClick(Uri.parse(it)) }
            }

            is SharesUi.Error -> LibraryEmptyState(
                iconRes = R.drawable.ic_info,
                title = "Can't open share",
                body = s.message,
                primaryLabel = "Retry",
                onPrimary = viewModel::retry,
                secondaryLabel = "Back",
                onSecondary = viewModel::backToServers,
                modifier = mod,
            )
        }
    }

    if (showAdd) {
        AddServerDialog(
            onDismiss = { showAdd = false },
            onAdd = { protocol, host, port, share, path, guest, user, pass, domain ->
                viewModel.addServer(protocol, host, port, share, path, guest, user, pass, domain)
                showAdd = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerList(
    servers: List<NetworkServer>,
    modifier: Modifier,
    onConnect: (NetworkServer) -> Unit,
    onRemove: (NetworkServer) -> Unit,
    onAdd: () -> Unit,
) {
    if (servers.isEmpty()) {
        LibraryEmptyState(
            iconRes = R.drawable.ic_folder,
            title = "No servers yet",
            body = "Add an SMB or FTP server to browse and play videos over your network.",
            primaryLabel = "Add server",
            onPrimary = onAdd,
            modifier = modifier,
        )
        return
    }
    LazyColumn(modifier.fillMaxSize()) {
        items(servers, key = { it.id }) { server ->
            ListItem(
                leadingContent = {
                    Icon(
                        painterResource(R.drawable.ic_folder),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                headlineContent = { Text(server.displayName) },
                supportingContent = {
                    Text(
                        "${server.protocol} · ${if (server.isGuest) "Guest" else server.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingContent = {
                    IconButton(onClick = { onRemove(server) }) {
                        Icon(
                            painterResource(R.drawable.ic_close),
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                modifier = Modifier.clickable { onConnect(server) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun EntryList(entries: List<ShareEntry>, modifier: Modifier, onClick: (ShareEntry) -> Unit) {
    if (entries.isEmpty()) {
        LibraryEmptyState(
            iconRes = R.drawable.ic_folder,
            title = "Empty folder",
            body = "No folders or playable videos here.",
            modifier = modifier,
        )
        return
    }
    LazyColumn(modifier.fillMaxSize()) {
        items(entries, key = { it.name + it.isDirectory }) { entry ->
            ListItem(
                leadingContent = {
                    Icon(
                        painterResource(if (entry.isDirectory) R.drawable.ic_folder else R.drawable.ic_nav_video),
                        contentDescription = null,
                        tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                headlineContent = { Text(entry.name, maxLines = 1) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                modifier = Modifier.clickable { onClick(entry) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun AddServerDialog(
    onDismiss: () -> Unit,
    onAdd: (ShareProtocol, String, Int, String, String, Boolean, String, String, String) -> Unit,
) {
    var protocol by remember { mutableStateOf(ShareProtocol.SMB) }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var share by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("") }
    var guest by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }

    val isSmb = protocol == ShareProtocol.SMB
    val valid = host.isNotBlank() && (!isSmb || share.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add server") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isSmb, onClick = { protocol = ShareProtocol.SMB }, label = { Text("SMB") })
                    FilterChip(selected = !isSmb, onClick = { protocol = ShareProtocol.FTP }, label = { Text("FTP") })
                }
                Field("Host / IP", host) { host = it }
                Field("Port (optional)", port) { port = it.filter(Char::isDigit) }
                if (isSmb) Field("Share name", share) { share = it }
                Field("Path (optional)", path) { path = it }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isSmb) "Guest access" else "Anonymous", Modifier.weight(1f))
                    Switch(checked = guest, onCheckedChange = { guest = it })
                }
                if (!guest) {
                    Field("Username", username) { username = it }
                    Field("Password", password, isPassword = true) { password = it }
                    if (isSmb) Field("Domain (optional)", domain) { domain = it }
                }
                if (!isSmb) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "FTP sends credentials unencrypted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onAdd(protocol, host, port.toIntOrNull() ?: 0, share, path, guest, username, password, domain)
                },
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}
