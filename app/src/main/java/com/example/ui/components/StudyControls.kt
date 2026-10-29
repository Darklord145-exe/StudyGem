package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EngineStatus
import com.example.data.model.StudyMode
import com.example.data.model.SubjectType
import com.example.ui.theme.BrightAmber
import com.example.ui.theme.ChipActiveBg
import com.example.ui.theme.ChipActiveBorder
import com.example.ui.theme.ChipInactiveBg
import com.example.ui.theme.ChipInactiveBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DeepDarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.StopCoral
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividViolet

@Composable
fun SubjectChipsRow(
    selectedSubject: SubjectType,
    onSubjectSelected: (SubjectType) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SubjectType.values().forEach { subject ->
            val isSelected = subject == selectedSubject
            val icon = getSubjectIcon(subject)

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) ChipActiveBg else ChipInactiveBg,
                animationSpec = tween(200),
                label = "chip_bg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) ChipActiveBorder else ChipInactiveBorder,
                animationSpec = tween(200),
                label = "chip_border"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) ElectricCyan else TextSecondary,
                animationSpec = tween(200),
                label = "chip_text"
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .clickable { onSubjectSelected(subject) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("subject_chip_${subject.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = subject.displayName,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = subject.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun ModeSwitcherSegmentedButton(
    selectedMode: StudyMode,
    onModeChanged: (StudyMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = DeepDarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Teach (Socratic) Button
            val isTeach = selectedMode == StudyMode.TEACH
            val teachBg by animateColorAsState(
                targetValue = if (isTeach) NeonEmerald.copy(alpha = 0.18f) else Color.Transparent,
                label = "teach_bg"
            )
            val teachBorder by animateColorAsState(
                targetValue = if (isTeach) NeonEmerald else Color.Transparent,
                label = "teach_border"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(teachBg)
                    .border(1.dp, teachBorder, RoundedCornerShape(8.dp))
                    .clickable { onModeChanged(StudyMode.TEACH) }
                    .padding(vertical = 8.dp)
                    .testTag("mode_switch_teach"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Teach",
                            tint = if (isTeach) NeonEmerald else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Teach",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isTeach) NeonEmerald else TextSecondary
                        )
                    }
                    Text(
                        text = "Socratic (Questions & Hints)",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (isTeach) NeonEmerald.copy(alpha = 0.8f) else TextMuted
                    )
                }
            }

            // Demonstrate (Direct) Button
            val isDemo = selectedMode == StudyMode.DEMONSTRATE
            val demoBg by animateColorAsState(
                targetValue = if (isDemo) ElectricCyan.copy(alpha = 0.18f) else Color.Transparent,
                label = "demo_bg"
            )
            val demoBorder by animateColorAsState(
                targetValue = if (isDemo) ElectricCyan else Color.Transparent,
                label = "demo_border"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(demoBg)
                    .border(1.dp, demoBorder, RoundedCornerShape(8.dp))
                    .clickable { onModeChanged(StudyMode.DEMONSTRATE) }
                    .padding(vertical = 8.dp)
                    .testTag("mode_switch_demonstrate"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DataObject,
                            contentDescription = "Demonstrate",
                            tint = if (isDemo) ElectricCyan else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Demonstrate",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isDemo) ElectricCyan else TextSecondary
                        )
                    }
                    Text(
                        text = "Direct Solutions & Code",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (isDemo) ElectricCyan.copy(alpha = 0.8f) else TextMuted
                    )
                }
            }
        }
    }
}

private data class StatusBadgeConfig(
    val bg: Color,
    val dot: Color,
    val text: String,
    val icon: ImageVector?
)

@Composable
fun ModelStatusBadge(
    status: EngineStatus,
    onStatusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = when (status) {
        is EngineStatus.Loaded -> {
            StatusBadgeConfig(
                NeonEmerald.copy(alpha = 0.15f),
                NeonEmerald,
                "Model Loaded",
                null
            )
        }
        is EngineStatus.MissingWeights -> {
            StatusBadgeConfig(
                BrightAmber.copy(alpha = 0.15f),
                BrightAmber,
                "Model Missing (Tap for ADB)",
                Icons.Default.WarningAmber
            )
        }
        is EngineStatus.DemoMode -> {
            StatusBadgeConfig(
                ElectricCyan.copy(alpha = 0.15f),
                ElectricCyan,
                "Demo Fallback",
                null
            )
        }
        is EngineStatus.Generating -> {
            StatusBadgeConfig(
                VividViolet.copy(alpha = 0.2f),
                VividViolet,
                "Generating...",
                null
            )
        }
        is EngineStatus.Checking -> {
            StatusBadgeConfig(
                DarkSurfaceElevated,
                TextMuted,
                "Checking Weights...",
                null
            )
        }
        is EngineStatus.Error -> {
            StatusBadgeConfig(
                StopCoral.copy(alpha = 0.15f),
                StopCoral,
                "Engine Error",
                Icons.Default.WarningAmber
            )
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(config.bg)
            .clickable { onStatusClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("model_status_badge"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (config.icon != null) {
            Icon(
                imageVector = config.icon,
                contentDescription = null,
                tint = config.dot,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(config.dot)
            )
            Spacer(modifier = Modifier.width(5.dp))
        }

        Text(
            text = config.text,
            style = MaterialTheme.typography.labelSmall,
            color = config.dot,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getSubjectIcon(subject: SubjectType): ImageVector {
    return when (subject) {
        SubjectType.CODING -> Icons.Default.Code
        SubjectType.MATHEMATICS -> Icons.Default.Functions
        SubjectType.SCIENCE -> Icons.Default.Science
        SubjectType.ESSAY -> Icons.Default.EditNote
    }
}
