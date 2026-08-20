package com.haji.racing.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haji.racing.ui.theme.RacingEnd
import com.haji.racing.ui.theme.RacingPrimary
import com.haji.racing.ui.theme.RacingPrimaryLight
import com.haji.racing.ui.theme.TextSecondary
import com.haji.racing.ui.theme.TextTertiary

/**
 * 速度曲线图：时间(横轴) x 速度 km/h(纵轴)
 * @param samples 每秒采样速度（km/h）
 * @param maxSpeed 曲线最大速度（用于比例）
 */
@Composable
fun SpeedChart(
    samples: List<Float>,
    maxSpeed: Float,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
) {
    val data = downsample(samples, 240)
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            if (data.size < 2) return@Canvas
            val w = size.width
            val h = size.height
            val padL = 6f
            val padR = 6f
            val padT = 14f
            val padB = 6f
            val plotW = w - padL - padR
            val plotH = h - padT - padB
            val safeMax = maxSpeed.coerceAtLeast(10f)

            fun xAt(i: Int): Float = padL + plotW * i / (data.size - 1)
            fun yAt(v: Float): Float =
                padT + plotH * (1f - (v / safeMax).coerceIn(0f, 1f))

            // 网格线（25/50/75/100%）
            for (frac in listOf(0.25f, 0.5f, 0.75f)) {
                val y = padT + plotH * frac
                drawLine(
                    color = androidx.compose.ui.graphics.Color(0x30FFFFFF),
                    start = Offset(padL, y),
                    end = Offset(w - padR, y),
                    strokeWidth = 1f,
                )
            }

            val linePath = Path()
            data.forEachIndexed { i, v ->
                val x = xAt(i)
                val y = yAt(v)
                if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }

            // 面积
            val areaPath = Path().apply {
                addPath(linePath)
                lineTo(xAt(data.size - 1), padT + plotH)
                lineTo(xAt(0), padT + plotH)
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        RacingPrimary.copy(alpha = 0.32f),
                        RacingPrimary.copy(alpha = 0.02f),
                    ),
                    startY = padT,
                    endY = padT + plotH,
                ),
            )

            // 曲线
            drawPath(
                path = linePath,
                brush = Brush.linearGradient(
                    colors = listOf(RacingPrimaryLight, RacingPrimary),
                ),
                style = Stroke(width = 2.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )

            // 最高点标记
            val maxIdx = data.indices.maxByOrNull { data[it] } ?: 0
            val maxPoint = Offset(xAt(maxIdx), yAt(data[maxIdx]))
            drawCircle(color = RacingEnd, radius = 4.5f, center = maxPoint)
            drawCircle(
                color = RacingEnd.copy(alpha = 0.3f),
                radius = 9f,
                center = maxPoint,
            )
        }

        Spacer(Modifier.height(6.dp))
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Text(
                text = "最大 ${"%.0f".format(maxSpeed)} km/h",
                color = RacingEnd,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${samples.size} 个采样点 · 全程",
                color = TextTertiary,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "速度 - 时间曲线",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier,
        )
    }
}

private fun downsample(samples: List<Float>, target: Int): List<Float> {
    if (samples.size <= target) return samples
    val step = samples.size.toFloat() / target
    return List(target) { i ->
        samples[(i * step).toInt().coerceAtMost(samples.size - 1)]
    }
}
