package com.example.videokey

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.videokey.data.MidiKaraokeParser
import com.example.videokey.data.ShubBlobExtractor
import com.songhub.android.database.NativeLib
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ShubBlobExtractorTest {

    @Test
    fun testNativeLibLoadAndExtraction() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue("NativeLib should be loaded", NativeLib.isLoaded)

        val blobFile = File("/sdcard/VideoKey/idx/shubblob")
        assertTrue("shubblob must exist on device", blobFile.exists())

        // 186214 "You're Still the One": sectorOffset = 276110, sectorCount = 32
        val extractedMidi = ShubBlobExtractor.extractMidi(
            sectorOffset = 276110L,
            sectorCount = 32,
            cacheDir = appContext.cacheDir,
            songCode = "186214",
            customBlobDirs = listOf(File("/sdcard/VideoKey/idx"))
        )

        assertNotNull("Extracted MIDI should not be null", extractedMidi)
        assertTrue("Extracted MIDI file must exist", extractedMidi!!.exists())
        assertTrue("Extracted MIDI size should be > 1000 bytes", extractedMidi.length() > 1000)

        // Parse karaoke lyrics
        val parsed = MidiKaraokeParser.parse(extractedMidi)
        assertNotNull("Parsed karaoke should not be null", parsed)
        assertTrue("Song should have lyrics lines", parsed.lines.isNotEmpty())
        println("SUCCESS! Extracted song 186214: lines count = ${parsed.lines.size}")
        for (i in 0 until minOf(5, parsed.lines.size)) {
            println("Lyric line $i: [${parsed.lines[i].startMs}ms] ${parsed.lines[i].fullText}")
        }
    }
}
