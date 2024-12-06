package com.example.spotifyclone.exoplayer

import android.app.PendingIntent
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.*
import androidx.media3.session.MediaSession.*
import com.example.spotifyclone.exoplayer.callbasks.MusicPlaybackPreparer
import com.example.spotifyclone.exoplayer.callbasks.MusicPlayerNotificationListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import javax.inject.Inject


private const val SERVICE_TAG = "MusicService"

@UnstableApi
@AndroidEntryPoint
class MusicService : MediaBrowserService() {

    @Inject
    lateinit var dataSourceFactory: DefaultDataSource.Factory

    @Inject
    lateinit var exoPlayer: ExoPlayer

    lateinit var firebaseMusicSource: FirebaseMusicSource

    private lateinit var musicNotificationManager: MusicNotificationManager

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSesstion: MediaSessionCompat
    private lateinit var mediaSessionConnector: androidx.media3.session.MediaSession

    var isForegroundService = false

    private var curPlayingSong: MediaMetadataCompat? = null

    override fun onCreate() {
        super.onCreate()
        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        mediaSesstion = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }

        sessionToken =
            (mediaSesstion.sessionToken as MediaSessionCompat.Token).token as MediaSession.Token?

        musicNotificationManager = MusicNotificationManager(
            this,
            mediaSesstion.sessionToken as MediaSessionCompat.Token,
            MusicPlayerNotificationListener(this)
        ) {
//            curSongDuration = exoPlayer.duration
        }

        val musicPlaybackPreparer = MusicPlaybackPreparer(firebaseMusicSource) {
            curPlayingSong = it
            preparerPlayer(
                firebaseMusicSource.songs, it, true
            )
        }

        mediaSessionConnector = Builder(this, ExoPlayer.Builder(this).build()).build()
        mediaSessionConnector = Builder(this, exoPlayer).setCallback(musicPlaybackPreparer).build()
        mediaSessionConnector.setPlayer(exoPlayer)
    }

    private fun preparerPlayer(
        song: List<MediaMetadataCompat>, itemToPlay: MediaMetadataCompat?, playNow: Boolean
    ) {
        val curSongIndex = if (curPlayingSong == null) 0 else song.indexOf(itemToPlay)
        exoPlayer.prepare()
        exoPlayer.seekTo(curSongIndex, 0L)
        exoPlayer.playWhenReady = playNow
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onGetRoot(
        clientPackageName: String, clientUid: Int, rootHints: Bundle?
    ): BrowserRoot? {
        TODO("Not yet implemented")
    }

    override fun onLoadChildren(
        parentId: String, result: Result<MutableList<MediaBrowser.MediaItem>>
    ) {
        TODO("Not yet implemented")
    }
}