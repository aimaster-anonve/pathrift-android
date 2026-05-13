package com.pathrift.anonve.android.core.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pathrift.anonve.android.R
import com.pathrift.anonve.android.core.storage.LocalProgressStore
import com.pathrift.anonve.android.core.ui.PathriftBackground
import com.pathrift.anonve.android.core.ui.PathriftGold
import com.pathrift.anonve.android.core.ui.PathriftNeonBlue
import com.pathrift.anonve.android.core.ui.PathriftSurface
import com.pathrift.anonve.android.core.ui.PathriftTextSecondary
import kotlinx.coroutines.flow.flowOf

@Composable
fun HomeScreen(
    onStartGame: () -> Unit,
    storage: LocalProgressStore? = null
) {
    val bestScore by (storage?.bestScore ?: flowOf(0L)).collectAsState(initial = 0L)
    val bestWave by (storage?.bestWave ?: flowOf(0)).collectAsState(initial = 0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PathriftBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PathriftNeonBlue,
                letterSpacing = 4.sp
            )

            Text(
                text = stringResource(R.string.main_menu_tagline),
                fontSize = 14.sp,
                color = PathriftTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(48.dp))

            if (bestWave > 0) {
                BestRunCard(bestScore = bestScore, bestWave = bestWave)
                Spacer(Modifier.height(32.dp))
            }

            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .width(220.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PathriftNeonBlue)
            ) {
                Text(
                    text = stringResource(R.string.main_menu_start),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PathriftBackground
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.main_menu_hint),
                fontSize = 11.sp,
                color = PathriftTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun BestRunCard(bestScore: Long, bestWave: Int) {
    Column(
        modifier = Modifier
            .background(PathriftSurface, RoundedCornerShape(12.dp))
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "BEST RUN",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = PathriftTextSecondary,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "$bestScore",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PathriftGold
        )
        Text(
            text = "Wave $bestWave",
            fontSize = 12.sp,
            color = PathriftTextSecondary
        )
    }
}
