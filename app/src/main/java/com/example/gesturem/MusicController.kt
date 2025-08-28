package com.example.gesturem

import android.content.Context
import android.media.MediaPlayer

class MusicController(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentIndex = 0
    private var songs: List<Song> = listOf()
    var currentSongTitle: String = ""
        private set

    fun loadSongs(songList: List<Song>) {
        songs = songList
    }

    fun getSongs(): List<Song> = songs

    fun play() {
        if (songs.isEmpty()) return
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, songs[currentIndex].uri)
        }
        currentSongTitle = songs[currentIndex].title
        mediaPlayer?.start()
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun next() {
        if (songs.isEmpty()) return
        currentIndex = (currentIndex + 1) % songs.size
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, songs[currentIndex].uri)
        currentSongTitle = songs[currentIndex].title
        mediaPlayer?.start()
    }

    fun previous() {
        if (songs.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) songs.size - 1 else currentIndex - 1
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, songs[currentIndex].uri)
        currentSongTitle = songs[currentIndex].title
        mediaPlayer?.start()
    }
}
