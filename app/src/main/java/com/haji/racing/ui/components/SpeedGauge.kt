package com.haji.racing.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haji.racing.ui.theme.RacingCardElevated
import com.haji.racing.ui.theme.RacingEnd
import com.haji.racing.ui.theme.RacingPrimary
import com.haji.racing.ui.theme.RacingPrimaryLight
import com.haji.racing.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

/**
 * 圆形速度表：270° 弧形刻度 + 渐变进度 + 中央数字
 */
@Composable
fun SpeedGauge(
    speedKmh: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 240.dp,
    maxKmh: Float = 160f,
) {
    val animated by animateFloatAsState(
        targetValue = speedKmh,
        animationSpec = tween(durationMillis = 350),
        label = "gauge",
    )

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasCenter = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f * 0.80f
            val strokeW = size.minDimension * 0.055f
            val arcSize = Size(radius * 2f, radius * 2f)
            val topLeft = Offset(canvasCenter.x - radius, canvasCenter.y - radius)
            val startAngle = 135f
            val sweepTotal = 270f

            // 背景弧
            drawArc(
                color = RacingCardElevated,
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeW, cap = StrokeCap.Round),
            )

            // 进度弧
            val progress = (animated / maxKmh).coerceIn(0f, 1f)
            if (progress > 0.003f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            RacingPrimaryLight,
                            RacingPrimary,
                            RacingEnd,
                        ),
                        center = canvasCenter,
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweepTotal * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round),
                )
            }

            // 刻度
            var v = 0f
            while (v <= maxKmh) {
                val angle = startAngle + sweepTotal * (v / maxKmh)
                val isMajor = (v % 30f) == 0f
                val outer = radius - strokeW * 0.9f
                val inner = outer - (if (isMajor) strokeW * 0.9f else strokeW * 0.45f)
                val rad = Math.toRadians(angle.toDouble())
                val cosA = cos(rad).toFloat()
                val sinA = sin(rad).toFloat()
                drawLine(
                    color = if (isMajor) Color(0xFF98A2B8) else Color(0xFF4A5568),
                    start = Offset(canvasCenter.x + cosA * inner, canvasCenter.y + sinA * inner),
                    end = Offset(canvasCenter.x + cosA * outer, canvasCenter.y + sinA * outer),
                    strokeWidth = if (isMajor) 2.5f else 1.2f,
                )
                v += 10f
            }
        }

        // 中央数字
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%.0f".format(animated),
                color = if (animated > maxKmh * 0.85f) RacingEnd else Color.White,
                fontSize = (diameter.value * 0.24f).sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = "km/h",
                color = TextSecondary,
                fontSize = (diameter.value * 0.075f).sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            )
        }
    }
}
