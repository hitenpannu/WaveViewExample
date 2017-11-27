package com.hitenderpannu.audioview

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

class Playback constructor(rawDatafile: File, private val listener: AudioDataListener) {

  private var samplesToPlay: ShortBuffer? = null
  private var numberOfSamples = 0
  private var shouldContinue = false
  private var playbackThread: Thread? = null

  init {
    val samples = getSamplesFromFile(rawDatafile)
    numberOfSamples = samples.size
    samplesToPlay = ShortBuffer.wrap(samples)
  }

  @Throws(IOException::class)
  private fun getSamplesFromFile(file: File): ShortArray {
    try {
      val inputStream = FileInputStream(file)
      val data = kotlin.ByteArray(file.length().toInt())
      inputStream.read(data)
      inputStream.close()

      val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
      val samples = ShortArray(buffer.limit())
      buffer.get(samples)
      return samples
    } catch (exception: IOException) {
      Log.e("PLAYBACK ", exception.message)
      throw exception
    }
  }

  fun stopPlaying() {
    if (playbackThread == null) return
    shouldContinue = false
    playbackThread = null
  }

  fun startPlaying() {
    if (playbackThread != null) return
    shouldContinue = true
    playbackThread = Thread(Runnable { startPlaybackThread() })
    playbackThread?.start()
  }

  private fun startPlaybackThread() {
    Log.e("TAG", "Started playing")
    var bufferSize = AudioTrack.getMinBufferSize(Config.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
        Config.AUDIO_ENCODER)

    if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
      bufferSize = Config.SAMPLE_RATE.times(Config.BYTE_TO_SHORT_CONVERSION_FACTOR)
    }

    val audioTrack = AudioTrack(AudioManager.STREAM_MUSIC, Config.SAMPLE_RATE,
        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize,
        AudioTrack.MODE_STREAM)

    audioTrack.play()

    val buffer = ShortArray(bufferSize)
    samplesToPlay?.let {
      it.rewind()
      val limit = numberOfSamples
      var totalWritten = 0;
      while (it.position() < limit && shouldContinue) {
        val samplesLeft = limit - it.position()
        var samplesToWrite = 0
        if (samplesLeft >= buffer.size) {
          it.get(buffer)
          samplesToWrite = buffer.size
        } else {
          for (i in numberOfSamples..(buffer.size - 1)) {
            buffer[i] = 0
          }
          it.get(buffer, 0, samplesLeft)
          samplesToWrite = samplesLeft
        }

        listener.onDataReceived(buffer)
        totalWritten += samplesToWrite
        audioTrack.write(buffer, 0, samplesToWrite)
      }

      if (!shouldContinue) {
        audioTrack.release()
      }
      listener.onDataReceived(null)
      Log.d("PLAYBACK", "Audio Streaming Finished")
    }

  }
}