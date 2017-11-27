package com.hitenderpannu.audioview

import android.media.AudioFormat
import android.media.MediaRecorder

object Config {

  val BYTE_TO_SHORT_CONVERSION_FACTOR = 2

  val SAMPLE_RATE = 44100
  val BIT_RATE = 16000
  val CHANNEL_COUNT = 1

  val AUDIO_INPUT_CHANNEL = AudioFormat.CHANNEL_IN_MONO
  val AUDIO_ENCODER = AudioFormat.ENCODING_PCM_16BIT
  val AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION


}