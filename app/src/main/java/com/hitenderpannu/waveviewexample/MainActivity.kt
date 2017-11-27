package com.hitenderpannu.waveviewexample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import com.hitenderpannu.audioview.*
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File

class MainActivity : AppCompatActivity() {

  private lateinit var recordButton: ImageButton
  private lateinit var playButton: ImageButton
  private lateinit var pauseButton: ImageButton
  private lateinit var encodeButton: Button
  private lateinit var decodeButton: Button
  private lateinit var audioPlot: AudioPlot

  private var audioFile: File? = null
  private lateinit var fileUtils: FileUtils

  private val audioRecorder: Recorder by lazy {
    Recorder(audioFile!!, object : AudioDataListener {
      override fun onDataReceived(audioData: ShortArray?) {
        Log.d("TAG", "data received")
        if (!isFinishing) {
          audioPlot.setSamples(audioData)
        }
      }
    })
  }

  private val audioPlayer: Playback by lazy {
    Playback(audioFile!!, object : AudioDataListener {
      override fun onDataReceived(audioData: ShortArray?) {
        Log.d("TAG", "data received from file")
        if (!isFinishing) {
          audioPlot.setSamples(audioData)
        }
      }
    })
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    fileUtils = FileUtils(this.baseContext)
    fileUtils.deleteFile(filename = fileUtils.RECORDED_AUDIO_FILE)
    audioFile = fileUtils.getFile(fileUtils.RECORDED_AUDIO_FILE)
    initializeViews()
  }

  private fun initializeViews() {
    recordButton = findViewById(R.id.button_record)
    pauseButton = findViewById(R.id.button_pause)
    playButton = findViewById(R.id.button_play)
    audioPlot = findViewById(R.id.audio_plot)
    encodeButton = findViewById(R.id.button_encode)
    decodeButton = findViewById(R.id.button_decode)

    if (audioFile == null) return

    recordButton.setOnClickListener {
      if (ActivityCompat.checkSelfPermission(this,
          Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
        audioRecorder.startRecording()
      } else {
        Toast.makeText(this, "Please provide RecordAudio Permission", Toast.LENGTH_LONG).show()
      }
    }

    playButton.setOnClickListener {
      audioRecorder.stopRecording()
      audioPlayer.startPlaying()
    }

    pauseButton.setOnClickListener {
      audioPlayer.stopPlaying()
    }

    encodeButton.setOnClickListener {
      encodeButton.isEnabled = false
      decodeButton.isEnabled = false
      audioFile?.let {
        audioRecorder.stopRecording()
        audioPlayer.stopPlaying()
        startEncoding(fileUtils.getFile(filename = fileUtils.ENCODED_FILE), it).subscribeOn(
            Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({ _ ->
          encodeButton.isEnabled = true
          decodeButton.isEnabled = true
          Toast.makeText(this, "DONE", Toast.LENGTH_SHORT).show()
        }, { error -> Log.e("ERROR", error.message) })
      }
    }

    decodeButton.setOnClickListener {
      encodeButton.isEnabled = false
      decodeButton.isEnabled = false
      audioFile?.let {
        audioRecorder.stopRecording()
        audioPlayer.stopPlaying()
        startDecoding(it, fileUtils.getFile(filename = fileUtils.ENCODED_FILE)).subscribeOn(
            Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({ _ ->
          encodeButton.isEnabled = true
          decodeButton.isEnabled = true
          Toast.makeText(this, "DONE", Toast.LENGTH_SHORT).show()
        }, { error -> Log.e("ERROR", error.message) })
      }
    }
  }

  private fun startEncoding(outputFile: File, inputFile: File): Single<Boolean> {
    return Single.fromCallable<Boolean> {
      val encoder = PCMEncoder(outputFile.path)
      encoder.prepare()
      if (encoder.encode(inputFile) == true) {
        encoder.stop()
        return@fromCallable true
      } else {
        return@fromCallable false
      }
    }
  }

  private fun startDecoding(outputFile: File, inputFile: File): Single<Boolean> {
    return Single.fromCallable<Boolean> {
      val decoder = PCMDecoder(outputFile, inputFile)
      return@fromCallable decoder.extractMediaFromM4a() == true
    }
  }

  override fun onPause() {
    super.onPause()
    audioRecorder.stopRecording()
    audioPlayer.stopPlaying()
  }
}
