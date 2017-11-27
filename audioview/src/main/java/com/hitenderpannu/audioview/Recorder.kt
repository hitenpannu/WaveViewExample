package com.hitenderpannu.audioview

import android.Manifest
import android.media.AudioRecord
import android.os.Process
import android.support.annotation.RequiresPermission
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder


class Recorder constructor(private val audioFile: File, private val listener: AudioDataListener) {

  private var recorderThread: Thread? = null
  private var shouldContinue = false

  @RequiresPermission(Manifest.permission.RECORD_AUDIO)
  fun startRecording() {
    if (recorderThread == null) {
      shouldContinue = true
      recorderThread = Thread(Runnable { startRecordingThread() })
      recorderThread?.start()
    }
  }

  fun stopRecording() {
    recorderThread?.let {
      recorderThread = null
      shouldContinue = false
    }
  }

  private fun startRecordingThread() {
    Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
    var bufferSize = AudioRecord.getMinBufferSize(Config.SAMPLE_RATE, Config.AUDIO_INPUT_CHANNEL,
        Config.AUDIO_ENCODER)

    if ((bufferSize == AudioRecord.ERROR) || (bufferSize == AudioRecord.ERROR_BAD_VALUE)) {
      bufferSize = Config.SAMPLE_RATE * Config.BYTE_TO_SHORT_CONVERSION_FACTOR
    }

    val audioBuffer = ByteArray(bufferSize)

    val audioRecord = AudioRecord(Config.AUDIO_SOURCE, Config.SAMPLE_RATE,
        Config.AUDIO_INPUT_CHANNEL, Config.AUDIO_ENCODER, bufferSize)

    if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
      Log.e("AUDIO_RECORDER", "Unable to initialize audioRecorder")
      return
    }

    try {
      val outputStream = FileOutputStream(audioFile, true)
      audioRecord.startRecording()
      while (shouldContinue) {
        audioRecord.read(audioBuffer, 0, audioBuffer.size)
        outputStream.write(audioBuffer);

        val audioData = ShortArray(audioBuffer.size.div(Config.BYTE_TO_SHORT_CONVERSION_FACTOR))
        ByteBuffer.wrap(audioBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audioData)
        listener.onDataReceived(audioData)
      }
      audioRecord.stop()
      audioRecord.release()
      outputStream.flush()
      outputStream.close()
    } catch (exception: Exception) {
      Log.e("AUDIO_RECORDER", exception.message)
    }

  }

}
