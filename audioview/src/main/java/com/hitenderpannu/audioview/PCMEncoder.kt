package com.hitenderpannu.audioview

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer

class PCMEncoder(private val outputPath: String?) {
  private val SAMPLE_RATE = 44100
  private val CHANNEL_COUNT = 1
  private val BIT_RATE = 16000
  private var mediaFormat: MediaFormat? = null
  private var mediaCodec: MediaCodec? = null
  private var mediaMuxer: MediaMuxer? = null
  private var codecInputBuffers: Array<ByteBuffer>? = null
  private var codecOutputBuffers: Array<ByteBuffer>? = null
  private var bufferInfo: MediaCodec.BufferInfo? = null
  private var audioTrackId: Int = 0
  private var totalBytesRead: Int = 0
  private var presentationTimeUs: Double = 0.toDouble()

  fun prepare() {
    if (outputPath == null) {
      throw IllegalStateException("The output path must be set first!")
    }
    try {
      mediaFormat = MediaFormat.createAudioFormat(COMPRESSED_AUDIO_FILE_MIME_TYPE, SAMPLE_RATE,
          CHANNEL_COUNT)
      mediaFormat?.let {
        it.setInteger(MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        it.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
      }

      mediaCodec = MediaCodec.createEncoderByType(COMPRESSED_AUDIO_FILE_MIME_TYPE)
      mediaCodec?.let {
        it.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        it.start()
      }

      codecInputBuffers = mediaCodec!!.inputBuffers
      codecOutputBuffers = mediaCodec!!.outputBuffers

      bufferInfo = MediaCodec.BufferInfo()

      mediaMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
      totalBytesRead = 0
      presentationTimeUs = 0.0
    } catch (e: IOException) {
      Log.e("PCM ENCODER", e.message)
    }

  }

  fun stop() {
    Log.d("PCM ENCODER", "stop: MediaMuxer Stop")
    mediaCodec?.stop()
    mediaCodec?.release()
    mediaMuxer?.stop()
    mediaMuxer?.release()
  }

  @SuppressLint("WrongConstant")
  fun encode(inputFile: File): Boolean? {
    Log.d("PCM ENCODER", " Starting encoding of InputStream")

    val tempBuffer = ByteArray(2 * SAMPLE_RATE)
    var hasMoreData = true
    var stop = false
    val kTimeOutUs = 1000000L
    try {
      val inputStream = FileInputStream(inputFile)
      inputStream.skip(44)
      while (!stop) {
        var inputBufferIndex = 0
        var currentBatchRead = 0
        while (inputBufferIndex != -1 && hasMoreData && currentBatchRead <= 50 * SAMPLE_RATE) {
          inputBufferIndex = mediaCodec!!.dequeueInputBuffer(CODEC_TIMEOUT.toLong())

          if (inputBufferIndex >= 0) {
            val buffer = codecInputBuffers!![inputBufferIndex]
            buffer.clear()

            val bytesRead = inputStream.read(tempBuffer, 0, buffer.limit())
            if (bytesRead == -1) {
              mediaCodec!!.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs.toLong(), 0)
              hasMoreData = false
              stop = true
            } else {
              totalBytesRead += bytesRead
              currentBatchRead += bytesRead
              buffer.put(tempBuffer, 0, bytesRead)
              mediaCodec!!.queueInputBuffer(inputBufferIndex, 0, bytesRead,
                  presentationTimeUs.toLong(),
                  0)
              presentationTimeUs = (kTimeOutUs * (totalBytesRead / 2) / SAMPLE_RATE).toDouble()
            }
          }
        }

        var outputBufferIndex = 0
        while (outputBufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
          outputBufferIndex = mediaCodec!!.dequeueOutputBuffer(bufferInfo!!, CODEC_TIMEOUT.toLong())
          if (outputBufferIndex >= 0) {
            val encodedData = codecOutputBuffers!![outputBufferIndex]
            encodedData.position(bufferInfo!!.offset)
            encodedData.limit(bufferInfo!!.offset + bufferInfo!!.size)

            if (bufferInfo!!.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0 && bufferInfo!!.size != 0) {
              mediaCodec!!.releaseOutputBuffer(outputBufferIndex, false)
            } else {
              mediaMuxer!!.writeSampleData(audioTrackId, codecOutputBuffers!![outputBufferIndex],
                  bufferInfo!!)
              mediaCodec!!.releaseOutputBuffer(outputBufferIndex, false)
            }
          } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            mediaFormat = mediaCodec!!.outputFormat
            audioTrackId = mediaMuxer!!.addTrack(mediaFormat!!)
            mediaMuxer!!.start()
          }
        }
      }
      inputStream.close()
      Log.d("PCM ENCODER", "Finished encoding of InputStream ")
      return true
    } catch (e: IOException) {
      Log.e("PCM ENCODER", "%s", e)
      return false
    }

  }

  companion object {

    private val COMPRESSED_AUDIO_FILE_MIME_TYPE = "audio/mp4a-latm"
    private val CODEC_TIMEOUT = 5000
  }
}
