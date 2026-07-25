package com.devbytes.vixplayer.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.devbytes.vixplayer.app.R
import com.devbytes.vixplayer.app.data.repository.PrivateItem
import com.devbytes.vixplayer.app.ui.library.components.LibraryEmptyState

/**
 * PIN-gated private folder. See [com.devbytes.vixplayer.app.data.repository.PrivateVault]
 * for why "private" means an app-private vault rather than hidden-in-place.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateFolderScreen(
    onBack: () -> Unit = {},
    onPlay: (String) -> Unit = {},
    viewModel: PrivateFolderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Leaving the screen re-locks it, so returning always needs the PIN.
    BackHandler { viewModel.lock(); onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Private Folder") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.lock(); onBack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (state.mode) {
                VaultMode.SETUP -> PinSetup(
                    error = state.error,
                    onSubmit = viewModel::submitNewPin,
                    onEdit = viewModel::clearError,
                )
                VaultMode.SHOW_RECOVERY -> RecoveryCode(
                    code = state.recoveryCode.orEmpty(),
                    onDone = viewModel::acknowledgeRecovery,
                )
                VaultMode.LOCKED -> PinEntry(
                    title = "Enter PIN",
                    error = state.error,
                    onSubmit = viewModel::unlock,
                    onForgot = viewModel::startRecovery,
                    onEdit = viewModel::clearError,
                )
                VaultMode.RECOVERY -> RecoveryEntry(
                    error = state.error,
                    onSubmit = viewModel::submitRecovery,
                    onCancel = viewModel::cancelRecovery,
                    onEdit = viewModel::clearError,
                )
                VaultMode.UNLOCKED -> VaultGrid(
                    items = state.items,
                    onPlay = { onPlay(it.uri.toString()) },
                    onUnhide = viewModel::unhide,
                    onDelete = viewModel::delete,
                )
            }
        }
    }
}

@Composable
private fun PinSetup(error: String?, onSubmit: (String, String) -> Unit, onEdit: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    GateColumn {
        Text(
            "Set a PIN",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "Videos you move here are hidden from the library and gallery until you unlock.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PinField(pin, "New PIN") { pin = it; onEdit() }
        PinField(confirm, "Confirm PIN") { confirm = it; onEdit() }
        error?.let { ErrorText(it) }
        Button(onClick = { onSubmit(pin, confirm) }, enabled = pin.isNotEmpty()) {
            Text("Set PIN")
        }
    }
}

@Composable
private fun RecoveryCode(code: String, onDone: () -> Unit) {
    GateColumn {
        Icon(
            painter = painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            "Save your recovery code",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "This is the only way back in if you forget your PIN. It won't be shown again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = code,
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        )
        Button(onClick = onDone) { Text("I've saved it") }
    }
}

@Composable
private fun PinEntry(
    title: String,
    error: String?,
    onSubmit: (String) -> Unit,
    onForgot: () -> Unit,
    onEdit: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    GateColumn {
        Icon(
            painter = painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        PinField(pin, "PIN") { pin = it; onEdit() }
        error?.let { ErrorText(it) }
        Button(onClick = { onSubmit(pin) }, enabled = pin.isNotEmpty()) { Text("Unlock") }
        TextButton(onClick = onForgot) { Text("Forgot PIN?") }
    }
}

@Composable
private fun RecoveryEntry(
    error: String?,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
    onEdit: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    GateColumn {
        Text("Enter recovery code", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "The code shown when you set your PIN. You'll then choose a new PIN.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedTextField(
            value = code,
            onValueChange = { code = it; onEdit() },
            singleLine = true,
            label = { Text("Recovery code") },
        )
        error?.let { ErrorText(it) }
        Button(onClick = { onSubmit(code) }, enabled = code.isNotBlank()) { Text("Continue") }
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Composable
private fun VaultGrid(
    items: List<PrivateItem>,
    onPlay: (PrivateItem) -> Unit,
    onUnhide: (PrivateItem) -> Unit,
    onDelete: (PrivateItem) -> Unit,
) {
    if (items.isEmpty()) {
        LibraryEmptyState(
            iconRes = R.drawable.ic_lock,
            title = "Nothing hidden yet",
            body = "Select videos in the library and choose \"Move to private\".",
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.file.path }) { item ->
            VaultCard(item = item, onPlay = { onPlay(item) }, onUnhide = { onUnhide(item) }, onDelete = { onDelete(item) })
        }
    }
}

@Composable
private fun VaultCard(
    item: PrivateItem,
    onPlay: () -> Unit,
    onUnhide: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onPlay),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.uri)
                    .decoderFactory(VideoFrameDecoder.Factory())
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(100.dp),
            )
            IconButton(onClick = { menu = true }, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Unhide") }, onClick = { menu = false; onUnhide() })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
            }
        }
        Text(
            text = item.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun GateColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) { content() }
}

@Composable
private fun PinField(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) onChange(it) },
        singleLine = true,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    )
}

@Composable
private fun ErrorText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}
