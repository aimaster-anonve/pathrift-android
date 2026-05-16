package com.pathrift.anonve.android.core.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pathrift.anonve.android.core.ui.AppLanguage
import com.pathrift.anonve.android.core.ui.LanguageManager
import com.pathrift.anonve.android.core.ui.PathriftBackground
import com.pathrift.anonve.android.core.ui.PathriftNeonBlue
import com.pathrift.anonve.android.core.ui.PathriftSurface
import com.pathrift.anonve.android.core.ui.PathriftTextPrimary
import com.pathrift.anonve.android.core.ui.PathriftTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val lang by LanguageManager.current.collectAsState()
    val context = LocalContext.current

    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    @Suppress("DEPRECATION")
    val buildCode = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        } catch (e: Exception) {
            1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LanguageManager.s("SETTINGS", "AYARLAR"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PathriftTextPrimary,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    Row(
                        modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ChevronLeft, null, tint = PathriftNeonBlue, modifier = Modifier.size(22.dp))
                        Text(LanguageManager.s("MAIN MENU", "ANA MENÜ"), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PathriftNeonBlue)
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(PathriftBackground)
                .padding(padding)
        ) {
            val isLandscape = maxWidth > maxHeight
            if (isLandscape) {
                // Landscape: Left panel (Language) + Right panel (Info + About)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left panel: Language
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SectionHeader(
                            title = LanguageManager.s("LANGUAGE", "DİL"),
                            icon = Icons.Default.Language
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppLanguage.values().forEach { language ->
                                val selected = lang == language
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val scale by animateFloatAsState(if (isPressed) 0.94f else 1.0f, spring(stiffness = 700f), label = "langBtnScale")
                                Button(
                                    onClick = { LanguageManager.setLanguage(language) },
                                    modifier = Modifier.weight(1f).height(44.dp).graphicsLayer { scaleX = scale; scaleY = scale },
                                    interactionSource = interactionSource,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) PathriftNeonBlue else PathriftSurface,
                                        contentColor = if (selected) PathriftBackground else PathriftTextPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${language.flag} ${language.displayName}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(PathriftSurface)
                    )

                    // Right panel: Game Info + About
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        SectionHeader(
                            title = LanguageManager.s("GAME INFO", "OYUN BİLGİSİ"),
                            icon = Icons.Default.Info
                        )
                        Spacer(Modifier.height(12.dp))
                        InfoRow(
                            label = LanguageManager.s("Version", "Versiyon"),
                            value = "$version ($buildCode)"
                        )
                        Spacer(Modifier.height(24.dp))
                        SectionHeader(title = "PATHRIFT", icon = Icons.Default.Bolt)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Endless tower defense where the map never stops shifting.\nPlace towers. Survive the Rift. Push further.",
                            fontSize = 13.sp,
                            color = PathriftTextSecondary,
                            lineHeight = 20.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PathriftSurface, RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        )
                    }
                }
            } else {
                // Portrait: vertical scroll
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Spacer(Modifier.height(16.dp))

                    // Language section
                    SectionHeader(
                        title = LanguageManager.s("LANGUAGE", "DİL"),
                        icon = Icons.Default.Language
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppLanguage.values().forEach { language ->
                            val selected = lang == language
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(if (isPressed) 0.94f else 1.0f, spring(stiffness = 700f), label = "langBtnScale")
                            Button(
                                onClick = { LanguageManager.setLanguage(language) },
                                modifier = Modifier.weight(1f).height(44.dp).graphicsLayer { scaleX = scale; scaleY = scale },
                                interactionSource = interactionSource,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) PathriftNeonBlue else PathriftSurface,
                                    contentColor = if (selected) PathriftBackground else PathriftTextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${language.flag} ${language.displayName}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Version / Game Info section
                    SectionHeader(
                        title = LanguageManager.s("GAME INFO", "OYUN BİLGİSİ"),
                        icon = Icons.Default.Info
                    )
                    Spacer(Modifier.height(12.dp))
                    InfoRow(
                        label = LanguageManager.s("Version", "Versiyon"),
                        value = "$version ($buildCode)"
                    )

                    Spacer(Modifier.height(16.dp))

                    // About / PATHRIFT section
                    SectionHeader(title = "PATHRIFT", icon = Icons.Default.Bolt)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Endless tower defense where the map never stops shifting.\nPlace towers. Survive the Rift. Push further.",
                        fontSize = 13.sp,
                        color = PathriftTextSecondary,
                        lineHeight = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PathriftSurface, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    )

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PathriftNeonBlue,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PathriftTextSecondary,
            letterSpacing = 1.5.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PathriftSurface, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = PathriftTextSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = PathriftTextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}
