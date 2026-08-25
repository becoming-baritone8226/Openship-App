package com.kareemessam.openship.shared.ui.screens.connect

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kareemessam.openship.shared.ui.components.MacWindowDots
import com.kareemessam.openship.shared.ui.components.OpenshipBrandLogo
import com.kareemessam.openship.shared.ui.components.StatusBadge
import com.kareemessam.openship.shared.ui.components.StatusKind
import com.kareemessam.openship.shared.ui.theme.LocalThemeMode
import com.kareemessam.openship.shared.ui.theme.OpenshipAppTheme
import com.kareemessam.openship.shared.ui.theme.ThemeMode
import com.kareemessam.openship.shared.viewmodel.ConnectUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    state: ConnectUiState,
    onUrlChanged: (String) -> Unit,
    onLabelChanged: (String) -> Unit,
    onPatChanged: (String) -> Unit,
    onProbeClicked: () -> Unit,
    onConnectClicked: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPat by remember { mutableStateOf(false) }
    val colors = OpenshipAppTheme.colors
    val themeModeState = LocalThemeMode.current

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { OpenshipBrandLogo() },
                actions = {
                    IconButton(
                        onClick = {
                            themeModeState.value = if (colors.isDark) ThemeMode.LIGHT else ThemeMode.DARK
                        }
                    ) {
                        Icon(
                            imageVector = if (colors.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = colors.textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.bgPage
                )
            )
        },
        containerColor = colors.bgPage,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.bgCard)
                    .border(1.dp, colors.borderCard, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacWindowDots()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Connect Server",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colors.textHeading
                    )
                    Text(
                        text = "Connect to your self-hosted Openship instance to monitor deployments and servers in real time.",
                        fontSize = 13.sp,
                        color = colors.textBody,
                        lineHeight = 18.sp
                    )
                }
            }

            // Connection Form Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.bgCard)
                    .border(1.dp, colors.borderCard, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Instance URL Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "INSTANCE URL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted
                        )
                        OutlinedTextField(
                            value = state.url,
                            onValueChange = onUrlChanged,
                            placeholder = { Text("http://192.168.1.112:4000", color = colors.textGhost) },
                            trailingIcon = {
                                IconButton(onClick = onProbeClicked) {
                                    if (state.isProbing) {
                                        CircularProgressIndicator(
                                            color = colors.textHeading,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Probe",
                                            tint = colors.textSecondary
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.bgCard,
                                unfocusedContainerColor = colors.bgCard,
                                focusedBorderColor = colors.borderFocus,
                                unfocusedBorderColor = colors.borderInput,
                                focusedTextColor = colors.textHeading,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            singleLine = true
                        )

                        // Quick Connection Presets (Wi-Fi LAN / USB)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ConnectionPresetPill(
                                label = "🏠 Wi-Fi (192.168.1.112:4000)",
                                onClick = {
                                    onUrlChanged("http://192.168.1.112:4000")
                                    onProbeClicked()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ConnectionPresetPill(
                                label = "🔌 USB (localhost:4000)",
                                onClick = {
                                    onUrlChanged("http://localhost:4000")
                                    onProbeClicked()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Live Discovery Result Pill
                    if (state.discoveredEnv != null) {
                        val env = state.discoveredEnv
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.bgPill)
                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Openship v${env.version}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = colors.textHeading
                                    )
                                    Text(
                                        text = "${env.deployMode?.replaceFirstChar { it.uppercase() } ?: "Docker"} · ${env.authMode}",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                }
                                StatusBadge(text = "Online", kind = StatusKind.SUCCESS, pulseDot = true)
                            }
                        }
                    }

                    // Server Label Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SERVER LABEL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted
                        )
                        OutlinedTextField(
                            value = state.label,
                            onValueChange = onLabelChanged,
                            placeholder = { Text("My Openship Server", color = colors.textGhost) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.bgCard,
                                unfocusedContainerColor = colors.bgCard,
                                focusedBorderColor = colors.borderFocus,
                                unfocusedBorderColor = colors.borderInput,
                                focusedTextColor = colors.textHeading,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            singleLine = true
                        )
                    }

                    // Personal Access Token (PAT) Field
                    AnimatedVisibility(visible = state.discoveredEnv?.authMode != "none") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "PERSONAL ACCESS TOKEN (PAT)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted
                            )
                            OutlinedTextField(
                                value = state.pat,
                                onValueChange = onPatChanged,
                                placeholder = { Text("opsh_pat_...", color = colors.textGhost) },
                                visualTransformation = if (showPat) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPat = !showPat }) {
                                        Icon(
                                            imageVector = if (showPat) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle PAT Visibility",
                                            tint = colors.textMuted
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = colors.bgCard,
                                    unfocusedContainerColor = colors.bgCard,
                                    focusedBorderColor = colors.borderFocus,
                                    unfocusedBorderColor = colors.borderInput,
                                    focusedTextColor = colors.textHeading,
                                    unfocusedTextColor = colors.textPrimary
                                ),
                                singleLine = true
                            )
                        }
                    }

                    // Error Message
                    if (state.probeError != null || state.connectError != null) {
                        Text(
                            text = state.probeError ?: state.connectError ?: "",
                            color = colors.statusFailed,
                            fontSize = 12.sp
                        )
                    }

                    // Connect Action Button
                    Button(
                        onClick = onConnectClicked,
                        enabled = !state.isConnecting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.btnPrimaryBg,
                            contentColor = colors.btnPrimaryText
                        )
                    ) {
                        if (state.isConnecting) {
                            CircularProgressIndicator(
                                color = colors.btnPrimaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "Connect Instance",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionPresetPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OpenshipAppTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.bgPill)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
            fontFamily = FontFamily.Monospace
        )
    }
}
