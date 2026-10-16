package com.example.videokey.data

data class Song(
    val code: String,
    val title: String,
    val artist: String,
    val filePath: String,
    val category: String = "OPM",
    val sectorOffset: Long = -1L,
    val sectorCount: Int = 0,
    // Location inside an encrypted .vkpack archive (filePath = pack file). -1 = not packed.
    val packId: Int = -1,
    val packOffset: Long = -1L,
    val packLength: Int = 0,
    val packOrigLength: Int = 0,
    val packCrc: Int = 0
) {
    val isPacked: Boolean get() = packId >= 0 && packOffset >= 0L
}

data class KaraokeLyric(
    val text: String,
    val timestampMs: Long,
    val durationMs: Long
)

data class KaraokeLine(
    val lineIndex: Int,
    val syllables: List<KaraokeLyric>,
    val startMs: Long,
    val endMs: Long,
    val fullText: String
)
