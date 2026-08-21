package com.kareemessam.openship.shared.ui.screens.monitor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kareemessam.openship.shared.ui.components.CircularMetricGauge
import com.kareemessam.openship.shared.ui.components.MacWindowDots
import com.kareemessam.openship.shared.ui.components.OpenshipTopBar
import com.kareemessam.openship.shared.ui.components.SparklineTrendCard
import com.kareemessam.openship.shared.ui.components.StatusBadge
import com.kareemessam.openship.shared.ui.components.StatusKind
import com.kareemessam.openship.shared.ui.theme.OpenshipAppTheme
import com.kareemessam.openship.shared.viewmodel.MonitorViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ServerMonitorScreen(
    modifier: Modifier = Modifier,
    viewModel: MonitorViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val colors = OpenshipAppTheme.colors
    val stats = state.currentStats

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(colors.bgPage)) {
                OpenshipTopBar(
                    instanceLabel = state.activeInstance?.label,
                    onSwitchInstance = { /* Dropdown */ }
                )
            }
        },
        containerColor = colors.bgPage,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monitoring",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = colors.textHeading,
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        text = "Real-time host telemetry & metrics",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }

                StatusBadge(
                    text = if (state.isStreaming) "Telemetry Live" else "Connecting",
                    kind = if (state.isStreaming) StatusKind.SUCCESS else StatusKind.WARNING,
                    pulseDot = state.isStreaming
                )
            }

            // Server Metadata Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.bgCard)
                    .border(1.dp, colors.borderCard, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MacWindowDots()

                        if (stats != null && stats.uptimeSeconds > 0) {
                            val uptimeStr = formatUptime(stats.uptimeSeconds)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Uptime: $uptimeStr",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.bgPill)
                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🖥️", fontSize = 18.sp)
                        }

                        Column {
                            Text(
                                text = state.activeServer?.name ?: "This Server",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.textHeading
                            )
                            Text(
                                text = "Deploy Mode: Docker · Host: ${state.activeInstance?.url ?: "localhost"}",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }

            // 3 Circular Gauges: CPU, Memory, Disk
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val cpuVal = stats?.cpu?.toFloat() ?: 0f
                CircularMetricGauge(
                    percentage = cpuVal,
                    label = "CPU",
                    valueSubtext = "${cpuVal.toInt()}% load",
                    modifier = Modifier.weight(1f)
                )

                val memPercent = stats?.memPercentage ?: 0f
                val memUsedGb = formatBytesToGb(stats?.memUsed ?: 0L)
                val memTotalGb = formatBytesToGb(stats?.memTotal ?: 0L)
                CircularMetricGauge(
                    percentage = memPercent,
                    label = "RAM",
                    valueSubtext = "$memUsedGb / $memTotalGb GB",
                    modifier = Modifier.weight(1f)
                )

                val diskPercent = stats?.diskPercentage ?: 0f
                val diskUsedGb = formatBytesToGb(stats?.diskUsed ?: 0L)
                val diskTotalGb = formatBytesToGb(stats?.diskTotal ?: 0L)
                CircularMetricGauge(
                    percentage = diskPercent,
                    label = "Disk",
                    valueSubtext = "$diskUsedGb / $diskTotalGb GB",
                    modifier = Modifier.weight(1f)
                )
            }

            // Rolling Live Sparkline Trend Graphs
            val currentCpuStr = "${(stats?.cpu ?: 0.0).toInt()}%"
            SparklineTrendCard(
                title = "CPU Utilization Trend",
                currentValue = currentCpuStr,
                history = state.cpuHistory,
                lineColor = colors.statusActive
            )

            val currentMemStr = "${(stats?.memPercentage ?: 0f).toInt()}% (${formatBytesToGb(stats?.memUsed ?: 0L)} GB)"
            SparklineTrendCard(
                title = "Memory Utilization Trend",
                currentValue = currentMemStr,
                history = state.memHistory,
                lineColor = colors.info.solid
            )

            // System Load Average Pills
            if (stats != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.bgCard)
                        .border(1.dp, colors.borderCard, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SYSTEM LOAD AVERAGES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LoadPill(label = "1 min", value = "${stats.load1Val}", modifier = Modifier.weight(1f))
                            LoadPill(label = "5 min", value = "${stats.load5Val}", modifier = Modifier.weight(1f))
                            LoadPill(label = "15 min", value = "${stats.load15Val}", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = OpenshipAppTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.bgPill)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 10.sp, color = colors.textMuted)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textHeading, fontFamily = FontFamily.Monospace)
        }
    }
}

private fun formatBytesToGb(bytes: Long): String {
    val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    return ((gb * 10).toInt() / 10.0).toString()
}

private fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val mins = (seconds % 3600) / 60
    return if (days > 0) "${days}d ${hours}h" else "${hours}h ${mins}m"
}
