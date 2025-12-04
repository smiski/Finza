// Caleb Mercier - 2025
// Completed with assistance from Chat GPT & Gemini
package com.example.finza.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- Helper: multiplatform-safe money formatter ----------
fun formatMoney(value: Double): String {
    val rounded = value.toLong()
    val chars = rounded.toString().toCharArray()
    val result = StringBuilder()

    var count = 0
    for (i in chars.indices.reversed()) {
        result.append(chars[i])
        count++
        if (count == 3 && i != 0) {
            result.append(',')
            count = 0
        }
    }

    return "$" + result.reverse().toString()
}


// ---------- Chart with bars + line + tooltip ----------
@Composable
fun RetirementChart(
    balances: List<Double>,
    agesPerYear: List<List<String>>,
    years: List<Int>,
    modifier: Modifier = Modifier
) {
    if (balances.isEmpty()) {
        Text("Run the simulation to see your retirement projection.")
        return
    }

    var hoveredIndex by remember { mutableStateOf<Int?>(null) }
    var hoverPosition by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val density = LocalDensity.current

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopStart
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .pointerInput(balances, agesPerYear) {
                    detectTapGestures { offset ->
                        if (canvasSize.width <= 0 || balances.isEmpty()) return@detectTapGestures

                        val barCount = balances.size
                        val barSpacingPx = with(density) { 4.dp.toPx() }
                        val totalSpacing = barSpacingPx * (barCount + 1)
                        val barWidth =
                            ((canvasSize.width.toFloat() - totalSpacing) / barCount)
                                .coerceAtLeast(1f)

                        val x = offset.x
                        val indexFloat = (x - barSpacingPx) / (barWidth + barSpacingPx)
                        val index = indexFloat.toInt()

                        if (index in balances.indices) {
                            hoveredIndex = index
                            hoverPosition = offset
                        } else {
                            hoveredIndex = null
                            hoverPosition = null
                        }
                    }
                }
        ) {

            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            val maxBalance = (balances.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
            val barCount = balances.size
            val barSpacingPx = with(density) { 4.dp.toPx() }
            val totalSpacing = barSpacingPx * (barCount + 1)
            val barWidth =
                ((width - totalSpacing) / barCount).coerceAtLeast(1f)

            // Draw bars
            for (i in balances.indices) {
                val value = balances[i].toFloat()
                val ratio = (value / maxBalance.toFloat()).coerceIn(0f, 1f)
                val barHeight = ratio * height

                val left = barSpacingPx + i * (barWidth + barSpacingPx)
                val top = height - barHeight

                drawRect(
                    color = Color(0xFF90CAF9), // light blue
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight)
                )
            }

            // Draw trend line across bar centers
            if (balances.size >= 2) {
                val path = Path()
                balances.forEachIndexed { i, value ->
                    val ratio = (value.toFloat() / maxBalance.toFloat()).coerceIn(0f, 1f)
                    val y = height - ratio * height
                    val left = barSpacingPx + i * (barWidth + barSpacingPx)
                    val centerX = left + barWidth / 2f
                    if (i == 0) {
                        path.moveTo(centerX, y)
                    } else {
                        path.lineTo(centerX, y)
                    }
                }

                drawPath(
                    path = path,
                    color = Color(0xFF1976D2), // darker blue
                    style = Stroke(
                        width = with(density) { 2.dp.toPx() },
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        // Tooltip overlay
        val idx = hoveredIndex
        val pos = hoverPosition

        if (idx != null && idx in balances.indices && pos != null) {
            val agesForYear = agesPerYear.getOrNull(idx)
            val ageLabel = agesForYear
                ?.takeIf { it.isNotEmpty() }
                ?.joinToString("/") ?: "N/A"

            val balance = balances[idx]
            val formattedBalance = formatMoney(balance)

            val tooltipWidthDp = 160.dp
            val tooltipHeightDp = 80.dp

            val xDp = with(density) { pos.x.toDp() }
            val yDp = with(density) { pos.y.toDp() }

            val adjustedX = (xDp - tooltipWidthDp / 2)
                .coerceAtLeast(0.dp)
            val adjustedY = (yDp - tooltipHeightDp - 8.dp)
                .coerceAtLeast(0.dp)

            Surface(
                tonalElevation = 8.dp,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .offset(x = adjustedX, y = adjustedY)
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    val yearLabel = years.getOrNull(idx) ?: (idx + 1)

                    Text(
                        "Year $yearLabel",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    )
                    Text(
                        "Age(s): $ageLabel",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Average: $formattedBalance",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}