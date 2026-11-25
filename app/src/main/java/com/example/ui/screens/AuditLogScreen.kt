package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AuthenticityVerdict
import com.example.data.MediaModality
import com.example.data.VerificationReport
import com.example.ui.components.AuditEmptyIllustration
import com.example.ui.components.VerdictBadge
import com.example.ui.components.iconVector
import com.example.ui.components.verdictAccentColor
import com.example.ui.theme.LocalClinicalColors
import com.example.ui.theme.MonoCaption
import com.example.ui.theme.MonoMetricMedium
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuditLogScreen(
    allReports: List<VerificationReport>,
    filteredReports: List<VerificationReport>,
    searchQuery: String,
    modalityFilter: MediaModality?,
    verdictFilter: AuthenticityVerdict?,
    onSearchChange: (String) -> Unit,
    onModalityFilterToggle: (MediaModality?) -> Unit,
    onVerdictFilterToggle: (AuthenticityVerdict?) -> Unit,
    onOpenReport: (VerificationReport) -> Unit,
    onDeleteReport: (Long) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val clinical = LocalClinicalColors.current
    var showClearDialog by remember { mutableStateOf(false) }

    val authenticCount = remember(allReports) {
        allReports.count { it.verdict == AuthenticityVerdict.AUTHENTIC }
    }
    val flaggedCount = remember(allReports) {
        allReports.count {
            it.verdict == AuthenticityVerdict.SUSPICIOUS ||
                it.verdict == AuthenticityVerdict.LIKELY_SYNTHETIC
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.confirm_clear_title)) },
            text = { Text(stringResource(R.string.confirm_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearAll()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary KPI Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AuditStatTile(
                    label = "TOTAL SCANS",
                    value = "${allReports.size}",
                    accent = colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                AuditStatTile(
                    label = "AUTHENTIC",
                    value = "$authenticCount",
                    accent = clinical.authentic,
                    modifier = Modifier.weight(1f)
                )
                AuditStatTile(
                    label = "FLAGGED",
                    value = "$flaggedCount",
                    accent = clinical.synthetic,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (allReports.isNotEmpty()) {
            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audit_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text(stringResource(R.string.audit_search_placeholder)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search audit log"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    }
                )
            }

            // Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = modalityFilter == null,
                                onClick = { onModalityFilterToggle(null) },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                                label = { Text("All Modalities") }
                            )
                        }
                        items(MediaModality.entries) { mod ->
                            FilterChip(
                                selected = modalityFilter == mod,
                                onClick = { onModalityFilterToggle(mod) },
                                modifier = Modifier.minimumInteractiveComponentSize(),
                                label = { Text(mod.shortLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorScheme.primaryContainer,
                                    selectedLabelColor = colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                listOf(
                                    AuthenticityVerdict.AUTHENTIC,
                                    AuthenticityVerdict.SUSPICIOUS,
                                    AuthenticityVerdict.LIKELY_SYNTHETIC
                                )
                            ) { v ->
                                FilterChip(
                                    selected = verdictFilter == v,
                                    onClick = { onVerdictFilterToggle(v) },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                    label = { Text(v.shortBadge) }
                                )
                            }
                        }
                        TextButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_audit_history_button")
                        ) {
                            Text(
                                text = stringResource(R.string.action_clear_audit),
                                color = colorScheme.error,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        if (allReports.isEmpty()) {
            item {
                EmptyAuditCard()
            }
        } else if (filteredReports.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = colorScheme.surface,
                    border = BorderStroke(1.dp, clinical.hairlineBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No matching records",
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Try adjusting your search query or clearing the modality and verdict filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredReports, key = { it.id }) { report ->
                AuditReportCard(
                    report = report,
                    onClick = { onOpenReport(report) },
                    onDelete = { onDeleteReport(report.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun AuditStatTile(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val clinical = LocalClinicalColors.current

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, clinical.hairlineBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MonoMetricMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AuditReportCard(
    report: VerificationReport,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val clinical = LocalClinicalColors.current
    val dateLabel = remember(report.timestampMillis) {
        SimpleDateFormat("MMM dd · HH:mm", Locale.US).format(Date(report.timestampMillis))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("audit_item_${report.id}"),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, clinical.hairlineBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colorScheme.surfaceContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = report.modality.iconVector(),
                            contentDescription = report.modality.label,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.mediaLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(report.specialty.label)
                            if (report.caseReference.isNotBlank()) {
                                append(" · ${report.caseReference}")
                            }
                            append(" · $dateLabel")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_audit_item_${report.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete audit record",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = report.executiveSummary,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VerdictBadge(verdict = report.verdict)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SHA ${report.sha256Hash.take(8)}",
                        style = MonoCaption,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${report.authenticityScore}/100",
                        style = MonoCaption,
                        color = verdictAccentColor(report.verdict),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAuditCard() {
    val colorScheme = MaterialTheme.colorScheme
    val clinical = LocalClinicalColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, clinical.hairlineBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AuditEmptyIllustration()
            Text(
                text = stringResource(R.string.audit_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.audit_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
