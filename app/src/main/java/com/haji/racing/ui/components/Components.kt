package com.haji.racing.ui.components

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haji.racing.ui.theme.RacingAccent
import com.haji.racing.ui.theme.RacingCard
import com.haji.racing.ui.theme.RacingCardElevated
import com.haji.racing.ui.theme.RacingEnd
import com.haji.racing.ui.theme.RacingOutline
import com.haji.racing.ui.theme.RacingPrimary
import com.haji.racing.ui.theme.RacingPrimaryDark
import com.haji.racing.ui.theme.RacingPrimaryLight
import com.haji.racing.ui.theme.RacingStart
import com.haji.racing.ui.theme.RacingWarn
import com.haji.racing.ui.theme.TextPrimary
import com.haji.racing.ui.theme.TextSecondary
import com.haji.racing.ui.theme.TextTertiary

// =====================================================================
// 卡片
// =====================================================================

@Composable
fun RacingCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(RacingCardElevated, RacingCard),
                ),
            )
            .border(1.dp, RacingOutline.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

/** 区块标题：大写小标签风格 */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(listOf(RacingPrimaryLight, RacingPrimary)),
                ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

// =====================================================================
// 统计瓦片
// =====================================================================

@Composable
fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary,
    accent: Color = RacingPrimary,
    valueStyle: TextStyle = MaterialTheme.typography.titleLarge,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(RacingCard)
            .border(1.dp, RacingOutline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = valueStyle,
            color = valueColor,
            maxLines = 1,
        )
    }
}

// =====================================================================
// 状态 Chip
// =====================================================================

@Composable
fun StatusChip(
    text: String,
    color: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    active: Boolean = true,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(color.copy(alpha = if (active) 0.16f else 0.08f))
            .border(
                width = 1.dp,
                brush = SolidColor(color.copy(alpha = if (active) 0.6f else 0.25f)),
                shape = RoundedCornerShape(50.dp),
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color.copy(alpha = if (active) 1f else 0.5f),
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = if (active) 1f else 0.6f),
        )
    }
}

/** 完成/未完成状态 chip */
@Composable
fun RecordingStatusChip(status: String, modifier: Modifier = Modifier) {
    if (status == "completed") {
        StatusChip(
            text = "完成",
            color = RacingStart,
            icon = Icons.Filled.CheckCircle,
            modifier = modifier,
        )
    } else {
        StatusChip(
            text = "未完成",
            color = RacingWarn,
            icon = Icons.Filled.Close,
            modifier = modifier,
            active = false,
        )
    }
}

/** GPS 状态 pill */
@Composable
fun GpsPill(ready: Boolean, accuracyMeters: Float?, modifier: Modifier = Modifier) {
    val color = if (ready) RacingStart else RacingWarn
    StatusChip(
        text = if (ready) {
            if (accuracyMeters != null && accuracyMeters < Float.MAX_VALUE) {
                "GPS 信号 ${accuracyMeters.toInt()}m"
            } else {
                "GPS 已就绪"
            }
        } else {
            "正在获取 GPS…"
        },
        color = color,
        icon = if (ready) Icons.Filled.MyLocation else Icons.Filled.GpsNotFixed,
        modifier = modifier,
    )
}

// =====================================================================
// 按钮
// =====================================================================

@Composable
fun RacingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.Primary,
) {
    val (brush, textColor, border) = when (variant) {
        ButtonVariant.Primary -> Triple(
            Brush.linearGradient(listOf(RacingPrimaryLight, RacingPrimary)),
            Color.White,
            null,
        )
        ButtonVariant.Outline -> Triple(null, RacingPrimary, RacingPrimary.copy(alpha = 0.6f))
        ButtonVariant.Danger -> Triple(null, RacingEnd, RacingEnd.copy(alpha = 0.6f))
        ButtonVariant.Ghost -> Triple(null, TextSecondary, RacingOutline)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (brush != null) Modifier.background(brush)
                else Modifier.background(Color.Transparent)
                    .border(1.dp, border!!, RoundedCornerShape(16.dp)),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor.copy(alpha = if (enabled) 1f else 0.4f),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = if (enabled) 1f else 0.4f),
            )
        }
    }
}

enum class ButtonVariant { Primary, Outline, Danger, Ghost }

/**
 * 大圆形启动按钮（带发光效果）
 */
@Composable
fun GlowStartButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    running: Boolean = false,
    icon: ImageVector,
) {
    val accent = if (running) RacingEnd else RacingPrimary
    Box(
        modifier = modifier.size(112.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 外圈发光
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f)),
        )
        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(
                    elevation = 28.dp,
                    shape = CircleShape,
                    ambientColor = accent.copy(alpha = 0.55f),
                    spotColor = accent.copy(alpha = 0.55f),
                )
                .clip(CircleShape)
                .background(
                    if (enabled) {
                        Brush.linearGradient(listOf(RacingPrimaryLight, RacingPrimaryDark))
                    } else {
                        Brush.linearGradient(listOf(RacingCardElevated, RacingCard))
                    }
                )
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) Color.White else TextTertiary,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

// =====================================================================
// 分段模式选择
// =====================================================================

@Composable
fun ModeSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RacingCard)
            .border(1.dp, RacingOutline, RoundedCornerShape(16.dp))
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(Brush.linearGradient(listOf(RacingPrimaryLight, RacingPrimary)))
                        } else {
                            Modifier
                        },
                    )
                    .clickable(onClick = { onSelect(index) })
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else TextSecondary,
                )
            }
        }
    }
}

// =====================================================================
// 空状态
// =====================================================================

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(RacingCard)
                .border(1.dp, RacingOutline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            RacingButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.width(180.dp),
            )
        }
    }
}

// =====================================================================
// 确认对话框
// =====================================================================

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确认",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RacingCardElevated,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(text = title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Text(text = message, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = if (destructive) RacingEnd else RacingPrimaryLight,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消", color = TextSecondary)
            }
        },
    )
}

// =====================================================================
// 顶部栏标题
// =====================================================================

@Composable
fun ScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
    }
}

/** 数字格式化辅助 */
@Composable
fun TrendingIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
        contentDescription = null,
        tint = RacingAccent,
        modifier = modifier,
    )
}

/** 输入框（深色风格） */
@Composable
fun RacingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RacingCard)
                .border(1.dp, RacingOutline, RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = singleLine,
                maxLines = maxLines,
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(RacingPrimary),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = TextTertiary,
                            fontSize = 15.sp,
                        )
                    }
                    inner()
                },
            )
        }
    }
}
