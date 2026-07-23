package com.devbytes.vixplayer.app.ui.player

sealed class PlayerSheet {
    object Subtitles : PlayerSheet()
    object Audio     : PlayerSheet()
    object Speed     : PlayerSheet()
    object SleepTimer : PlayerSheet()
    object Cast      : PlayerSheet()
    object FileInfo  : PlayerSheet()
}
