package com.example.ai

import android.content.Context
import android.util.Log
import com.example.data.AuthenticityVerdict
import com.example.data.ClinicalSpecialty
import com.example.data.ConfidenceLevel
import com.example.data.ForensicIndicator
import com.example.data.ForensicMetric
import com.example.data.IndicatorSeverity
import com.example.data.MediaAttachment
import com.example.data.MediaHelper
import com.example.data.MediaModality
import com.example.data.VerificationReport
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "ForensicAiRepository"

class ForensicAiRepository {

    private val forensicSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "verdict" to Schema.enumeration(
                    values = listOf("AUTHENTIC", "SUSPICIOUS", "LIKELY_SYNTHETIC", "INCONCLUSIVE"),
                    description = "Overall forensic authenticity classification"
                ),
                "authenticityScore" to Schema.integer(
                    description = "Estimated probability of authenticity from 0 (definitely synthetic/manipulated) to 100 (high confidence authentic)"
                ),
                "confidence" to Schema.enumeration(
                    values = listOf("HIGH", "MODERATE", "LOW"),
                    description = "Diagnostic certainty given media resolution, length, and signal clarity"
                ),
                "executiveSummary" to Schema.string(
                    description = "Concise 2-3 sentence clinical forensic summary tailored to the clinician's specialty"
                ),
                "indicators" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "category" to Schema.string(description = "Forensic domain category, e.g. Noise Floor, Anatomical Plausibility, Spectral Prosody, Temporal Continuity, Linguistic Perplexity"),
                            "finding" to Schema.string(description = "Specific observable artifact or authenticity cue found in the submitted media"),
                            "severity" to Schema.enumeration(
                                values = listOf("BENIGN", "LOW", "MEDIUM", "HIGH"),
                                description = "Risk severity of this signal"
                            )
                        )
                    ),
                    description = "3 to 5 specific forensic indicators observed"
                ),
                "metrics" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "name" to Schema.string(description = "Short technical integrity metric label"),
                            "score" to Schema.integer(description = "Integrity score 0 to 100 where 100 indicates natural/authentic signal"),
                            "detail" to Schema.string(description = "Brief one-line technical note")
                        )
                    ),
                    description = "3 to 4 modality-specific integrity scores"
                ),
                "clinicalRecommendation" to Schema.string(
                    description = "Concrete, practical clinical verification step recommended for the clinician"
                )
            )
        )
    }

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.2f
                responseMimeType = "application/json"
                responseSchema = forensicSchema
            }
        )
    }

    suspend fun analyzeImage(
        context: Context,
        attachment: MediaAttachment,
        specialty: ClinicalSpecialty,
        caseReference: String,
        onStageUpdate: (String, Int) -> Unit
    ): VerificationReport = withContext(Dispatchers.IO) {
        onStageUpdate("Computing SHA-256 provenance digest…", 1)
        val bitmap = when {
            attachment.capturedBitmap != null -> MediaHelper.scaleBitmapIfNeeded(attachment.capturedBitmap)
            attachment.uri != null -> MediaHelper.decodeScaledBitmap(context, attachment.uri)
            else -> null
        } ?: throw IllegalArgumentException("Unable to read selected image. Please select another image.")

        val hash = when {
            attachment.sha256Preview.isNotBlank() -> attachment.sha256Preview
            attachment.uri != null -> MediaHelper.computeSha256FromUri(context, attachment.uri)
            else -> MediaHelper.computeSha256(MediaHelper.bitmapToJpegBytes(bitmap))
        }

        val metadata = buildString {
            append("${bitmap.width}×${bitmap.height} px")
            if (attachment.fileSizeBytes > 0L) {
                append(" · ${MediaHelper.formatFileSize(attachment.fileSizeBytes)}")
            }
            if (attachment.mimeType.isNotBlank()) {
                append(" · ${attachment.mimeType}")
            }
        }

        onStageUpdate("Running visual artifact & anatomical inspection…", 2)
        val prompt = buildSystemRubric(
            modality = MediaModality.IMAGE,
            specialty = specialty,
            extraContext = """
                Examine this image carefully for signs of AI generation (diffusion/GAN synthesis), deepfake face/body manipulation, medical imaging splicing (e.g., injected lesions, tampered radiographic markers, inconsistent sensor grain/noise floor, unnatural specular reflections, warped anatomy, inconsistent DICOM/UI overlay typography, or edge blending seams).
                Provide 4 metrics covering: Sensor Noise Integrity, Structural & Anatomical Coherence, Boundary & Lighting Consistency, and Provenance Fidelity.
            """.trimIndent()
        )

        val responseJson = executeModelCall(onStageUpdate) {
            model.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            ).text
        }

        parseReport(
            jsonString = responseJson,
            modality = MediaModality.IMAGE,
            specialty = specialty,
            caseReference = caseReference,
            mediaLabel = attachment.displayName.ifBlank { "Captured Clinical Image" },
            mediaMetadata = metadata,
            sha256Hash = hash
        )
    }

    suspend fun analyzeVideo(
        context: Context,
        attachment: MediaAttachment,
        specialty: ClinicalSpecialty,
        caseReference: String,
        onStageUpdate: (String, Int) -> Unit
    ): VerificationReport = withContext(Dispatchers.IO) {
        val uri = attachment.uri
            ?: throw IllegalArgumentException("No video selected for verification.")

        onStageUpdate("Computing SHA-256 digest & inspecting video stream…", 1)
        val hash = attachment.sha256Preview.ifBlank { MediaHelper.computeSha256FromUri(context, uri) }
        val mime = attachment.mimeType.ifBlank { "video/mp4" }

        val inlineBytes = if (attachment.fileSizeBytes in 1L..MediaHelper.MAX_INLINE_BYTES) {
            MediaHelper.readBytesFromUri(context, uri, MediaHelper.MAX_INLINE_BYTES)
        } else {
            null
        }

        val usingKeyFrames = inlineBytes == null
        val sampledFrames = if (usingKeyFrames) {
            onStageUpdate("Video exceeds 16 MB inline limit; sampling diagnostic keyframes…", 2)
            MediaHelper.extractVideoKeyFrames(context, uri, frameCount = 6)
        } else {
            onStageUpdate("Inspecting temporal continuity, motion & lip-sync…", 2)
            emptyList()
        }

        if (usingKeyFrames && sampledFrames.isEmpty()) {
            throw IllegalArgumentException("Could not read video bytes or extract keyframes from the selected file.")
        }

        val metadata = buildString {
            if (attachment.durationMillis > 0L) append(MediaHelper.formatDuration(attachment.durationMillis))
            if (attachment.resolutionLabel.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append(attachment.resolutionLabel)
            }
            if (attachment.fileSizeBytes > 0L) {
                if (isNotEmpty()) append(" · ")
                append(MediaHelper.formatFileSize(attachment.fileSizeBytes))
            }
            if (usingKeyFrames) append(" · Keyframe-sampled (${sampledFrames.size} frames)")
        }

        val prompt = buildSystemRubric(
            modality = MediaModality.VIDEO,
            specialty = specialty,
            extraContext = """
                Analyze this clinical video${if (usingKeyFrames) " (represented by ${sampledFrames.size} evenly sampled chronological keyframes)" else ""} for deepfake face swapping, synthetic lip-sync drift, unnatural blinking/micro-expressions, temporal flickering, frame-interpolation artifacts, background warping around head/instrument edges, or manipulated ultrasound/endoscopy cineloop regions.
                Provide 4 metrics covering: Temporal Continuity, Facial & Motion Coherence, Spatial Edge Integrity, and Compression Consistency.
            """.trimIndent()
        )

        val responseJson = executeModelCall(onStageUpdate) {
            model.generateContent(
                content {
                    if (inlineBytes != null) {
                        inlineData(inlineBytes, mime)
                    } else {
                        sampledFrames.forEach { frame -> image(frame) }
                    }
                    text(prompt)
                }
            ).text
        }

        parseReport(
            jsonString = responseJson,
            modality = MediaModality.VIDEO,
            specialty = specialty,
            caseReference = caseReference,
            mediaLabel = attachment.displayName.ifBlank { "Clinical Video Recording" },
            mediaMetadata = metadata.ifBlank { mime },
            sha256Hash = hash
        )
    }

    suspend fun analyzeAudio(
        context: Context,
        attachment: MediaAttachment,
        specialty: ClinicalSpecialty,
        caseReference: String,
        onStageUpdate: (String, Int) -> Unit
    ): VerificationReport = withContext(Dispatchers.IO) {
        onStageUpdate("Computing SHA-256 digest & extracting acoustic payload…", 1)
        val audioBytes: ByteArray
        val hash: String
        val mime: String

        when {
            attachment.recordedAudioFile != null && attachment.recordedAudioFile.exists() -> {
                val file = attachment.recordedAudioFile
                if (file.length() > MediaHelper.MAX_INLINE_BYTES) {
                    throw IllegalArgumentException("Recorded audio exceeds the 16 MB inline analysis limit.")
                }
                audioBytes = file.readBytes()
                hash = attachment.sha256Preview.ifBlank { MediaHelper.computeSha256(audioBytes) }
                mime = "audio/mp4"
            }
            attachment.uri != null -> {
                audioBytes = MediaHelper.readBytesFromUri(context, attachment.uri, MediaHelper.MAX_INLINE_BYTES)
                    ?: throw IllegalArgumentException("Audio file exceeds the 16 MB inline limit or could not be read.")
                hash = attachment.sha256Preview.ifBlank { MediaHelper.computeSha256(audioBytes) }
                mime = attachment.mimeType.ifBlank { "audio/mpeg" }
            }
            else -> throw IllegalArgumentException("No audio clip selected or recorded.")
        }

        if (audioBytes.isEmpty()) {
            throw IllegalArgumentException("The selected audio file is empty.")
        }

        val metadata = buildString {
            if (attachment.durationMillis > 0L) append(MediaHelper.formatDuration(attachment.durationMillis))
            if (isNotEmpty()) append(" · ")
            append(MediaHelper.formatFileSize(audioBytes.size.toLong()))
            append(" · $mime")
        }

        onStageUpdate("Screening spectral prosody & neural vocoder signatures…", 2)
        val prompt = buildSystemRubric(
            modality = MediaModality.AUDIO,
            specialty = specialty,
            extraContext = """
                Listen carefully to this clinical audio recording and screen for AI voice cloning, neural TTS synthesis, splice edits, robotic phase/vocoder metallic artifacts, unnaturally uniform room-tone silence, missing natural micro-breaths or glottal transitions, cadence monotony, or synthetic dictation phrasing.
                Provide 4 metrics covering: Acoustic Naturalness, Vocoder Artifact Absence, Prosodic & Breath Variation, and Background Continuity.
            """.trimIndent()
        )

        val responseJson = executeModelCall(onStageUpdate) {
            model.generateContent(
                content {
                    inlineData(audioBytes, mime)
                    text(prompt)
                }
            ).text
        }

        parseReport(
            jsonString = responseJson,
            modality = MediaModality.AUDIO,
            specialty = specialty,
            caseReference = caseReference,
            mediaLabel = attachment.displayName.ifBlank { "Clinical Audio Sample" },
            mediaMetadata = metadata,
            sha256Hash = hash
        )
    }

    suspend fun analyzeText(
        clinicalText: String,
        specialty: ClinicalSpecialty,
        caseReference: String,
        onStageUpdate: (String, Int) -> Unit
    ): VerificationReport = withContext(Dispatchers.IO) {
        val cleaned = clinicalText.trim()
        if (cleaned.length < 20) {
            throw IllegalArgumentException("Please enter at least 20 characters of clinical text for reliable forensic analysis.")
        }

        onStageUpdate("Computing SHA-256 text fingerprint…", 1)
        val hash = MediaHelper.computeSha256(cleaned)
        val wordCount = cleaned.split(Regex("\\s+")).count { it.isNotBlank() }
        val metadata = "$wordCount words · ${cleaned.length} chars · UTF-8"

        onStageUpdate("Analyzing linguistic perplexity & clinical coherence…", 2)
        val prompt = buildSystemRubric(
            modality = MediaModality.TEXT,
            specialty = specialty,
            extraContext = """
                Evaluate the clinical document or transcript below for signs of AI/LLM generation, synthetic chart fabrication, templated prose uniformity, over-polished transitions atypical of authentic clinician charting, hallucinated medical measurements/dosing, or contradictory clinical timeline details.
                Provide 4 metrics covering: Linguistic Authenticity, Clinical Detail Specificity, Stylistic Burstiness, and Medical Consistency.

                --- SUBMITTED CLINICAL TEXT ---
                $cleaned
                --- END SUBMITTED CLINICAL TEXT ---
            """.trimIndent()
        )

        val responseJson = executeModelCall(onStageUpdate) {
            model.generateContent(prompt).text
        }

        val excerptLabel = cleaned.lineSequence().firstOrNull { it.isNotBlank() }
            ?.take(48)
            ?.let { if (cleaned.length > 48) "$it…" else it }
            ?: "Clinical Document Excerpt"

        parseReport(
            jsonString = responseJson,
            modality = MediaModality.TEXT,
            specialty = specialty,
            caseReference = caseReference,
            mediaLabel = excerptLabel,
            mediaMetadata = metadata,
            sha256Hash = hash
        )
    }

    private suspend fun executeModelCall(
        onStageUpdate: (String, Int) -> Unit,
        block: suspend () -> String?
    ): String {
        return try {
            val raw = withTimeout(60_000L) {
                block()
            }
            onStageUpdate("Compiling structured clinical report…", 3)
            if (raw.isNullOrBlank()) {
                throw IllegalStateException("Forensic model returned an empty response.")
            }
            raw
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Forensic analysis timed out", e)
            throw IllegalStateException("Forensic analysis timed out after 60 seconds. Please check connectivity and try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Forensic analysis failed", e)
            throw e
        }
    }

    private fun buildSystemRubric(
        modality: MediaModality,
        specialty: ClinicalSpecialty,
        extraContext: String
    ): String {
        return """
            You are VeriMed, a clinical-grade digital media forensics screening assistant helping licensed clinicians assess whether diagnostic media is authentic or synthetically manipulated / AI-generated (deepfake).
            Modality: ${modality.label}
            Clinical Context: ${specialty.label} (${specialty.focusSummary})

            Instructions:
            - Be rigorous, objective, calibrated, and conservative—avoid false alarms on routine compression or low-resolution clinical captures, yet clearly flag strong synthetic or tampering cues.
            - If evidence is ambiguous or resolution/length is insufficient, classify as INCONCLUSIVE or SUSPICIOUS with MODERATE or LOW confidence and explain why.
            - Keep tone crisp, concise, and clinical.

            $extraContext
        """.trimIndent()
    }

    private fun parseReport(
        jsonString: String,
        modality: MediaModality,
        specialty: ClinicalSpecialty,
        caseReference: String,
        mediaLabel: String,
        mediaMetadata: String,
        sha256Hash: String
    ): VerificationReport {
        val json = JSONObject(jsonString)
        val verdict = AuthenticityVerdict.fromWire(json.optString("verdict", "INCONCLUSIVE"))
        val score = json.optInt("authenticityScore", 50).coerceIn(0, 100)
        val confidence = ConfidenceLevel.fromWire(json.optString("confidence", "MODERATE"))
        val summary = json.optString("executiveSummary", "Forensic screening completed.")
        val recommendation = json.optString(
            "clinicalRecommendation",
            "Verify original source provenance before clinical reliance."
        )

        val indicators = mutableListOf<ForensicIndicator>()
        val indArray = json.optJSONArray("indicators")
        if (indArray != null) {
            for (i in 0 until indArray.length()) {
                val obj = indArray.optJSONObject(i) ?: continue
                indicators.add(
                    ForensicIndicator(
                        category = obj.optString("category", "Forensic Signal"),
                        finding = obj.optString("finding", ""),
                        severity = IndicatorSeverity.fromWire(obj.optString("severity", "LOW"))
                    )
                )
            }
        }

        val metrics = mutableListOf<ForensicMetric>()
        val metricsArray = json.optJSONArray("metrics")
        if (metricsArray != null) {
            for (i in 0 until metricsArray.length()) {
                val obj = metricsArray.optJSONObject(i) ?: continue
                metrics.add(
                    ForensicMetric(
                        name = obj.optString("name", "Integrity Signal"),
                        score = obj.optInt("score", score).coerceIn(0, 100),
                        detail = obj.optString("detail", "")
                    )
                )
            }
        }

        return VerificationReport(
            timestampMillis = System.currentTimeMillis(),
            modality = modality,
            specialty = specialty,
            caseReference = caseReference.trim(),
            mediaLabel = mediaLabel,
            mediaMetadata = mediaMetadata,
            sha256Hash = sha256Hash,
            verdict = verdict,
            authenticityScore = score,
            confidence = confidence,
            executiveSummary = summary,
            indicators = indicators,
            metrics = metrics,
            clinicalRecommendation = recommendation
        )
    }
}
