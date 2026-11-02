package com.example.data.model

data class StudentProfile(
    val universityId: String? = null,
    val universityName: String? = null,
    val semesterId: String? = null,
    val semesterName: String? = null,
    val subjectId: String? = null,
    val subjectName: String? = null,
    val isOnboardingCompleted: Boolean = false
) {
    val isConfigured: Boolean
        get() = !universityId.isNullOrBlank() && !semesterId.isNullOrBlank()
}
