package com.kareemessam.openship.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kareemessam.openship.shared.model.InstanceConfig
import com.kareemessam.openship.shared.ui.theme.OpenshipAppTheme

@Composable
fun InstanceSwitcherDropdown(
    activeInstance: InstanceConfig?,
    allInstances: List<InstanceConfig>,
    onInstanceSelected: (String) -> Unit,
    onAddInstanceClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = OpenshipAppTheme.colors

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(colors.bgCardElevated)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.statusActive)
            )

            Text(
                text = activeInstance?.label ?: "Select Server",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textHeading,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Switch Instance",
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(colors.bgCard)
                .border(1.dp, colors.borderCard, RoundedCornerShape(12.dp))
        ) {
            Text(
                text = "SWITCH INSTANCE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            allInstances.forEach { instance ->
                val isSelected = instance.id == activeInstance?.id
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = instance.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.textHeading else colors.textPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = instance.url,
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = colors.statusActive,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(16.dp))
                        }
                    },
                    onClick = {
                        expanded = false
                        onInstanceSelected(instance.id)
                    }
                )
            }

            HorizontalDivider(color = colors.borderSubtle, modifier = Modifier.padding(vertical = 4.dp))

            DropdownMenuItem(
                text = {
                    Text(
                        text = "+ Connect New Server",
                        color = colors.btnPrimaryBg,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = colors.btnPrimaryBg,
                        modifier = Modifier.size(16.dp)
                    )
                },
                onClick = {
                    expanded = false
                    onAddInstanceClicked()
                }
            )
        }
    }
}
