package com.example.soundmodule

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(context: Context) {

    private val soundPool: SoundPool
    private val soundMap: HashMap<Int, Int>
    private val activeStreams: MutableList<Int> = mutableListOf()


    init {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        soundMap = HashMap()
        // Example sounds, add your own sound resource IDs here
        soundMap[R.raw.siz_liniyadasiz] = soundPool.load(context, R.raw.siz_liniyadasiz, 1)
        soundMap[R.raw.siz_oflaynsiz] = soundPool.load(context, R.raw.siz_oflaynsiz, 1)
        soundMap[R.raw.kettik] = soundPool.load(context, R.raw.kettik, 1)
        soundMap[R.raw.safar_boshlandi_xavfsizlik_kamari] =
            soundPool.load(context, R.raw.safar_boshlandi_xavfsizlik_kamari, 1)

    }

    private fun playSoundInternal(soundId: Int) {
        // Stop all active streams
        stopActiveStreams()
        // Play the new sound
        soundMap[soundId]?.let { sound ->
            val streamId = soundPool.play(sound, 1.0f, 1.0f, 1, 0, 1.0f)
            activeStreams.add(streamId)
        }
    }

    private fun stopActiveStreams() {
        for (streamId in activeStreams) {
            soundPool.stop(streamId)
        }
        activeStreams.clear()
    }

    fun playSoundYouAreOnline() {
        playSoundInternal(R.raw.siz_liniyadasiz)
    }

    fun playSoundYouAreOffline() {
        playSoundInternal(R.raw.siz_oflaynsiz)
    }

    fun playSoundJourneyBeginWithBelt() {
        playSoundInternal(R.raw.safar_boshlandi_xavfsizlik_kamari)
    }

    fun playSoundLetsGo() {
        playSoundInternal(R.raw.kettik)
    }
}