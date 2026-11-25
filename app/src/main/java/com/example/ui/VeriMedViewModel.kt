package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ForensicAiRepository
import com.example.data.AnalysisUiState
import com.example.data.AuditRepository
import com.example.data.AuthenticityVerdict
import com.example.data.ClinicalSpecialty
import com.example.data.MediaAttachment
import com.example.data.MediaHelper
import com.example.data.MediaModality
import com.example.data.PreferencesRepository
import com.example.data.ThemeMode
import com.example.data.VeriMedDatabase
import com.example.data.VerificationReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "VeriMedViewModel"

data class AudioRecorderUiState(
    val isRecording: Boolean = false,
    val elapsedMillis: Long = 0L,
    val amplitudes: List<Float> = List(28) { 0.12f },
    val isPlayingPreview: Boolean = false,
    val recorderError: String? = null
)

class VeriMedViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val preferencesRepository = PreferencesRepository(appContext)
    private val auditRepository = AuditRepository(
        VeriMedDatabase.getInstance(appContext).auditReportDao()
    )
    private val forensicAiRepository = ForensicAiRepository()

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val defaultSpecialty: StateFlow<ClinicalSpecialty> = preferencesRepository.defaultSpecialtyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClinicalSpecialty.GENERAL)

    val allAuditReports: StateFlow<List<VerificationReport>> = auditRepository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedModality = MutableStateFlow(MediaModality.IMAGE)
    val selectedModality: StateFlow<MediaModality> = _selectedModality.asStateFlow()

    private val _selectedSpecialty = MutableStateFlow(ClinicalSpecialty.GENERAL)
    val selectedSpecialty: StateFlow<ClinicalSpecialty> = _selectedSpecialty.asStateFlow()

    private val _caseReference = MutableStateFlow("")
    val caseReference: StateFlow<String> = _caseReference.asStateFlow()

    private val _clinicalText = MutableStateFlow("")
    val clinicalText: StateFlow<String> = _clinicalText.asStateFlow()

    private val _imageAttachment = MutableStateFlow<MediaAttachment?>(null)
    val imageAttachment: StateFlow<MediaAttachment?> = _imageAttachment.asStateFlow()

    private val _videoAttachment = MutableStateFlow<MediaAttachment?>(null)
    val videoAttachment: StateFlow<MediaAttachment?> = _videoAttachment.asStateFlow()

    private val _audioAttachment = MutableStateFlow<MediaAttachment?>(null)
    val audioAttachment: StateFlow<MediaAttachment?> = _audioAttachment.asStateFlow()

    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    private val _activeReport = MutableStateFlow<VerificationReport?>(null)
    val activeReport: StateFlow<VerificationReport?> = _activeReport.asStateFlow()

    private val _audioRecorderState = MutableStateFlow(AudioRecorderUiState())
    val audioRecorderState: StateFlow<AudioRecorderUiState> = _audioRecorderState.asStateFlow()

    // Audit Log filter state
    private val _auditSearchQuery = MutableStateFlow("")
    val auditSearchQuery: StateFlow<String> = _auditSearchQuery.asStateFlow()

    private val _auditModalityFilter = MutableStateFlow<MediaModality?>(null)
    val auditModalityFilter: StateFlow<MediaModality?> = _auditModalityFilter.asStateFlow()

    private val _auditVerdictFilter = MutableStateFlow<AuthenticityVerdict?>(null)
    val auditVerdictFilter: StateFlow<AuthenticityVerdict?> = _auditVerdictFilter.asStateFlow()

    val filteredAuditReports: StateFlow<List<VerificationReport>> = combine(
        allAuditReports,
        _auditSearchQuery,
        _auditModalityFilter,
        _auditVerdictFilter
    ) { reports, query, modality, verdict ->
        val q = query.trim().lowercase()
        reports.filter { item ->
            val matchesModality = modality == null || item.modality == modality
            val matchesVerdict = verdict == null || item.verdict == verdict
            val matchesQuery = q.isEmpty() ||
                item.caseReference.lowercase().contains(q) ||
                item.mediaLabel.lowercase().contains(q) ||
                item.executiveSummary.lowercase().contains(q) ||
                item.specialty.label.lowercase().contains(q)
            matchesModality && matchesVerdict && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var mediaRecorder: MediaRecorder? = null
    private var activeRecordingFile: File? = null
    private var recordingJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null

    init {
        viewModelScope.launch {
            val savedSpecialty = preferencesRepository.defaultSpecialtyFlow.first()
            _selectedSpecialty.value = savedSpecialty
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun cycleQuickDarkMode(currentlyDark: Boolean) {
        setThemeMode(if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK)
    }

    fun setDefaultSpecialty(specialty: ClinicalSpecialty) {
        _selectedSpecialty.value = specialty
        viewModelScope.launch {
            preferencesRepository.setDefaultSpecialty(specialty)
        }
    }

    fun selectModality(modality: MediaModality) {
        if (_selectedModality.value != modality) {
            stopAudioPlayback()
            _selectedModality.value = modality
            if (_analysisState.value is AnalysisUiState.Error) {
                _analysisState.value = AnalysisUiState.Idle
            }
        }
    }

    fun selectSpecialty(specialty: ClinicalSpecialty) {
        _selectedSpecialty.value = specialty
    }

    fun updateCaseReference(value: String) {
        _caseReference.value = value.take(40)
    }

    fun updateClinicalText(value: String) {
        _clinicalText.value = value
        if (_analysisState.value is AnalysisUiState.Error) {
            _analysisState.value = AnalysisUiState.Idle
        }
    }

    fun loadSampleClinicalNote() {
        _clinicalText.value = """
            RADIOLOGY CONSULTATION NOTE — CHEST PA/LATERAL
            Indication: Persistent non-productive cough x 3 weeks, mild exertional dyspnea.
            Technique: Standard upright PA and lateral chest radiographs reviewed.
            Findings: Lungs are well-expanded without focal consolidation, pneumothorax, or pleural effusion. Cardiomediastinal silhouette is within normal limits. Mild degenerative spurring of the mid-thoracic spine noted. No acute osseous abnormality.
            Impression: No acute cardiopulmonary process. Recommend clinical correlation and follow-up spirometry if symptoms persist beyond 2 weeks.
        """.trimIndent()
        if (_selectedSpecialty.value == ClinicalSpecialty.GENERAL) {
            _selectedSpecialty.value = ClinicalSpecialty.DOCUMENTATION
        }
    }

    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            val attachment = withContext(Dispatchers.IO) {
                val meta = MediaHelper.queryUriMetadata(appContext, uri, "image/jpeg")
                val preview = MediaHelper.decodeScaledBitmap(appContext, uri, maxDimension = 960)
                val hash = MediaHelper.computeSha256FromUri(appContext, uri)
                MediaAttachment(
                    modality = MediaModality.IMAGE,
                    uri = uri,
                    displayName = meta.displayName,
                    mimeType = meta.mimeType,
                    fileSizeBytes = meta.fileSizeBytes,
                    resolutionLabel = preview?.let { "${it.width}×${it.height}" } ?: "",
                    previewThumbnail = preview,
                    sha256Preview = hash
                )
            }
            _imageAttachment.value = attachment
            _analysisState.value = AnalysisUiState.Idle
        }
    }

    fun onImageCaptured(bitmap: Bitmap) {
        viewModelScope.launch {
            val attachment = withContext(Dispatchers.IO) {
                val scaled = MediaHelper.scaleBitmapIfNeeded(bitmap, maxDimension = 1440)
                val bytes = MediaHelper.bitmapToJpegBytes(scaled)
                val hash = MediaHelper.computeSha256(bytes)
                val stamp = SimpleDateFormat("HHmmss", Locale.US).format(Date())
                MediaAttachment(
                    modality = MediaModality.IMAGE,
                    capturedBitmap = scaled,
                    displayName = "clinical_capture_$stamp.jpg",
                    mimeType = "image/jpeg",
                    fileSizeBytes = bytes.size.toLong(),
                    resolutionLabel = "${scaled.width}×${scaled.height}",
                    previewThumbnail = scaled,
                    sha256Preview = hash
                )
            }
            _imageAttachment.value = attachment
            _analysisState.value = AnalysisUiState.Idle
        }
    }

    fun clearImageAttachment() {
        _imageAttachment.value = null
    }

    fun onVideoSelected(uri: Uri) {
        viewModelScope.launch {
            val attachment = withContext(Dispatchers.IO) {
                val meta = MediaHelper.queryUriMetadata(appContext, uri, "video/mp4")
                val info = MediaHelper.extractVideoInfo(appContext, uri)
                val hash = MediaHelper.computeSha256FromUri(appContext, uri)
                val res = if (info.width > 0 && info.height > 0) "${info.width}×${info.height}" else ""
                MediaAttachment(
                    modality = MediaModality.VIDEO,
                    uri = uri,
                    displayName = meta.displayName,
                    mimeType = meta.mimeType,
                    fileSizeBytes = meta.fileSizeBytes,
                    durationMillis = info.durationMillis,
                    resolutionLabel = res,
                    previewThumbnail = info.thumbnail,
                    sha256Preview = hash
                )
            }
            _videoAttachment.value = attachment
            _analysisState.value = AnalysisUiState.Idle
        }
    }

    fun clearVideoAttachment() {
        _videoAttachment.value = null
    }

    fun onAudioSelected(uri: Uri) {
        stopAudioPlayback()
        viewModelScope.launch {
            val attachment = withContext(Dispatchers.IO) {
                val meta = MediaHelper.queryUriMetadata(appContext, uri, "audio/mpeg")
                val duration = MediaHelper.extractAudioDuration(appContext, uri)
                val hash = MediaHelper.computeSha256FromUri(appContext, uri)
                MediaAttachment(
                    modality = MediaModality.AUDIO,
                    uri = uri,
                    displayName = meta.displayName,
                    mimeType = meta.mimeType,
                    fileSizeBytes = meta.fileSizeBytes,
                    durationMillis = duration,
                    sha256Preview = hash
                )
            }
            _audioAttachment.value = attachment
            _analysisState.value = AnalysisUiState.Idle
        }
    }

    fun clearAudioAttachment() {
        stopAudioPlayback()
        _audioAttachment.value = null
    }

    fun startAudioRecording() {
        stopAudioPlayback()
        if (_audioRecorderState.value.isRecording) return

        try {
            val recordingsDir = File(appContext.cacheDir, "clinical_audio").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outFile = File(recordingsDir, "voice_sample_$stamp.m4a")
            val recorder = MediaHelper.createAudioRecorder(appContext, outFile)
            recorder.start()
            mediaRecorder = recorder
            activeRecordingFile = outFile

            val startTime = System.currentTimeMillis()
            _audioRecorderState.value = AudioRecorderUiState(
                isRecording = true,
                elapsedMillis = 0L,
                amplitudes = List(28) { 0.14f }
            )

            recordingJob?.cancel()
            recordingJob = viewModelScope.launch {
                while (isActive && _audioRecorderState.value.isRecording) {
                    delay(120L)
                    val elapsed = System.currentTimeMillis() - startTime
                    val ampRaw = runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                    val normalized = (ampRaw / 24000f).coerceIn(0.12f, 1.0f)
                    val updatedBars = _audioRecorderState.value.amplitudes.drop(1) + normalized
                    _audioRecorderState.value = _audioRecorderState.value.copy(
                        elapsedMillis = elapsed,
                        amplitudes = updatedBars
                    )
                    if (elapsed >= 60_000L) {
                        stopAudioRecording()
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            releaseRecorder()
            _audioRecorderState.value = AudioRecorderUiState(
                isRecording = false,
                recorderError = "Microphone unavailable: ${e.localizedMessage ?: "unable to record"}"
            )
        }
    }

    fun stopAudioRecording() {
        if (!_audioRecorderState.value.isRecording) return
        val elapsed = _audioRecorderState.value.elapsedMillis
        val recordedFile = activeRecordingFile

        recordingJob?.cancel()
        recordingJob = null

        val stopSucceeded = runCatching {
            mediaRecorder?.stop()
        }.isSuccess
        releaseRecorder()

        _audioRecorderState.value = _audioRecorderState.value.copy(isRecording = false)

        if (stopSucceeded && recordedFile != null && recordedFile.exists() && recordedFile.length() > 0L) {
            viewModelScope.launch {
                val attachment = withContext(Dispatchers.IO) {
                    val hash = MediaHelper.computeSha256FromFile(recordedFile)
                    MediaAttachment(
                        modality = MediaModality.AUDIO,
                        recordedAudioFile = recordedFile,
                        displayName = recordedFile.name,
                        mimeType = "audio/mp4",
                        fileSizeBytes = recordedFile.length(),
                        durationMillis = elapsed.coerceAtLeast(1000L),
                        sha256Preview = hash
                    )
                }
                _audioAttachment.value = attachment
                _analysisState.value = AnalysisUiState.Idle
            }
        } else {
            _audioRecorderState.value = _audioRecorderState.value.copy(
                recorderError = "Recording was too short. Hold for at least 1 second."
            )
        }
    }

    fun toggleAudioPlayback() {
        val attachment = _audioAttachment.value ?: return
        if (_audioRecorderState.value.isPlayingPreview) {
            stopAudioPlayback()
            return
        }
        try {
            val player = MediaPlayer().apply {
                when {
                    attachment.recordedAudioFile != null -> setDataSource(attachment.recordedAudioFile.absolutePath)
                    attachment.uri != null -> setDataSource(appContext, attachment.uri)
                    else -> return
                }
                setOnCompletionListener {
                    stopAudioPlayback()
                }
                prepare()
                start()
            }
            mediaPlayer = player
            _audioRecorderState.value = _audioRecorderState.value.copy(isPlayingPreview = true)
        } catch (e: Exception) {
            Log.e(TAG, "Audio preview playback failed", e)
            stopAudioPlayback()
        }
    }

    fun stopAudioPlayback() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        _audioRecorderState.value = _audioRecorderState.value.copy(isPlayingPreview = false)
    }

    fun canRunAnalysis(): Boolean {
        if (_analysisState.value is AnalysisUiState.Analyzing) return false
        return when (_selectedModality.value) {
            MediaModality.IMAGE -> _imageAttachment.value != null
            MediaModality.VIDEO -> _videoAttachment.value != null
            MediaModality.AUDIO -> _audioAttachment.value != null && !_audioRecorderState.value.isRecording
            MediaModality.TEXT -> _clinicalText.value.trim().length >= 20
        }
    }

    fun runForensicAnalysis() {
        if (!canRunAnalysis()) return
        stopAudioPlayback()

        val modality = _selectedModality.value
        val specialty = _selectedSpecialty.value
        val caseRef = _caseReference.value

        viewModelScope.launch {
            _analysisState.value = AnalysisUiState.Analyzing("Preparing clinical media payload…", 0)
            try {
                val report = when (modality) {
                    MediaModality.IMAGE -> {
                        val att = _imageAttachment.value
                            ?: throw IllegalStateException("Select an image first.")
                        forensicAiRepository.analyzeImage(
                            context = appContext,
                            attachment = att,
                            specialty = specialty,
                            caseReference = caseRef
                        ) { stage, step ->
                            _analysisState.value = AnalysisUiState.Analyzing(stage, step)
                        }
                    }
                    MediaModality.VIDEO -> {
                        val att = _videoAttachment.value
                            ?: throw IllegalStateException("Select a video first.")
                        forensicAiRepository.analyzeVideo(
                            context = appContext,
                            attachment = att,
                            specialty = specialty,
                            caseReference = caseRef
                        ) { stage, step ->
                            _analysisState.value = AnalysisUiState.Analyzing(stage, step)
                        }
                    }
                    MediaModality.AUDIO -> {
                        val att = _audioAttachment.value
                            ?: throw IllegalStateException("Select or record an audio sample first.")
                        forensicAiRepository.analyzeAudio(
                            context = appContext,
                            attachment = att,
                            specialty = specialty,
                            caseReference = caseRef
                        ) { stage, step ->
                            _analysisState.value = AnalysisUiState.Analyzing(stage, step)
                        }
                    }
                    MediaModality.TEXT -> {
                        forensicAiRepository.analyzeText(
                            clinicalText = _clinicalText.value,
                            specialty = specialty,
                            caseReference = caseRef
                        ) { stage, step ->
                            _analysisState.value = AnalysisUiState.Analyzing(stage, step)
                        }
                    }
                }

                val savedReport = auditRepository.saveReport(report)
                _analysisState.value = AnalysisUiState.Success(savedReport)
                _activeReport.value = savedReport
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Forensic verification error", e)
                _analysisState.value = AnalysisUiState.Error(
                    e.localizedMessage ?: "Verification failed. Please check network connectivity and retry."
                )
            }
        }
    }

    fun dismissError() {
        _analysisState.value = AnalysisUiState.Idle
    }

    fun openReport(report: VerificationReport) {
        _activeReport.value = report
    }

    fun closeActiveReport() {
        _activeReport.value = null
        if (_analysisState.value is AnalysisUiState.Success) {
            _analysisState.value = AnalysisUiState.Idle
        }
    }

    fun startNewVerification() {
        _activeReport.value = null
        _analysisState.value = AnalysisUiState.Idle
    }

    fun updateAuditSearch(query: String) {
        _auditSearchQuery.value = query
    }

    fun toggleAuditModalityFilter(modality: MediaModality?) {
        _auditModalityFilter.value = if (_auditModalityFilter.value == modality) null else modality
    }

    fun toggleAuditVerdictFilter(verdict: AuthenticityVerdict?) {
        _auditVerdictFilter.value = if (_auditVerdictFilter.value == verdict) null else verdict
    }

    fun deleteAuditReport(id: Long) {
        viewModelScope.launch {
            auditRepository.deleteReport(id)
            if (_activeReport.value?.id == id) {
                _activeReport.value = null
            }
        }
    }

    fun clearAllAuditReports() {
        viewModelScope.launch {
            auditRepository.clearAll()
            _activeReport.value = null
        }
    }

    fun formatShareableSummary(report: VerificationReport): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm z", Locale.US)
            .format(Date(report.timestampMillis))
        val indicatorsText = report.indicators.joinToString("\n") {
            "  - [${it.severity.label}] ${it.category}: ${it.finding}"
        }
        return buildString {
            appendLine("VERIMED CLINICAL FORENSIC SCREENING REPORT")
            appendLine("==========================================")
            appendLine("Timestamp: $dateStr")
            if (report.caseReference.isNotBlank()) {
                appendLine("Case Ref: ${report.caseReference}")
            }
            appendLine("Modality: ${report.modality.label} (${report.specialty.label})")
            appendLine("Media: ${report.mediaLabel} (${report.mediaMetadata})")
            appendLine("SHA-256: ${report.sha256Hash}")
            appendLine()
            appendLine("Verdict: ${report.verdict.displayTitle} (${report.confidence.label})")
            appendLine("Authenticity Score: ${report.authenticityScore}/100 | Synthetic Risk: ${report.syntheticRiskScore}%")
            appendLine()
            appendLine("Clinical Assessment:")
            appendLine(report.executiveSummary)
            if (indicatorsText.isNotBlank()) {
                appendLine()
                appendLine("Detected Signals:")
                appendLine(indicatorsText)
            }
            appendLine()
            appendLine("Recommended Action:")
            appendLine(report.clinicalRecommendation)
            appendLine()
            appendLine("Disclaimer: AI-assisted probabilistic forensic screening; confirm via institutional provenance protocols before clinical reliance.")
        }
    }

    private fun releaseRecorder() {
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null
    }

    override fun onCleared() {
        recordingJob?.cancel()
        releaseRecorder()
        stopAudioPlayback()
        super.onCleared()
    }
}
