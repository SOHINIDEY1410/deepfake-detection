package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ForensicIndicator
import com.example.data.ForensicMetric
import com.example.data.VerificationReport
import com.example.ui.components.AuthenticityRadialGauge
import com.example.ui.components.VerdictBadge
import com.example.ui.components.iconVector
import com.example.ui.components.severityColors
import com.example.ui.components.verdictAccentColor
import com.example.ui.theme.LocalClinicalColors
import com.example.ui.theme.MonoCaption
import com.example.ui.theme.MonoMetricMedium
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    report: VerificationReport,
    shareableText: String,
    onBack: () -> Unit,
    onNewVerification: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val colorScheme = MaterialTheme.colorScheme
    val clinical = LocalClinicalColors.current
    var hashCopied by remember(report.sha256Hash) { mutableStateOf(false) }

    val formattedDate = remember(report.timestampMillis) {
        SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.US).format(Date(report.timestampMillis))
    }

    val shareAction = {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "VeriMed Forensic Report — ${report.mediaLabel}")
            putExtra(Intent.EXTRA_TEXT, shareableText)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export Forensic Summary"))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.report_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${report.modality.shortLabel} · ${report.specialty.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("report_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = shareAction,
                        modifier = Modifier.testTag("report_share_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = stringResource(R.string.cd_share_report)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Verdict Hero Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = colorScheme.surface,
                    border = BorderStroke(1.dp, clinical.hairlineBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VerdictBadge(verdict = report.verdict)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colorScheme.surfaceContainer
                            ) {
                                Text(
                                    text = report.confidence.label.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        AuthenticityRadialGauge(
                            score = report.authenticityScore,
                            verdict = report.verdict
                        )

                        Text(
                            text = report.verdict.displayTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = colorScheme.onSurface
                        )

                        HorizontalDivider(color = clinical.hairlineBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ScorePillStat(
                                label = "AUTHENTICITY",
                                value = "${report.authenticityScore}%",
                                highlight = verdictAccentColor(report.verdict)
                            )
                            ScorePillStat(
                                label = "SYNTHETIC RISK",
                                value = "${report.syntheticRiskScore}%",
                                highlight = colorScheme.onSurface
                            )
                            ScorePillStat(
                                label = "SIGNALS",
                                value = "${report.indicators.size}",
                                highlight = colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 2. Clinical Assessment Summary
            item {
                ReportSectionCard(title = stringResource(R.string.section_executive_summary)) {
                    Text(
                        text = report.executiveSummary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onSurface
                    )
                }
            }

            // 3. Artifact Telemetry Metrics
            if (report.metrics.isNotEmpty()) {
                item {
                    ReportSectionCard(title = stringResource(R.string.section_forensic_metrics)) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            report.metrics.forEach { metric ->
                                MetricTelemetryRow(metric = metric)
                            }
                        }
                    }
                }
            }

            // 4. Detected Forensic Signals
            if (report.indicators.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.section_forensic_signals),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(report.indicators) { indicator ->
                    IndicatorCard(indicator = indicator)
                }
            }

            // 5. Clinical Recommendation
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.primaryContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MedicalServices,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.section_clinical_recommendation),
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = report.clinicalRecommendation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 6. Media Provenance & SHA-256 Chain of Custody
            item {
                ReportSectionCard(title = stringResource(R.string.section_chain_of_custody)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProvenanceRow("Source", report.mediaLabel)
                        ProvenanceRow("Metadata", report.mediaMetadata)
                        if (report.caseReference.isNotBlank()) {
                            ProvenanceRow("Case Ref", report.caseReference)
                        }
                        ProvenanceRow("Context", "${report.modality.label} · ${report.specialty.label}")
                        ProvenanceRow("Timestamp", formattedDate)

                        HorizontalDivider(color = clinical.hairlineBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SHA-256 DIGEST",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = report.sha256Hash,
                                    style = MonoCaption,
                                    color = colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(report.sha256Hash))
                                    hashCopied = true
                                },
                                modifier = Modifier.testTag("copy_sha256_button")
                            ) {
                                Icon(
                                    imageVector = if (hashCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                                    contentDescription = stringResource(R.string.cd_copy_hash),
                                    tint = if (hashCopied) colorScheme.primary else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 7. Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = shareAction,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("export_report_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_export_report))
                    }
                    Button(
                        onClick = onNewVerification,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("new_verification_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_new_scan))
                    }
                }
            }

            // 8. Clinical Disclaimer
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.clinical_disclaimer),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ScorePillStat(
    label: String,
    value: String,
    highlight: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MonoMetricMedium,
            color = highlight
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReportSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val clinical = LocalClinicalColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, clinical.hairlineBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun MetricTelemetryRow(metric: ForensicMetric) {
    val clinical = LocalClinicalColors.current
    val colorScheme = MaterialTheme.colorScheme
    val barColor = when {
        metric.score >= 70 -> clinical.authentic
        metric.score >= 45 -> clinical.suspicious
        else -> clinical.synthetic
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = metric.name,
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${metric.score}/100",
                style = MonoCaption,
                color = barColor,
                fontWeight = FontWeight.SemiBold
            )
        }
        LinearProgressIndicator(
            progress = { metric.score.coerceIn(0, 100) / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = colorScheme.surfaceVariant
        )
        if (metric.detail.isNotBlank()) {
            Text(
                text = metric.detail,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IndicatorCard(indicator: ForensicIndicator) {
    val colorScheme = MaterialTheme.colorScheme
    val clinical = LocalClinicalColors.current
    val (badgeBg, badgeFg) = severityColors(indicator.severity)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, clinical.hairlineBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = indicator.category,
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = indicator.severity.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text = indicator.finding,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProvenanceRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
