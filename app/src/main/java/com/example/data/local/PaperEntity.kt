package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Paper
import com.google.firebase.Timestamp
import java.util.Date

@Entity(tableName = "papers")
data class PaperEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val pdfUrl: String,
    val universityId: String,
    val universityName: String,
    val courseId: String,
    val courseName: String,
    val semesterId: String,
    val semesterName: String,
    val subjectId: String,
    val subjectName: String,
    val yearId: String,
    val year: String,
    val paperTypeId: String,
    val paperTypeName: String,
    val languageId: String,
    val languageName: String,
    val status: String,
    val description: String,
    val viewCount: Long,
    val createdAtMillis: Long?,
    // Local device state
    val isFavorite: Boolean = false,
    val personalNotes: String = "",
    val lastViewedTimestamp: Long? = null,
    val downloadedFilePath: String? = null,
    val downloadedSizeBytes: Long = 0L,
    val downloadedAt: Long? = null
) {
    fun toDomain(): Paper {
        return Paper(
            id = id,
            title = title,
            pdfUrl = pdfUrl,
            universityId = universityId,
            universityName = universityName,
            courseId = courseId,
            courseName = courseName,
            semesterId = semesterId,
            semesterName = semesterName,
            subjectId = subjectId,
            subjectName = subjectName,
            yearId = yearId,
            year = year,
            paperTypeId = paperTypeId,
            paperTypeName = paperTypeName,
            languageId = languageId,
            languageName = languageName,
            status = status,
            description = description,
            viewCount = viewCount,
            createdAt = createdAtMillis?.let { Timestamp(Date(it)) },
            isFavorite = isFavorite,
            personalNotes = personalNotes,
            localPdfPath = downloadedFilePath
        )
    }

    companion object {
        fun fromDomain(paper: Paper, existingEntity: PaperEntity? = null): PaperEntity {
            return PaperEntity(
                id = paper.id,
                title = paper.title,
                pdfUrl = paper.pdfUrl,
                universityId = paper.universityId,
                universityName = paper.universityName,
                courseId = paper.courseId,
                courseName = paper.courseName,
                semesterId = paper.semesterId,
                semesterName = paper.semesterName,
                subjectId = paper.subjectId,
                subjectName = paper.subjectName,
                yearId = paper.yearId,
                year = paper.year,
                paperTypeId = paper.paperTypeId,
                paperTypeName = paper.paperTypeName,
                languageId = paper.languageId,
                languageName = paper.languageName,
                status = paper.status,
                description = paper.description,
                viewCount = paper.viewCount,
                createdAtMillis = paper.createdAt?.toDate()?.time,
                isFavorite = existingEntity?.isFavorite ?: paper.isFavorite,
                personalNotes = existingEntity?.personalNotes ?: paper.personalNotes,
                lastViewedTimestamp = existingEntity?.lastViewedTimestamp,
                downloadedFilePath = existingEntity?.downloadedFilePath ?: paper.localPdfPath,
                downloadedSizeBytes = existingEntity?.downloadedSizeBytes ?: 0L,
                downloadedAt = existingEntity?.downloadedAt
            )
        }
    }
}
