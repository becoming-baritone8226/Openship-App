package com.kareemessam.openship.shared.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kareemessam.openship.shared.ui.theme.LocalThemeMode
import com.kareemessam.openship.shared.ui.theme.OpenshipAppTheme
import com.kareemessam.openship.shared.ui.theme.OpenshipColors
import com.kareemessam.openship.shared.ui.theme.ThemeMode
import openship_app.shared.generated.resources.Res
import openship_app.shared.generated.resources.app_logo
import openship_app.shared.generated.resources.app_logo_white
import org.jetbrains.compose.resources.painterResource

@Composable
fun OpenshipBrandLogo(
    modifier: Modifier = Modifier
) {
    val colors = OpenshipAppTheme.colors
    val logoResource = if (colors.isDark) Res.drawable.app_logo_white else Res.drawable.app_logo

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        // True OpenShip Image Logo (centered and extracted directly from source artwork)
        Image(
            painter = painterResource(logoResource),
            contentDescription = "OpenShip Logo",
            modifier = Modifier.size(26.dp)
        )

        Text(
            text = "OpenShip",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = colors.textHeading,
            letterSpacing = (-0.3).sp
        )
    }
}

@Composable
fun MacWindowDots(
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(OpenshipColors.MacClose)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(OpenshipColors.MacMinimize)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(OpenshipColors.MacMaximize)
        )
    }
}

@Composable
fun OpenshipTopBar(
    instanceLabel: String?,
    onSwitchInstance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OpenshipAppTheme.colors
    val themeModeState = LocalThemeMode.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OpenshipBrandLogo()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Theme toggle
            IconButton(
                onClick = {
                    themeModeState.value = if (colors.isDark) ThemeMode.LIGHT else ThemeMode.DARK
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (colors.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Instance pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.bgPill)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(999.dp))
                    .clickable(onClick = onSwitchInstance)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.statusActive)
                )
                Text(
                    text = instanceLabel ?: "Localhost",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textHeading
                )
            }
        }
    }
}
