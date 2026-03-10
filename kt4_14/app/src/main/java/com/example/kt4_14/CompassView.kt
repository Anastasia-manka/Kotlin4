package com.example.kt4_14

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassView(
    azimuth: Float,
    modifier: Modifier = Modifier
) {
    // Плавная интерполяция
    val smoothAzimuth = remember { mutableStateOf(azimuth) }

    LaunchedEffect(azimuth) {
        val target = azimuth
        val start = smoothAzimuth.value
        var diff = target - start
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360

        val steps = 20
        for (i in 1..steps) {
            smoothAzimuth.value = (start + diff * i / steps + 360) % 360
            delay(16)
        }
        smoothAzimuth.value = target
    }

    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.size(320.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 - 40f  // Радиус круга
        val textRadius = radius + 25f

        // Рисуем круг компаса
        drawCircle(
            color = Color.LightGray,
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
        )


        // Север (N) - точно сверху
        drawText(
            textMeasurer = textMeasurer,
            text = "N",
            topLeft = Offset(center.x - 10f, center.y - textRadius - 20f),
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 28.sp,
                color = Color.Red,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        )

        // Рисуем градусные метки
        for (i in 0 until 360 step 30) {
            val angle = Math.toRadians(i.toDouble()).toFloat()
            val startX = center.x + (radius - 15) * cos(angle)
            val startY = center.y + (radius - 15) * sin(angle)
            val endX = center.x + radius * cos(angle)
            val endY = center.y + radius * sin(angle)

            drawLine(
                color = Color.Gray,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = if (i % 90 == 0) 3f else 1f
            )
        }

        // Стрелка компаса
        rotate(degrees = -smoothAzimuth.value, pivot = center) {
            // Северная часть (красная)
            drawLine(
                color = Color.Red,
                start = center,
                end = Offset(center.x, center.y - radius + 20f),
                strokeWidth = 8f
            )

            // Южная часть (серая)
            drawLine(
                color = Color.Gray,
                start = center,
                end = Offset(center.x, center.y + radius - 40f),
                strokeWidth = 6f
            )

            // Наконечники
            drawCircle(
                color = Color.Red,
                radius = 12f,
                center = Offset(center.x, center.y - radius + 30f)
            )

            drawCircle(
                color = Color.Gray,
                radius = 8f,
                center = Offset(center.x, center.y + radius - 50f)
            )
        }

        // Центральная точка
        drawCircle(
            color = Color.Black,
            radius = 10f,
            center = center
        )
    }
}