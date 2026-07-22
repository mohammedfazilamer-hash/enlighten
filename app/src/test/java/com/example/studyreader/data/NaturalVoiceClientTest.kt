package com.example.studyreader.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NaturalVoiceClientTest {
  @Test
  fun naturalVoiceBaseUrl_usesOllamaComputerAndVoicePort() {
    assertEquals("http://192.168.1.25:11435", naturalVoiceBaseUrl("192.168.1.25:11434"))
  }

  @Test
  fun naturalVoiceBaseUrl_replacesCustomPortAndPathFreeUrl() {
    assertEquals("http://study-pc.local:11435", naturalVoiceBaseUrl("https://study-pc.local:443"))
  }
}
