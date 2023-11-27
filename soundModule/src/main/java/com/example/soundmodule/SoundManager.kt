package com.example.soundmodule

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.util.Calendar

class SoundManager(context: Context) {

    private val soundPool: SoundPool
    private val soundMap: HashMap<Int, Int>
    private val activeStreams: MutableList<Int> = mutableListOf()

    private val preferences by lazy {
        context.getSharedPreferences(
            "SoundManagerPrefs",
            Context.MODE_PRIVATE
        )
    }

    // Cache the last online date to minimize SharedPreferences access.
    private var lastOnlineDate: Long
        get() = preferences.getLong("lastOnlineDate", 0)
        set(value) = preferences.edit().putLong("lastOnlineDate", value).apply()

    // Use a lazy initializer for the calendar to avoid unnecessary instances.
    private val currentDate by lazy { Calendar.getInstance() }


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

        soundMap[R.raw.siz_yetib_keldingiz] =
            soundPool.load(context, R.raw.siz_yetib_keldingiz, 1)

        soundMap[R.raw.siz_liniyadasiz_kuningiz] =
            soundPool.load(context, R.raw.siz_liniyadasiz_kuningiz, 1)


    }

    fun playSoundBasedOnFirstOnline() {
        currentDate.timeInMillis =
            System.currentTimeMillis() // Ensure the calendar is up to date when this method is called.

        if (isFirstTimeOnlineToday()) {
            lastOnlineDate =
                currentDate.timeInMillis // Update the cached and persistent last online date.
            playSoundYouAreOnlineGoodDay()
        } else {
            playSoundYouAreOnline()
        }
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


    private fun playSoundYouAreOnline() {
        playSoundInternal(R.raw.siz_liniyadasiz)
    }

    fun playSoundYouAreOffline() {
        playSoundInternal(R.raw.siz_oflaynsiz)
    }

    fun playSoundJourneyBeginWithBelt() {
        playSoundInternal(R.raw.safar_boshlandi_xavfsizlik_kamari)
    }

    fun playSoundArrivedToDestination() {
        playSoundInternal(R.raw.siz_yetib_keldingiz)
    }

    private fun playSoundYouAreOnlineGoodDay() {
        playSoundInternal(R.raw.siz_liniyadasiz_kuningiz)
    }

    fun playSoundLetsGo() {
        playSoundInternal(R.raw.kettik)
    }


    private fun isFirstTimeOnlineToday(): Boolean {
        // Convert the lastOnlineDate to a Calendar instance.
        val lastDateCalendar = Calendar.getInstance().apply {
            timeInMillis = lastOnlineDate
        }
        // Compare the current date and the last online date.
        return lastDateCalendar.get(Calendar.YEAR) != currentDate.get(Calendar.YEAR) ||
                lastDateCalendar.get(Calendar.DAY_OF_YEAR) != currentDate.get(Calendar.DAY_OF_YEAR)
    }
}