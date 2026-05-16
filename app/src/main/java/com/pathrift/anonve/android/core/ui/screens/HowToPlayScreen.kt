package com.pathrift.anonve.android.core.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pathrift.anonve.android.core.ui.LanguageManager
import com.pathrift.anonve.android.core.ui.PathriftBackground
import com.pathrift.anonve.android.core.ui.PathriftNeonBlue
import com.pathrift.anonve.android.core.ui.PathriftSurface
import com.pathrift.anonve.android.core.ui.PathriftTextPrimary
import com.pathrift.anonve.android.core.ui.PathriftTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
    val lang by LanguageManager.current.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LanguageManager.s("HOW TO PLAY", "NASIL OYNANIR"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PathriftTextPrimary,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                navigationIcon = {
                    Row(
                        modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ChevronLeft, null, tint = PathriftNeonBlue, modifier = Modifier.size(22.dp))
                        Text("HOME", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PathriftNeonBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PathriftSurface.copy(alpha = 0.9f),
                    titleContentColor = PathriftTextPrimary
                )
            )
        },
        containerColor = PathriftBackground
    ) { padding ->
        val ruleSections = listOf(
            Triple(
                LanguageManager.s("PLACE TOWERS", "KULELERİ YERLEŞTIR"),
                Icons.Default.Apps,
                LanguageManager.s(
                    "Tap empty slots to place towers. Each tower has unique strengths.",
                    "Kule yerleştirmek için boş slotlara dokun. Her kulenin benzersiz güçlü yönleri vardır."
                )
            ),
            Triple(
                LanguageManager.s("SEND WAVES", "DALGA GÖNDER"),
                Icons.Default.PlayArrow,
                LanguageManager.s(
                    "Press SEND WAVE to start the next enemy wave. Enemies follow the path.",
                    "Sonraki düşman dalgasını başlatmak için DALGA GÖNDER'e bas. Düşmanlar yolu takip eder."
                )
            ),
            Triple(
                LanguageManager.s("RIFT SHIFT", "RIFT KAYIŞI"),
                Icons.Default.Bolt,
                LanguageManager.s(
                    "Every 5 waves the map shifts! Move and reposition your towers.",
                    "Her 5 dalgada bir harita kayar! Kulelerini taşı ve yeniden konumlandır."
                )
            ),
            Triple(
                LanguageManager.s("TOWERS", "KULELER"),
                Icons.Default.TrackChanges,
                LanguageManager.s(
                    "Bolt: Fast single-target\nBlast: Area damage\nFrost: Slows enemies 40%\nSniper: Long range, all layers\nArtillery: Bridge only, AoE",
                    "Şimşek: Hızlı tek hedef\nPatlama: Alan hasarı\nBuz: Düşmanları %40 yavaşlatır\nKeskin Nişancı: Uzun menzil, tüm katmanlar\nTopçu: Yalnızca köprü, AoE"
                )
            ),
            Triple(
                LanguageManager.s("BRIDGE LAYERS", "KATMAN SİSTEMİ"),
                Icons.Default.Layers,
                LanguageManager.s(
                    "Some maps have bridge segments. Sniper hits all layers. Artillery targets bridges only.",
                    "Bazı haritaların köprü bölümleri vardır. Keskin Nişancı tüm katmanları vurur. Topçu yalnızca köprüleri hedef alır."
                )
            ),
            Triple(
                LanguageManager.s("LIVES", "CANLAR"),
                Icons.Default.Favorite,
                LanguageManager.s(
                    "You have 3 lives. Each enemy that reaches the exit costs 1 life.",
                    "3 canın var. Çıkışa ulaşan her düşman 1 can alır."
                )
            ),
            Triple(
                LanguageManager.s("SCORE", "PUAN"),
                Icons.Default.Star,
                LanguageManager.s(
                    "Score = Wave × 1000 + Kills × 5. Push as far as you can!",
                    "Puan = Dalga × 1000 + Öldürme × 5. Mümkün olduğunca ilerle!"
                )
            )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(PathriftBackground)
                .padding(padding)
        ) {
            val isLandscape = maxWidth > maxHeight
            if (isLandscape) {
                // Landscape: 2-column grid of rule cards
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(ruleSections) { (title, icon, text) ->
                        RuleSection(title = title, icon = icon, text = text)
                    }
                }
            } else {
                // Portrait: single-column vertical scroll
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Spacer(Modifier.height(12.dp))
                    ruleSections.forEachIndexed { index, (title, icon, text) ->
                        RuleSection(title = title, icon = icon, text = text)
                        if (index < ruleSections.lastIndex) {
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun RuleSection(title: String, icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PathriftSurface, RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PathriftNeonBlue,
            modifier = Modifier.size(18.dp).padding(top = 1.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PathriftNeonBlue,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = PathriftTextSecondary,
                lineHeight = 19.sp
            )
        }
    }
}
