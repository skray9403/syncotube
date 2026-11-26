package com.metrolist.android.ui.nectar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

data class NectarTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationLabel: String,
    val durationSec: Int,
    val palette: List<Color>,
)

data class NectarAlbum(
    val id: String,
    val title: String,
    val subtitle: String,
    val palette: List<Color>,
)

data class NectarLyricLine(
    val timestampSec: Int,
    val text: String,
)

object NectarSampleData {
    val tracks = listOf(
        NectarTrack(
            id = "1",
            title = "Midnight Echoes",
            artist = "Solstice",
            album = "Equinox",
            durationLabel = "3:42",
            durationSec = 222,
            palette = listOf(Color(0xFF1E2749), Color(0xFF4A5899), Color(0xFFD99B66)),
        ),
        NectarTrack(
            id = "2",
            title = "Amber Sunsets",
            artist = "Aurora Blue",
            album = "Warm Horizon",
            durationLabel = "3:42",
            durationSec = 222,
            palette = listOf(Color(0xFF8C3B2B), Color(0xFFD97D54), Color(0xFFF2C57C)),
        ),
        NectarTrack(
            id = "3",
            title = "Cascade Reflection",
            artist = "Komorebi",
            album = "Still Water",
            durationLabel = "4:18",
            durationSec = 258,
            palette = listOf(Color(0xFF2B3A42), Color(0xFF5D737E), Color(0xFFB8C4BB)),
        ),
        NectarTrack(
            id = "4",
            title = "Velvet Horizon",
            artist = "Kaelen",
            album = "Dusk Notes",
            durationLabel = "3:05",
            durationSec = 185,
            palette = listOf(Color(0xFF3E2F5B), Color(0xFF7B5E7B), Color(0xFFE0B1CB)),
        ),
    )

    val albums = listOf(
        NectarAlbum("a1", "Ambient Focus", "Playlist • 42 tracks", listOf(Color(0xFF264653), Color(0xFF2A9D8F), Color(0xFFE9C46A))),
        NectarAlbum("a2", "Neo-Soul Living", "Album • Aurora Blue", listOf(Color(0xFF7F4F24), Color(0xFFB08968), Color(0xFFE6CCB2))),
        NectarAlbum("a3", "Twilight Beats", "Mix • Updated today", listOf(Color(0xFF3D348B), Color(0xFF7678ED), Color(0xFFF7B801))),
        NectarAlbum("a4", "Glass Garden", "Album • Milo", listOf(Color(0xFF344E41), Color(0xFF588157), Color(0xFFA3B18A))),
    )

    val lyrics = listOf(
        NectarLyricLine(12, "Streetlights blur into gold"),
        NectarLyricLine(35, "Echoes drift across the bay"),
        NectarLyricLine(60, "Quiet static in the air"),
        NectarLyricLine(84, "Hold the signal through the static"),
        NectarLyricLine(108, "Every frequency aligns tonight"),
        NectarLyricLine(134, "Until the morning light"),
        NectarLyricLine(162, "Fading embers on the horizon"),
        NectarLyricLine(190, "We stay awake until dawn"),
    )

    val outputDevices = listOf(
        "Pixel Buds Pro",
        "Phone Speaker",
        "Living Room Cast",
    )
}

@Composable
fun NectarArtwork(
    palette: List<Color>,
    shape: Shape = MaterialTheme.shapes.medium,
    modifier: Modifier = Modifier,
) {
    val base = palette.getOrElse(0) { MaterialTheme.colorScheme.primaryContainer }
    val mid = palette.getOrElse(1) { MaterialTheme.colorScheme.primary }
    val accent = palette.getOrElse(2) { MaterialTheme.colorScheme.secondaryContainer }

    Box(modifier = modifier.clip(shape)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = base)
            drawCircle(
                color = mid.copy(alpha = 0.85f),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.68f, size.height * 0.36f),
            )
            drawCircle(
                color = accent.copy(alpha = 0.8f),
                radius = size.minDimension * 0.24f,
                center = Offset(size.width * 0.32f, size.height * 0.70f),
            )
        }
    }
}
