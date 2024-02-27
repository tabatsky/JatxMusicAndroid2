package jatx.musictransmitter.android.domain

import java.io.File

interface Settings {
    var currentMusicDirPath: String
    var currentFileList: List<File>
    var volume: Int
    var isShuffle: Boolean
    var isLocalMode: Boolean
}