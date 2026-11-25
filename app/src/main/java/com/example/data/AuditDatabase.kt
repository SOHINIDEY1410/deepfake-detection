package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "audit_reports")
data class AuditReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampMillis: Long,
    val modality: String,
    val specialty: String,
    val caseReference: String,
    val mediaLabel: String,
    val mediaMetadata: String,
    val sha256Hash: String,
    val verdict: String,
    val authenticityScore: Int,
    val confidence: String,
    val executiveSummary: String,
    val indicatorsJson: String,
    val metricsJson: String,
    val clinicalRecommendation: String
)

@Dao
interface AuditReportDao {
    @Query("SELECT * FROM audit_reports ORDER BY timestampMillis DESC")
    fun getAllReports(): Flow<List<AuditReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(entity: AuditReportEntity): Long

    @Query("DELETE FROM audit_reports WHERE id = :id")
    suspend fun deleteReportById(id: Long)

    @Query("DELETE FROM audit_reports")
    suspend fun clearAll()
}

@Database(entities = [AuditReportEntity::class], version = 1, exportSchema = false)
abstract class VeriMedDatabase : RoomDatabase() {
    abstract fun auditReportDao(): AuditReportDao

    companion object {
        @Volatile
        private var instance: VeriMedDatabase? = null

        fun getInstance(context: Context): VeriMedDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VeriMedDatabase::class.java,
                    "verimed_audit.db"
                ).build().also { instance = it }
            }
        }
    }
}

class AuditRepository(private val dao: AuditReportDao) {
    val allReports: Flow<List<VerificationReport>> = dao.getAllReports().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun saveReport(report: VerificationReport): VerificationReport {
        val insertedId = dao.insertReport(report.toEntity())
        return report.copy(id = insertedId)
    }

    suspend fun deleteReport(id: Long) {
        dao.deleteReportById(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}

private fun VerificationReport.toEntity(): AuditReportEntity {
    val indicatorsArray = JSONArray().apply {
        indicators.forEach { ind ->
            put(
                JSONObject().apply {
                    put("category", ind.category)
                    put("finding", ind.finding)
                    put("severity", ind.severity.name)
                }
            )
        }
    }
    val metricsArray = JSONArray().apply {
        metrics.forEach { metric ->
            put(
                JSONObject().apply {
                    put("name", metric.name)
                    put("score", metric.score)
                    put("detail", metric.detail)
                }
            )
        }
    }
    return AuditReportEntity(
        id = id,
        timestampMillis = timestampMillis,
        modality = modality.name,
        specialty = specialty.name,
        caseReference = caseReference,
        mediaLabel = mediaLabel,
        mediaMetadata = mediaMetadata,
        sha256Hash = sha256Hash,
        verdict = verdict.name,
        authenticityScore = authenticityScore,
        confidence = confidence.name,
        executiveSummary = executiveSummary,
        indicatorsJson = indicatorsArray.toString(),
        metricsJson = metricsArray.toString(),
        clinicalRecommendation = clinicalRecommendation
    )
}

private fun AuditReportEntity.toDomain(): VerificationReport {
    val parsedIndicators = runCatching {
        val arr = JSONArray(indicatorsJson)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            ForensicIndicator(
                category = obj.optString("category", "Forensic Signal"),
                finding = obj.optString("finding", ""),
                severity = IndicatorSeverity.fromWire(obj.optString("severity", "LOW"))
            )
        }
    }.getOrDefault(emptyList())

    val parsedMetrics = runCatching {
        val arr = JSONArray(metricsJson)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            ForensicMetric(
                name = obj.optString("name", "Integrity Metric"),
                score = obj.optInt("score", 50).coerceIn(0, 100),
                detail = obj.optString("detail", "")
            )
        }
    }.getOrDefault(emptyList())

    return VerificationReport(
        id = id,
        timestampMillis = timestampMillis,
        modality = runCatching { MediaModality.valueOf(modality) }.getOrDefault(MediaModality.IMAGE),
        specialty = runCatching { ClinicalSpecialty.valueOf(specialty) }.getOrDefault(ClinicalSpecialty.GENERAL),
        caseReference = caseReference,
        mediaLabel = mediaLabel,
        mediaMetadata = mediaMetadata,
        sha256Hash = sha256Hash,
        verdict = AuthenticityVerdict.fromWire(verdict),
        authenticityScore = authenticityScore.coerceIn(0, 100),
        confidence = ConfidenceLevel.fromWire(confidence),
        executiveSummary = executiveSummary,
        indicators = parsedIndicators,
        metrics = parsedMetrics,
        clinicalRecommendation = clinicalRecommendation
    )
}
