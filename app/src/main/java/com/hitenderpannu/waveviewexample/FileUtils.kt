package com.hitenderpannu.waveviewexample

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException

class FileUtils(private val context: Context) {

  val RECORDED_AUDIO_FILE = "recorded.pcm"
  val ENCODED_FILE = "encoded.m4a"

  fun getFile(filename: String): File {
    val audioDir = getAudioFileDirectory()
    val audioFile = File(audioDir, filename)
    if (!audioFile.exists()) {
      try {
        audioFile.createNewFile()
      } catch (e: IOException) {
        Log.e("TAG", e.message)
      }

    }
    return audioFile
  }

  // TODO: 2/6/17 convert to internal memory after testing
  fun getAudioFileDirectory(): File {
    val appDirectory = context.filesDir
    val audioDir = File(appDirectory, "audio")
    if (!audioDir.exists()) {
      audioDir.mkdir()
    }
    return audioDir
  }

  fun deleteFile(filename: String) {
    val audioDir = getAudioFileDirectory()
    val audioFile = File(audioDir, filename)
    if (audioFile.exists()) {
      try {
        audioFile.delete()
      } catch (e: IOException) {
        Log.e("TAG", e.message)
      }
    }
  }

}