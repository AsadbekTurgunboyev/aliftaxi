package com.example.taxi.utils

import android.content.Context
import android.media.MediaPlayer
import com.example.taxi.R

class SoundPlayer(private val soundType: SoundType, private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playSound() {
        val soundResId = when (soundType) {
            SoundType.LowSound -> R.raw.alif_message
            SoundType.MiddleSound -> R.raw.alif_message
            SoundType.RequestSound -> R.raw.alif_request
        }

        mediaPlayer = MediaPlayer.create(context, soundResId)
        mediaPlayer?.isLooping = false
        mediaPlayer?.start()
    }

    fun playRequestSound(){
        val soundResId = R.raw.alif_request
        mediaPlayer = MediaPlayer.create(context, soundResId)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    fun stopSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    enum class SoundType {
        LowSound,
        MiddleSound,
        RequestSound
    }


}