package com.budgetkhata.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.budgetkhata.R
import com.budgetkhata.core.Money

data class EventBar(
    val label: String,
    val collectedPaise: Long,
    val pendingPaise: Long
)

@Composable
fun DonutChart(
    percent: Float,
    centerLabel: String,
    centerCaption: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 132.dp,
    strokeWidth: Dp = 14.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val arcSize = this.size.minDimension - stroke
            val topLeft = Offset(
                x = (this.size.width - arcSize) / 2f,
                y = (this.size.height - arcSize) / 2f
            )
            val arcDimensions = Size(arcSize, arcSize)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcDimensions,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            val sweep = 360f * (percent.coerceIn(0f, 100f) / 100f)
            if (sweep > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcDimensions,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = centerCaption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CollectionBarChart(
    items: List<EventBar>,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 128.dp
) {
    if (items.isEmpty()) return

    val paidColor = com.budgetkhata.ui.theme.LocalStatusPalette.current.paid
    val pendingColor = com.budgetkhata.ui.theme.LocalStatusPalette.current.pending
    val maxTotal = items.maxOf { it.collectedPaise + it.pendingPaise }.coerceAtLeast(1L)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(maxHeight + 34.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        items.take(6).forEach { item ->
            val total = (item.collectedPaise + item.pendingPaise).coerceAtLeast(0L)
            val totalFraction = (total.toFloat() / maxTotal.toFloat()).coerceIn(0f, 1f)
            val pendingFraction = if (total <= 0L) 0f else item.pendingPaise.toFloat() / total.toFloat()
            val collectedFraction = if (total <= 0L) 0f else item.collectedPaise.toFloat() / total.toFloat()

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .height(maxHeight)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    val usableHeight = maxHeight - 4.dp
                    val barHeight = usableHeight * totalFraction
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((barHeight * pendingFraction).coerceAtLeast(0.dp))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(pendingColor.copy(alpha = 0.75f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((barHeight * collectedFraction).coerceAtLeast(0.dp))
                            .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                            .background(paidColor)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = Money.format(item.collectedPaise),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 6.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

@Composable
fun ChartLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendDot(stringResource(R.string.collected), com.budgetkhata.ui.theme.LocalStatusPalette.current.paid)
        LegendDot(stringResource(R.string.pending), com.budgetkhata.ui.theme.LocalStatusPalette.current.pending)
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(Modifier.size(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 2.dp)
        )
    }
}
