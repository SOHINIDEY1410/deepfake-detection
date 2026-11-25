package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AuthenticityVerdict
import com.example.data.IndicatorSeverity
import com.example.data.MediaModality
import com.example.ui.theme.LocalClinicalColors
import com.example.ui.theme.MonoCaption
import com.example.ui.theme.MonoMetricLarge
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun MediaModality.iconVector(): ImageVector = when (this) {
    MediaModality.IMAGE -> Icons.Outlined.Image
    MediaModality.VIDEO -> Icons.Outlined.Videocam
    MediaModality.AUDIO -> Icons.Outlined.AudioFile
    MediaModality.TEXT -> Icons.Outlined.Description
}

@Composable
fun verdictAccentColor(verdict: AuthenticityVerdict): Color {
    val clinical = LocalClinicalColors.current
    return when (verdict) {
        AuthenticityVerdict.AUTHENTIC -> clinical.authentic
        AuthenticityVerdict.SUSPICIOUS -> clinical.suspicious
        AuthenticityVerdict.LIKELY_SYNTHETIC -> clinical.synthetic
        AuthenticityVerdict.INCONCLUSIVE -> clinical.inconclusive
    }
}

@Composable
fun verdictContainerColor(verdict: AuthenticityVerdict): Color {
    val clinical = LocalClinicalColors.current
    return when (verdict) {
        AuthenticityVerdict.AUTHENTIC -> clinical.authenticContainer
        AuthenticityVerdict.SUSPICIOUS -> clinical.suspiciousContainer
        AuthenticityVerdict.LIKELY_SYNTHETIC -> clinical.syntheticContainer
        AuthenticityVerdict.INCONCLUSIVE -> clinical.inconclusiveContainer
    }
}

@Composable
fun verdictOnContainerColor(verdict: AuthenticityVerdict): Color {
    val clinical = LocalClinicalColors.current
    return when (verdict) {
        AuthenticityVerdict.AUTHENTIC -> clinical.onAuthenticContainer
        AuthenticityVerdict.SUSPICIOUS -> clinical.onSuspiciousContainer
        AuthenticityVerdict.LIKELY_SYNTHETIC -> clinical.onSyntheticContainer
        AuthenticityVerdict.INCONCLUSIVE -> clinical.onInconclusiveContainer
    }
}

@Composable
fun severityColors(severity: IndicatorSeverity): Pair<Color, Color> {
    val clinical = LocalClinicalColors.current
    return when (severity) {
        IndicatorSeverity.BENIGN -> clinical.authenticContainer to clinical.onAuthenticContainer
        IndicatorSeverity.LOW -> clinical.inconclusiveContainer to clinical.onInconclusiveContainer
        IndicatorSeverity.MEDIUM -> clinical.suspiciousContainer to clinical.onSuspiciousContainer
        IndicatorSeverity.HIGH -> clinical.syntheticContainer to clinical.onSyntheticContainer
    }
}

@Composable
fun VerdictBadge(
    verdict: AuthenticityVerdict,
    modifier: Modifier = Modifier
) {
    val accent = verdictAccentColor(verdict)
    val container = verdictContainerColor(verdict)
    val onContainer = verdictOnContainerColor(verdict)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = container
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Text(
                text = verdict.shortBadge,
                style = MaterialTheme.typography.labelSmall,
                color = onContainer,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Minimalist Swiss-clinical radiographic light-table header canvas.
 * Adapts smoothly to both Clinical Day (light) and Reading Room (dark) themes.
 */
@Composable
fun RadiographicHeaderCard(
    totalAudits: Int,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val clinical = LocalClinicalColors.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, clinical.hairlineBorder)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle architectural radiographic grid & clinical telemetry wave
            Canvas(
                modifier = Modifier
                    .matchParentSize()
            ) {
                val w = size.width
                val h = size.height

                // Atmospheric teal radial glow in top-right quadrant
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            clinical.gridAccent.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.88f, h * 0.22f),
                        radius = w * 0.65f
                    )
                )

                // Precision millimeter grid lines (subtle clinical aesthetic)
                val gridStep = 28.dp.toPx()
                val gridColor = clinical.gridAccent.copy(alpha = 0.06f)
                var x = gridStep
                while (x < w) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f
                    )
                    x += gridStep
                }
                var y = gridStep
                while (y < h) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                    y += gridStep
                }

                // Subtle ECG / signal integrity waveform along bottom-right
                val wavePath = Path().apply {
                    val baseY = h * 0.78f
                    moveTo(w * 0.45f, baseY)
                    lineTo(w * 0.62f, baseY)
                    lineTo(w * 0.65f, baseY - 6.dp.toPx())
                    lineTo(w * 0.68f, baseY + 5.dp.toPx())
                    lineTo(w * 0.72f, baseY - 22.dp.toPx())
                    lineTo(w * 0.76f, baseY + 12.dp.toPx())
                    lineTo(w * 0.79f, baseY - 4.dp.toPx())
                    lineTo(w * 0.83f, baseY)
                    lineTo(w, baseY)
                }
                drawPath(
                    path = wavePath,
                    color = clinical.gridAccent.copy(alpha = 0.22f),
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.primary)
                            )
                            Text(
                                text = "4-MODALITY ENGINE READY",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Text(
                        text = if (totalAudits == 1) "1 LOGGED" else "$totalAudits LOGGED",
                        style = MonoCaption,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = stringResource(R.string.verify_header_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colorScheme.onSurface
                )

                Text(
                    text = stringResource(R.string.verify_header_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Animated circular authenticity gauge with precision graduation ticks and monospace telemetry.
 */
@Composable
fun AuthenticityRadialGauge(
    score: Int,
    verdict: AuthenticityVerdict,
    modifier: Modifier = Modifier
) {
    val accent = verdictAccentColor(verdict)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val tickColor = MaterialTheme.colorScheme.outlineVariant

    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(score) { triggered = true }

    val animatedFraction by animateFloatAsState(
        targetValue = if (triggered) (score.coerceIn(0, 100) / 100f) else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "authenticity_gauge_sweep"
    )

    Box(
        modifier = modifier.size(164.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 12.dp.toPx()
            val inset = strokePx / 2f + 10.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // Outer precision tick marks (24 ticks around the 260-degree sweep)
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerR = size.minDimension / 2f - 2.dp.toPx()
            val innerR = outerR - 4.dp.toPx()
            val tickCount = 24
            for (i in 0..tickCount) {
                val deg = 140.0 + (260.0 * i / tickCount)
                val rad = deg * PI / 180.0
                val p1 = Offset(
                    (center.x + innerR * cos(rad)).toFloat(),
                    (center.y + innerR * sin(rad)).toFloat()
                )
                val p2 = Offset(
                    (center.x + outerR * cos(rad)).toFloat(),
                    (center.y + outerR * sin(rad)).toFloat()
                )
                drawLine(
                    color = tickColor,
                    start = p1,
                    end = p2,
                    strokeWidth = if (i % 6 == 0) 2.2f else 1.2f,
                    cap = StrokeCap.Round
                )
            }

            // Track arc
            drawArc(
                color = trackColor,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active progress arc
            drawArc(
                color = accent,
                startAngle = 140f,
                sweepAngle = 260f * animatedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${score.coerceIn(0, 100)}",
                style = MonoMetricLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "AUTHENTICITY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Minimalist oscilloscope waveform for live clinical audio capture or attached voice clips.
 */
@Composable
fun LiveAudioWaveform(
    amplitudes: List<Float>,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val barColor = if (isActive) activeColor else inactiveColor

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val bars = amplitudes.ifEmpty { List(28) { 0.16f } }
        val count = bars.size
        val gap = 4.dp.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2.dp.toPx())
        val midY = size.height / 2f

        bars.forEachIndexed { idx, amp ->
            val h = (size.height * amp.coerceIn(0.12f, 1.0f)).coerceAtLeast(4.dp.toPx())
            val x = idx * (barWidth + gap)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, midY - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

/**
 * Minimalist vector-style Canvas illustration for empty Audit Log screen.
 */
@Composable
fun AuditEmptyIllustration(
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outline

    Canvas(modifier = modifier.size(136.dp)) {
        val w = size.width
        val h = size.height

        // Subtle circular halo
        drawCircle(
            color = primary.copy(alpha = 0.08f),
            radius = w * 0.44f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // Minimalist clinical folder outline
        drawRoundRect(
            color = surfaceVariant,
            topLeft = Offset(w * 0.2f, h * 0.26f),
            size = Size(w * 0.6f, h * 0.50f),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        drawRoundRect(
            color = outline.copy(alpha = 0.5f),
            topLeft = Offset(w * 0.2f, h * 0.26f),
            size = Size(w * 0.6f, h * 0.50f),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx())
        )

        // Document lines inside
        val lineY1 = h * 0.43f
        val lineY2 = h * 0.54f
        drawLine(
            color = outline.copy(alpha = 0.6f),
            start = Offset(w * 0.30f, lineY1),
            end = Offset(w * 0.60f, lineY1),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = outline.copy(alpha = 0.4f),
            start = Offset(w * 0.30f, lineY2),
            end = Offset(w * 0.52f, lineY2),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Clinical verification shield badge in lower-right
        val shieldCenter = Offset(w * 0.68f, h * 0.66f)
        drawCircle(
            color = primary,
            radius = w * 0.14f,
            center = shieldCenter
        )
        // Medical cross inside shield
        val crossHalf = w * 0.06f
        val crossStroke = 2.4.dp.toPx()
        drawLine(
            color = Color.White,
            start = Offset(shieldCenter.x - crossHalf, shieldCenter.y),
            end = Offset(shieldCenter.x + crossHalf, shieldCenter.y),
            strokeWidth = crossStroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(shieldCenter.x, shieldCenter.y - crossHalf),
            end = Offset(shieldCenter.x, shieldCenter.y + crossHalf),
            strokeWidth = crossStroke,
            cap = StrokeCap.Round
        )
    }
}
