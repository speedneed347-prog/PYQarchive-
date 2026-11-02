package com.example.data.model

import com.google.firebase.Timestamp

data class Paper(
    val id: String = "",
    val title: String = "",
    val pdfUrl: String = "",
    val universityId: String = "",
    val universityName: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val semesterId: String = "",
    val semesterName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val yearId: String = "",
    val year: String = "",
    val paperTypeId: String = "",
    val paperTypeName: String = "",
    val languageId: String = "",
    val languageName: String = "",
    val status: String = "published",
    val description: String = "",
    val viewCount: Long = 0L,
    val lastViewedAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    // Client-side local flags:
    val isFavorite: Boolean = false,
    val personalNotes: String = "",
    val localPdfPath: String? = null
)

data class University(
    val id: String = "",
    val name: String = "",
    val shortName: String = "",
    val description: String = "",
    val logo: String = ""
)

data class Course(
    val id: String = "",
    val name: String = ""
)

data class Semester(
    val id: String = "",
    val name: String = ""
)

data class Subject(
    val id: String = "",
    val name: String = "",
    val code: String = ""
)

data class YearItem(
    val id: String = "",
    val year: String = ""
)

data class PaperType(
    val id: String = "",
    val name: String = ""
)

data class LanguageItem(
    val id: String = "",
    val name: String = ""
)

data class ExamSchedule(
    val id: String = "",
    val universityId: String = "",
    val universityName: String = "",
    val semesterId: String = "",
    val semesterName: String = "",
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val academicYear: String = ""
) {
    val daysRemaining: Long
        get() {
            val startMillis = startDate?.toDate()?.time ?: return -1L
            val nowMillis = System.currentTimeMillis()
            val diff = startMillis - nowMillis
            return (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(0L)
        }
}
