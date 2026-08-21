package com.kareemessam.openship.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kareemessam.openship.shared.model.ProjectStatus
import com.kareemessam.openship.shared.model.ProjectSummary
import com.kareemessam.openship.shared.ui.theme.OpenshipAppTheme

@Composable
fun ProjectCard(
    project: ProjectSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OpenshipAppTheme.colors
    val statusKind = when (project.status) {
        ProjectStatus.READY -> StatusKind.SUCCESS
        ProjectStatus.BUILDING, ProjectStatus.QUEUED -> StatusKind.WARNING
        ProjectStatus.FAILED -> StatusKind.DANGER
        ProjectStatus.STOPPED -> StatusKind.NEUTRAL
        ProjectStatus.UNKNOWN -> StatusKind.INFO
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgCard)
            .border(1.dp, colors.borderCard, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Mac window dots + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MacWindowDots()

                StatusBadge(
                    text = project.statusText,
                    kind = statusKind,
                    pulseDot = project.status == ProjectStatus.BUILDING || project.status == ProjectStatus.READY
                )
            }

            // Project Title & Framework Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Framework Squircle Icon Tile (matching Openship dashboard)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.bgPill)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getFrameworkEmoji(project.framework),
                        fontSize = 20.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textHeading,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = project.framework.replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "·",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                        Text(
                            text = "Production",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }
                }
            }

            // Git & Branch Pill
            if (!project.gitRepo.isNullOrBlank() || !project.gitBranch.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.bgPill)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = "Branch",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = project.gitBranch ?: "main",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textHeading,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!project.gitRepo.isNullOrBlank()) {
                        Text(
                            text = project.gitRepo,
                            fontSize = 11.sp,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Live SSL / Domain Badge (exact matching Openship dashboard screenshot)
            val portText = if (project.hostPort != null) ":${project.hostPort}" else ":8080"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
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
                        text = "live at localhost$portText",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Logs",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textHeading
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Logs",
                        tint = colors.textHeading,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

private fun getFrameworkEmoji(framework: String): String {
    return when (framework.lowercase()) {
        "springboot", "spring", "java" -> "🍃"
        "docker", "compose" -> "🐳"
        "nextjs", "next" -> "▲"
        "react" -> "⚛️"
        "vue", "nuxt" -> "💚"
        "svelte", "sveltekit" -> "🟧"
        "nodejs", "node", "express" -> "🟢"
        "python", "django", "fastapi", "flask" -> "🐍"
        "go", "golang" -> "🔵"
        "rust" -> "🦀"
        "ruby", "rails" -> "💎"
        "php", "laravel" -> "🐘"
        "dotnet", "blazor" -> "🟣"
        else -> "📦"
    }
}
