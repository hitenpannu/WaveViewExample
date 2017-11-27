package com.hitenderpannu.audioview

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PCMDecoder(private val pcmFile: File, private val m4aFile: File) {
  private var extractor: MediaExtractor? = null
  private var codec: MediaCodec? = null

  fun extractMediaFromM4a(): Boolean? {
    try {
      extractor = MediaExtractor()
      extractor!!.setDataSource(m4aFile.path)
      val format = extractor!!.getTrackFormat(0)
      val mime = format.getString(MediaFormat.KEY_MIME)

      //decode
      codec = MediaCodec.createDecoderByType(mime)
      codec!!.configure(format, null, null, 0)
      codec!!.start()

      val codecInputBuffers = codec!!.inputBuffers
      var codecOutputBuffers = codec!!.outputBuffers
      val info = MediaCodec.BufferInfo()

      extractor!!.selectTrack(0)

      //start decoding
      val kTimeOutUs: Long = 10000
      var sawInputEOS = false
      var sawOutputEOS = false
      var noOutputCounter = 0
      val noOutputCounterLimit = 10

      // TODO: 2/10/17  remove the wrong percentage calculations
      val outputStream = FileOutputStream(pcmFile)
      while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit) {

        noOutputCounter++
        // read a buffer before feeding it to the decoder
        if (!sawInputEOS) {
          val inputBufIndex = codec!!.dequeueInputBuffer(kTimeOutUs)
          if (inputBufIndex >= 0) {
            val dstBuf = codecInputBuffers[inputBufIndex]
            var sampleSize = extractor!!.readSampleData(dstBuf, 0)
            var presentationTimeUs: Long = 0
            if (sampleSize < 0) {
              Log.d("PCM DECODER", "startDecoding:saw input EOS. Stopping playback ")
              sawInputEOS = true
              sampleSize = 0
            } else {
              presentationTimeUs = extractor!!.sampleTime
            }

            codec!!.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs,
                if (sawInputEOS) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
            if (!sawInputEOS) extractor!!.advance()
          } else {
            Log.d("PCM DECODER", "startDecoding: inputBufIndex " + inputBufIndex)
          }
        } // !sawInputEOS

        // decode to PCM and push it to the AudioTrack player
        val res = codec!!.dequeueOutputBuffer(info, kTimeOutUs)

        if (res >= 0) {
          if (info.size > 0) noOutputCounter = 0

          val buf = codecOutputBuffers[res]

          val chunk = ByteArray(info.size)
          buf.get(chunk)
          buf.clear()
          if (chunk.isNotEmpty()) {
            outputStream.write(chunk)
          }
          codec!!.releaseOutputBuffer(res, false)
          if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
            Log.d("PCM DECODER", "startDecoding:saw output EOS.")
            sawOutputEOS = true
          }
        } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
          codecOutputBuffers = codec!!.outputBuffers
          Log.d("PCM DECODER", "startDecoding:output buffers have changed.")
        } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          val outputFormat = codec!!.outputFormat
          Log.d("PCM DECODER", "startDecoding:output format has changed to " + outputFormat)
        } else {
          Log.d("PCM DECODER", "startDecoding:dequeueOutputBuffer returned " + res)
        }
      }

      Log.d("PCM DECODER", "startDecoding:stopping...")

      if (codec != null) {
        codec!!.stop()
        codec!!.release()
        codec = null
      }

      outputStream.flush()
      outputStream.close()

      return true
    } catch (e: IOException) {
      Log.e("PCM DECODER", e.message)
      return false
    }
  }
}
