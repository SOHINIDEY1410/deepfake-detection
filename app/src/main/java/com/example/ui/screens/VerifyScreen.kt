package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.AnalysisUiState
import com.example.data.ClinicalSpecialty
import com.example.data.MediaAttachment
import com.example.data.MediaHelper
import com.example.data.MediaModality
import com.example.ui.AudioRecorderUiState
import com.example.ui.components.LiveAudioWaveform
import com.example.ui.components.RadiographicHeaderCard
import com.example.ui.components.iconVector
import com.example.ui.theme.LocalClinicalColors
import com.example.ui.theme.MonoCaption

@Composable
fun VerifyScreen(
    totalAudits: Int,
    selectedModality: MediaModality,
    selectedSpecialty: ClinicalSpecialty,
    caseReference: String,
    clinicalText: String,
    imageAttachment: MediaAttachment?,
    videoAttachment: MediaAttachment?,
    audioAttachment: MediaAttachment?,
    audioRecorderState: AudioRecorderUiState,
    analysisState: AnalysisUiState,
    canAnalyze: Boolean,
    onSelectModality: (MediaModality) -> Unit,
    onSelectSpecialty: (ClinicalSpecialty) -> Unit,
    onCaseRefChange: (String) -> Unit,
    onClinicalTextChange: (String) -> Unit,
    onLoadSampleNote: () -> Unit,
    onImageSelected: (android.net.Uri) -> Unit,
    onImageCaptured: (android.graphics.Bitmap) -> Unit,
    onClearImage: () -> Unit,
    onVideoSelected: (android.net.Uri) -> Unit,
    onClearVideo: () -> Unit,
    onAudioSelected: (android.net.Uri) -> Unit,
    onClearAudio: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onToggleAudioPlayback: () -> Unit,
    onRunAnalysis: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var micPermissionDenied by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) onImageSelected(uri) }
    )

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap -> if (bitmap != null) onImageCaptured(bitmap) }
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) onVideoSelected(uri) }
    )

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> if (uri != null) onAudioSelected(uri) }
    )

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            micPermissionDenied = !granted
            if (granted) {
                onStartRecording()
            }
        }
    )

    val isAnalyzing = analysisState is AnalysisUiState.Analyzing

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            RadiographicHeaderCard(totalAudits = totalAudits)
        }

        item {
            ModalitySegmentedBar(
                selected = selectedModality,
                onSelect = onSelectModality
            )
        }

        item {
            ClinicalSpecialtySection(
                selected = selectedSpecialty,
                onSelect = onSelectSpecialty
            )
        }

        item {
            when (selectedModality) {
                MediaModality.IMAGE -> ImageWorkspaceCard(
                    attachment = imageAttachment,
                    onPickImage = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onCapturePhoto = { cameraPreviewLauncher.launch(null) },
                    onClear = onClearImage
                )

                MediaModality.VIDEO -> VideoWorkspaceCard(
                    attachment = videoAttachment,
                    onPickVideo = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    onClear = onClearVideo
                )

                MediaModality.AUDIO -> AudioWorkspaceCard(
                    attachment = audioAttachment,
                    recorderState = audioRecorderState,
                    micPermissionDenied = micPermissionDenied,
                    onPickAudio = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                    onToggleRecord = {
                        if (audioRecorderState.isRecording) {
                            onStopRecording()
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                micPermissionDenied = false
                                onStartRecording()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    onTogglePlay = onToggleAudioPlayback,
                    onClear = onClearAudio
                )

                MediaModality.TEXT -> TextWorkspaceCard(
                    text = clinicalText,
                    onTextChange = onClinicalTextChange,
                    onLoadSample = onLoadSampleNote
                )
            }
        }

        item {
            CaseReferenceCard(
                value = caseReference,
                onValueChange = onCaseRefChange
            )
        }

        item {
            AnimatedVisibility(
                visible = analysisState is AnalysisUiState.Analyzing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val state = analysisState as? AnalysisUiState.Analyzing
                if (state != null) {
                    AnalyzingProgressCard(
                        stageLabel = state.stageLabel,
                        stepIndex = state.stepIndex
                    )
                }
            }

            AnimatedVisibility(
                visible = analysisState is AnalysisUiState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val errorState = analysisState as? AnalysisUiState.Error
                if (errorState != null) {
                    ForensicErrorCard(
                        message = errorState.message,
                        onRetry = onRunAnalysis,
                        onDismiss = onDismissError
                    )
                }
            }
        }

        item {
            Button(
                onClick = onRunAnalysis,
                enabled = canAnalyze && !isAnalyzing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("run_analysis_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isAnalyzing) {
                        stringResource(R.string.action_analyzing)
                    } else {
                        stringResource(R.string.action_run_analysis)
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ModalitySegmentedBar(
    selected: MediaModality,
    onSelect: (MediaModality) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val clinical = LocalClinicalColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, clinical.hairlineBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MediaModality.entries.forEach { modality ->
                val isSelected = modality == selected
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(modality) }
                        .testTag("modality_tab_${modality.name.lowercase()}"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) colorScheme.surface else colorScheme.surfaceContainer,
                    border = if (isSelected) BorderStroke(1.dp, clinical.hairlineBorder) else null,
                    tonalElevation = if (isSelected) 1.dp else 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = modality.iconVector(),
                            contentDescription = modality.label,
                            modifier = Modifier.size(19.dp),
                            tint = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = modality.shortLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClinicalSpecialtySection(
    selected: ClinicalSpecialty,
    onSelect: (ClinicalSpecialty) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.clinical_context_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ClinicalSpecialty.entries) { specialty ->
                val isSelected = specialty == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(specialty) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("specialty_chip_${specialty.name.lowercase()}"),
                    label = {
                        Text(
                            text = specialty.label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        Text(
            text = selected.focusSummary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ImageWorkspaceCard(
    attachment: MediaAttachment?,
    onPickImage: () -> Unit,
    onCapturePhoto: () -> Unit,
    onClear: () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (attachment == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colorScheme.surfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.image_drop_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.image_drop_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickImage,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("choose_image_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_choose_image))
                    }
                    OutlinedButton(
                        onClick = onCapturePhoto,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("capture_photo_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = stringResource(R.string.action_capture_photo),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_capture_photo))
                    }
                }
            } else {
                AttachmentHeaderRow(
                    title = attachment.displayName,
                    subtitle = listOfNotNull(
                        attachment.resolutionLabel.takeIf { it.isNotBlank() },
                        MediaHelper.formatFileSize(attachment.fileSizeBytes).takeIf { attachment.fileSizeBytes > 0 }
                    ).joinToString(" · "),
                    sha256Preview = attachment.sha256Preview,
                    onClear = onClear
                )
                attachment.previewThumbnail?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Selected clinical image preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colorScheme.surfaceContainer),
                        contentScale = ContentScale.Crop
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onPickImage,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("Replace Image")
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoWorkspaceCard(
    attachment: MediaAttachment?,
    onPickVideo: () -> Unit,
    onClear: () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (attachment == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colorScheme.surfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.VideoLibrary,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.video_drop_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.video_drop_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onPickVideo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("choose_video_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_choose_video))
                }
            } else {
                AttachmentHeaderRow(
                    title = attachment.displayName,
                    subtitle = listOfNotNull(
                        MediaHelper.formatDuration(attachment.durationMillis).takeIf { attachment.durationMillis > 0 },
                        attachment.resolutionLabel.takeIf { it.isNotBlank() },
                        MediaHelper.formatFileSize(attachment.fileSizeBytes).takeIf { attachment.fileSizeBytes > 0 }
                    ).joinToString(" · "),
                    sha256Preview = attachment.sha256Preview,
                    onClear = onClear
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (attachment.previewThumbnail != null) {
                        Image(
                            bitmap = attachment.previewThumbnail.asImageBitmap(),
                            contentDescription = "Selected clinical video keyframe",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.VideoLibrary,
                            contentDescription = "Video selected",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    if (attachment.durationMillis > 0L) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.surface.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = MediaHelper.formatDuration(attachment.durationMillis),
                                style = MonoCaption,
                                color = colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                if (attachment.fileSizeBytes > MediaHelper.MAX_INLINE_BYTES) {
                    Text(
                        text = "Clip exceeds 16 MB inline threshold; VeriMed will automatically sample chronological keyframes for temporal inspection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = onPickVideo,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Replace Video")
                }
            }
        }
    }
}

@Composable
private fun AudioWorkspaceCard(
    attachment: MediaAttachment?,
    recorderState: AudioRecorderUiState,
    micPermissionDenied: Boolean,
    onPickAudio: () -> Unit,
    onToggleRecord: () -> Unit,
    onTogglePlay: () -> Unit,
    onClear: () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (recorderState.isRecording) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colorScheme.errorContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(colorScheme.error)
                                )
                                Text(
                                    text = "RECORDING VOICE SAMPLE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurface
                                )
                            }
                            Text(
                                text = MediaHelper.formatDuration(recorderState.elapsedMillis),
                                style = MonoCaption,
                                color = colorScheme.onSurface
                            )
                        }
                        LiveAudioWaveform(
                            amplitudes = recorderState.amplitudes,
                            isActive = true
                        )
                        Button(
                            onClick = onToggleRecord,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("stop_recording_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.error,
                                contentColor = colorScheme.onError
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.action_stop_recording))
                        }
                    }
                }
            } else if (attachment == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colorScheme.surfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.GraphicEq,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.audio_drop_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.audio_drop_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                if (micPermissionDenied) {
                    Text(
                        text = stringResource(R.string.audio_permission_rationale),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.error
                    )
                }
                recorderState.recorderError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickAudio,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("choose_audio_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "File",
                            maxLines = 1
                        )
                    }
                    OutlinedButton(
                        onClick = onToggleRecord,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("record_audio_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_record_audio),
                            maxLines = 1
                        )
                    }
                }
            } else {
                AttachmentHeaderRow(
                    title = attachment.displayName,
                    subtitle = listOfNotNull(
                        MediaHelper.formatDuration(attachment.durationMillis).takeIf { attachment.durationMillis > 0 },
                        MediaHelper.formatFileSize(attachment.fileSizeBytes).takeIf { attachment.fileSizeBytes > 0 }
                    ).joinToString(" · "),
                    sha256Preview = attachment.sha256Preview,
                    onClear = onClear
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            onClick = onTogglePlay,
                            shape = CircleShape,
                            color = colorScheme.primary,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("play_audio_preview_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (recorderState.isPlayingPreview) {
                                        Icons.Outlined.Pause
                                    } else {
                                        Icons.Outlined.PlayArrow
                                    },
                                    contentDescription = if (recorderState.isPlayingPreview) {
                                        "Pause audio preview"
                                    } else {
                                        "Play audio preview"
                                    },
                                    tint = colorScheme.onPrimary
                                )
                            }
                        }
                        LiveAudioWaveform(
                            amplitudes = listOf(
                                0.25f, 0.45f, 0.7f, 0.4f, 0.85f, 0.55f, 0.35f,
                                0.65f, 0.9f, 0.5f, 0.4f, 0.72f, 0.6f, 0.3f,
                                0.52f, 0.78f, 0.42f, 0.28f
                            ),
                            isActive = recorderState.isPlayingPreview,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onPickAudio,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("Replace File")
                    }
                    TextButton(
                        onClick = onToggleRecord,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("Record New")
                    }
                }
            }
        }
    }
}

@Composable
private fun TextWorkspaceCard(
    text: String,
    onTextChange: (String) -> Unit,
    onLoadSample: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val clinical = LocalClinicalColors.current
    val clipboard = LocalClipboardManager.current
    val wordCount = remember(text) {
        text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.text_input_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "$wordCount words",
                    style = MonoCaption,
                    color = colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 260.dp)
                    .testTag("clinical_text_input"),
                placeholder = {
                    Text(
                        text = stringResource(R.string.text_input_placeholder),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                shape = RoundedCornerShape(14.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        clipboard.getText()?.text?.let { pasted ->
                            if (pasted.isNotBlank()) onTextChange(pasted)
                        }
                    },
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("paste_text_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_paste), style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onLoadSample,
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("load_sample_note_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Science,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_load_sample), style = MaterialTheme.typography.labelMedium)
                }

                Spacer(modifier = Modifier.weight(1f))

                if (text.isNotEmpty()) {
                    IconButton(
                        onClick = { onTextChange("") },
                        modifier = Modifier.testTag("clear_text_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.action_clear),
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentHeaderRow(
    title: String,
    subtitle: String,
    sha256Preview: String,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (sha256Preview.isNotBlank() && sha256Preview != "unavailable") {
                Text(
                    text = "SHA-256 ${sha256Preview.take(16)}…",
                    style = MonoCaption,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        IconButton(
            onClick = onClear,
            modifier = Modifier.testTag("clear_media_attachment_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove attached media",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CaseReferenceCard(
    value: String,
    onValueChange: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val clinical = LocalClinicalColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, clinical.hairlineBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("case_reference_input"),
                singleLine = true,
                label = { Text(stringResource(R.string.case_ref_label)) },
                placeholder = { Text(stringResource(R.string.case_ref_placeholder)) },
                shape = RoundedCornerShape(12.dp)
            )
            Text(
                text = stringResource(R.string.case_ref_phi_note),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnalyzingProgressCard(
    stageLabel: String,
    stepIndex: Int
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stageLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "STEP ${stepIndex.coerceIn(1, 3)}/3",
                    style = MonoCaption,
                    color = colorScheme.primary
                )
            }
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = colorScheme.primary,
                trackColor = colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun ForensicErrorCard(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("forensic_error_card"),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.errorContainer,
        border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = "Verification error",
                    tint = colorScheme.error
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = colorScheme.onErrorContainer)
                }
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedButton(onClick = onRetry) {
                    Text("Retry", color = colorScheme.onErrorContainer)
                }
            }
        }
    }
}
