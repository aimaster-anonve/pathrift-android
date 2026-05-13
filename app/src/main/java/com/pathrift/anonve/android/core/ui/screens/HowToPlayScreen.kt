package com.pathrift.anonve.android.core.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PathriftTextPrimary,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "back",
                            tint = PathriftNeonBlue,
                            modifier = Modifier.size(28.dp)
                        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PathriftBackground)
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            RuleSection(
                title = LanguageManager.s("PLACE TOWERS", "KULELERİ YERLEŞTIR"),
                icon = Icons.Default.Apps,
                text = LanguageManager.s(
                    "Tap empty slots to place towers. Each tower has unique strengths.",
                    "Kule yerleştirmek için boş slotlara dokun. Her kulenin benzersiz güçlü yönleri vardır."
                )
            )
            Spacer(Modifier.height(12.dp))

            RuleSection(
                title = LanguageManager.s("SEND WAVES", "DALGA GÖNDER"),
                icon = Icons.Default.PlayArrow,
                text = LanguageManager.s(
                    "Press SEND WAVE to start the next enemy wave. Enemies follow the path.",
                    "Sonraki düşman dalgasını başlatmak için DALGA GÖNDER'e bas. Düşmanlar yolu takip eder."
                )
            )
            Spacer(Modifier.height(12.dp))

            RuleSection(
                title = LanguageManager.s("RIFT SHIFT", "RIFT KAYIŞI"),
                icon = Icons.Default.Bolt,
                text = LanguageManager.s(
                    "Every 5 waves the map shifts! Move and reposition your towers.",
                    "Her 5 dalgada bir harita kayar! Kulelerini taşı ve yeniden konumlandır."
                )
            )
            Spacer(Modifier.height(12.dp))

            RuleSection(
                title = LanguageManager.s("TOWERS", "KULELER"),
                icon = Icons.Default.TrackChanges,
                text = LanguageManager.s(
                    "Bolt: Fast single-target\nBlast: Area damage\nFrost: Slows enemies 40%",
                    "Şimşek: Hızlı tek hedef\nPatlama: Alan hasarı\nBuz: Düşmanları %40 yavaşlatır"
                )
            )
            Spacer(Modifier.height(12.dp))

            RuleSection(
                title = LanguageManager.s("LIVES", "CANLAR"),
                icon = Icons.Default.Favorite,
                text = LanguageManager.s(
                    "You have 3 lives. Each enemy that reaches the exit costs 1 life.",
                    "3 canın var. Çıkışa ulaşan her düşman 1 can alır."
                )
            )
            Spacer(Modifier.height(12.dp))

            RuleSection(
                title = LanguageManager.s("SCORE", "PUAN"),
                icon = Icons.Default.Star,
                text = LanguageManager.s(
                    "Score = Wave × 1000 + Kills × 5. Push as far as you can!",
                    "Puan = Dalga × 1000 + Öldürme × 5. Mümkün olduğunca ilerle!"
                )
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun RuleSection(title: String, icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PathriftSurface, RoundedCornerShape(10.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PathriftNeonBlue,
            modifier = Modifier.size(22.dp).padding(top = 1.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PathriftNeonBlue,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                color = PathriftTextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}
