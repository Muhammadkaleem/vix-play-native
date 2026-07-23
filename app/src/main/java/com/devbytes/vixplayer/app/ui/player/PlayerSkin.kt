package com.devbytes.vixplayer.app.ui.player

sealed class PlayerSkin {
    data object Classic : PlayerSkin()
    data object Minimal : PlayerSkin()
    data object Cinema : PlayerSkin()
    data object Pro     : PlayerSkin()
}
