package com.example.data

import android.graphics.Bitmap
import android.net.Uri
import java.io.File

enum class ThemeMode(val label: String, val subtitle: String) {
    SYSTEM("System", "Match device display"),
    LIGHT("Clinical Day", "High-clarity light surfaces"),
    DARK("Reading Room", "Low-glare dark diagnostic mode")
}

enum class MediaModality(val label: String, val shortLabel: String) {
    IMAGE("Diagnostic Image", "Image"),
    VIDEO("Clinical Video", "Video"),
    AUDIO("Clinical Audio", "Audio"),
    TEXT("Clinical Text", "Text")
}

enum class ClinicalSpecialty(val label: String, val focusSummary: String) {
    GENERAL(
        "General Clinical",
        "Cross-specialty media provenance, noise patterns, and synthetic generation artifacts"
    ),
    RADIOLOGY(
        "Radiology",
        "Anatomical plausibility, modality noise floors, DICOM-like overlay splicing, and lesion synthesis"
    ),
    DERMATOLOGY(
        "Dermatology",
        "Skin micro-texture continuity, dermoscopic lighting, specular reflections, and boundary blending"
    ),
    TELEHEALTH(
        "Telehealth",
        "Facial micro-expression coherence, lip-sync alignment, vocoder harmonics, and background warping"
    ),
    DOCUMENTATION(
        "Documentation",
        "Synthetic LLM linguistic uniformity, non-standard clinical shorthand, and fabricated dosing/metrics"
    )
}

enum class AuthenticityVerdict(val displayTitle: String, val shortBadge: String) {
    AUTHENTIC("Likely Authentic", "AUTHENTIC"),
    SUSPICIOUS("Review Recommended", "SUSPICIOUS"),
    LIKELY_SYNTHETIC("Likely Synthetic / Deepfake", "SYNTHETIC"),
    INCONCLUSIVE("Inconclusive", "INCONCLUSIVE");

    companion object {
        fun fromWire(value: String): AuthenticityVerdict = when (value.trim().uppercase()) {
            "AUTHENTIC" -> AUTHENTIC
            "SUSPICIOUS" -> SUSPICIOUS
            "LIKELY_SYNTHETIC", "SYNTHETIC", "DEEPFAKE" -> LIKELY_SYNTHETIC
            else -> INCONCLUSIVE
        }
    }
}

enum class ConfidenceLevel(val label: String) {
    HIGH("High Confidence"),
    MODERATE("Moderate Confidence"),
    LOW("Low Confidence");

    companion object {
        fun fromWire(value: String): ConfidenceLevel = when (value.trim().uppercase()) {
            "HIGH" -> HIGH
            "MODERATE", "MEDIUM" -> MODERATE
            else -> LOW
        }
    }
}

enum class IndicatorSeverity(val label: String) {
    BENIGN("Benign"),
    LOW("Low"),
    MEDIUM("Moderate"),
    HIGH("High");

    companion object {
        fun fromWire(value: String): IndicatorSeverity = when (value.trim().uppercase()) {
            "BENIGN", "NONE", "CLEAR" -> BENIGN
            "LOW" -> LOW
            "MEDIUM", "MODERATE" -> MEDIUM
            "HIGH", "CRITICAL" -> HIGH
            else -> LOW
        }
    }
}

data class ForensicIndicator(
    val category: String,
    val finding: String,
    val severity: IndicatorSeverity
)

data class ForensicMetric(
    val name: String,
    val score: Int, // 0..100 integrity score (100 = strongest authenticity signal)
    val detail: String
)

data class VerificationReport(
    val id: Long = 0L,
    val timestampMillis: Long = System.currentTimeMillis(),
    val modality: MediaModality,
    val specialty: ClinicalSpecialty,
    val caseReference: String,
    val mediaLabel: String,
    val mediaMetadata: String,
    val sha256Hash: String,
    val verdict: AuthenticityVerdict,
    val authenticityScore: Int, // 0..100
    val confidence: ConfidenceLevel,
    val executiveSummary: String,
    val indicators: List<ForensicIndicator>,
    val metrics: List<ForensicMetric>,
    val clinicalRecommendation: String
) {
    val syntheticRiskScore: Int
        get() = (100 - authenticityScore).coerceIn(0, 100)
}

data class MediaAttachment(
    val modality: MediaModality,
    val uri: Uri? = null,
    val capturedBitmap: Bitmap? = null,
    val recordedAudioFile: File? = null,
    val displayName: String = "",
    val mimeType: String = "",
    val fileSizeBytes: Long = 0L,
    val durationMillis: Long = 0L,
    val resolutionLabel: String = "",
    val previewThumbnail: Bitmap? = null,
    val sha256Preview: String = ""
)

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data class Analyzing(val stageLabel: String, val stepIndex: Int) : AnalysisUiState
    data class Success(val report: VerificationReport) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}
