package com.example.gesturem

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*

class MainActivity : ComponentActivity() {
    private lateinit var musicController: MusicController
    private lateinit var database: DatabaseReference
    private lateinit var songList: List<Song>

    private val firebaseCommandPath = "gestures/command"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        musicController = MusicController(this)

        val permissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                initializeMusicApp()
            }
        }

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionRequest.launch(permission)
        } else {
            initializeMusicApp()
        }
    }

    private fun initializeMusicApp() {
        songList = loadLocalSongs()
        musicController.loadSongs(songList)
        setupRealtimeDatabaseListener()
        showUI()
    }

    private fun loadLocalSongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE
        )

        val cursor = contentResolver.query(musicUri, projection, null, null, null)
        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val title = it.getString(titleIndex)
                val uri = ContentUris.withAppendedId(musicUri, id)
                songs.add(Song(uri, title))
            }
        }
        return songs
    }

    private fun setupRealtimeDatabaseListener() {
        database = FirebaseDatabase.getInstance().getReference(firebaseCommandPath)

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val command = snapshot.getValue(String::class.java)?.trim()?.uppercase()
                Log.d("FirebaseCommand", "Received command: $command")

                when (command) {
                    "UP" -> musicController.play()
                    "DOWN" -> musicController.pause()
                    "RIGHT" -> musicController.next()
                    "LEFT" -> musicController.previous()
                    else -> return // ignore empty or unrecognized commands
                }

                // Clear the command after handling
                database.setValue("")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseCommand", "Database error: ${error.message}")
            }
        })
    }

    private fun showUI() {
        setContent {
            var nowPlaying by remember { mutableStateOf("") }

            // UI state sync with current song
            LaunchedEffect(Unit) {
                database.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val command = snapshot.getValue(String::class.java)?.uppercase()
                        if (command in listOf("UP", "RIGHT", "LEFT")) {
                            nowPlaying = musicController.currentSongTitle
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column {
                        Text(
                            text = "🎵 Songs from Device",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                        ) {
                            items(songList) { song ->
                                Text(song.title, style = MaterialTheme.typography.bodyLarge)
                                Divider()
                            }
                        }

                        if (nowPlaying.isNotEmpty()) {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    text = "Now Playing: $nowPlaying",
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
