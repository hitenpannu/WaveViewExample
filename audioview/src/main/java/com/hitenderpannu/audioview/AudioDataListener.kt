package com.hitenderpannu.audioview

interface AudioDataListener {
  fun onDataReceived(audioData: ShortArray?)
}