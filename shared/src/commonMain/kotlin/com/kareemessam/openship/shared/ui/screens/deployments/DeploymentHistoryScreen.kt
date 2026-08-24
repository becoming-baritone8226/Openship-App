package com.kareemessam.openship.shared.ui.screens.deployments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kareemessam.openship.shared.model.DeploymentDto
import com.kareemessam.openship.shared.ui.components.StatusBadge
import com.kareemessam.openship.shared.ui.components.StatusKind
import com.kareemessam.openship.shared.ui.theme.OpenshipAppTheme
import com.kareemessam.openship.shared.viewmodel.DeploymentHistoryUiState
import com.kareemessam.openship.shared.viewmodel.formatRelativeAge
import com.kareemessam.openship.shared.viewmodel.isRollbackEligible

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeploymentHistoryScreen(
    state: DeploymentHistoryUiState,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onRollback: (DeploymentDto) -> Unit,
    onRollbackConfirm: () -> Unit,
    onRollbackCancel: () -> Unit,
    onRollbackResultConsumed: () -> Unit,
    onOpenLogs: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OpenshipAppTheme.colors

    // Route to the new deployment's logs once a rollback returns an id.
    LaunchedEffect(state.rollbackResultDeploymentId) {
        val deploymentId = state.rollbackResultDeploymentId
        if (deploymentId != null) {
            onOpenLogs(deploymentId)
            onRollbackResultConsumed()
        }
    }

    state.rollbackTarget?.let { target ->
        if (state.rollbackResultDeploymentId == null) {
            val activeDeployment = state.deployments.firstOrNull { it.id == state.activeDeploymentId }
            RollbackConfirmDialog(
                target = target,
                activeDeployment = activeDeployment,
                isLoading = state.rollbackLoading,
                error = state.rollbackError,
                onConfirm = onRollbackConfirm,
                onCancel = onRollbackCancel
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Deployment History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = colors.textHeading
                        )
                        Text(
                            text = "${state.deployments.size} deployment${if (state.deployments.size == 1) "" else "s"}",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textHeading
                        )
                    }
                },
                actions = {
                    if (state.selectedDeploymentId != null) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = colors.textSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgPage)
            )
        },
        containerColor = colors.bgPage,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = colors.btnPrimaryBg,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                state.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.bgCard)
                            .border(1.dp, colors.statusFailedBorder, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = colors.statusFailed,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Failed to load history",
                                fontWeight = FontWeight.Bold,
                                color = colors.textHeading,
                                fontSize = 16.sp
                            )
                            Text(
                                text = state.error,
                                color = colors.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                state.deployments.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.bgCard)
                            .border(1.dp, colors.borderCard, RoundedCornerShape(16.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Empty",
                                tint = colors.textMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No prior deployments",
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textHeading,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Deployments for this project will appear here.",
                                color = colors.textMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = state.deployments,
                            key = { it.id }
                        ) { deployment ->
                            DeploymentHistoryRow(
                                deployment = deployment,
                                isSelected = state.selectedDeploymentId == deployment.id,
                                canRollback = state.rollbackAvailable &&
                                    isRollbackEligible(deployment, state.activeDeploymentId),
                                onClick = { onSelect(deployment.id) },
                                onRollback = { onRollback(deployment) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RollbackConfirmDialog(
    target: DeploymentDto,
    activeDeployment: DeploymentDto?,
    isLoading: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = OpenshipAppTheme.colors
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text("Rollback deployment?", fontWeight = FontWeight.Bold, color = colors.warning.solid)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Rollback to ${target.commitSha?.take(7) ?: "—"} — " +
                        "${target.commitMessage ?: "no message"} · ${formatRelativeAge(target.createdAt)}",
                    fontSize = 14.sp,
                    color = colors.textHeading
                )
                Text(
                    text = if (activeDeployment != null) {
                        "This will replace the current active deployment " +
                            "(${activeDeployment.commitSha?.take(7) ?: "—"})."
                    } else {
                        "This will replace the current active deployment."
                    },
                    fontSize = 13.sp,
                    color = colors.warning.solid
                )
                Text(
                    text = "This action is destructive and cannot be undone.",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
                if (error != null) {
                    Text(
                        text = error,
                        fontSize = 13.sp,
                        color = colors.statusFailed
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = colors.btnPrimaryBg,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text("Rollback", color = colors.statusFailed)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !isLoading) {
                Text("Cancel")
            }
        },
        containerColor = colors.bgCard
    )
}

@Composable
private fun DeploymentHistoryRow(
    deployment: DeploymentDto,
    isSelected: Boolean,
    canRollback: Boolean,
    onClick: () -> Unit,
    onRollback: () -> Unit
) {
    val colors = OpenshipAppTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgCard)
            .border(
                1.dp,
                if (isSelected) colors.borderFocus else colors.borderCard,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    text = (deployment.status ?: "unknown").replaceFirstChar { it.uppercase() },
                    kind = statusToKind(deployment.status),
                    pulseDot = false
                )
                Text(
                    text = formatRelativeAge(deployment.createdAt),
                    fontSize = 11.sp,
                    color = colors.textMuted,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = deployment.commitSha?.take(7) ?: "—",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textHeading,
                    fontFamily = FontFamily.Monospace
                )
                if (!deployment.branch.isNullOrBlank()) {
                    Text(
                        text = "· ${deployment.branch}",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }
            }

            if (!deployment.commitMessage.isNullOrBlank()) {
                Text(
                    text = deployment.commitMessage,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (canRollback) {
                Button(
                    onClick = onRollback,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.btnPrimaryBg,
                        contentColor = colors.btnPrimaryText
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rollback to this deployment", fontSize = 12.sp)
                }
            }
        }
    }
}

private fun statusToKind(status: String?): StatusKind = when (status?.lowercase()) {
    "ready", "success", "active", "healthy", "up" -> StatusKind.SUCCESS
    "building", "deploying", "queued" -> StatusKind.WARNING
    "failed", "error", "crashed" -> StatusKind.DANGER
    "cancelled", "stopped" -> StatusKind.NEUTRAL
    else -> StatusKind.INFO
}
