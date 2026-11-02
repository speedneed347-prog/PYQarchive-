package com.example.ui.home

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Course
import com.example.data.model.ExamSchedule
import com.example.data.model.Paper
import com.example.data.model.Semester
import com.example.data.model.StudentProfile
import com.example.data.model.Subject
import com.example.data.model.University
import com.example.data.repository.ArchiveRepository
import com.example.data.repository.StudentProfileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null,
    val showOnboardingDialog: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ArchiveRepository(application)
    private val studentProfileManager = StudentProfileManager.getInstance(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val studentProfile: StateFlow<StudentProfile> = studentProfileManager.profileFlow

    val universities: StateFlow<List<University>> = repository.allUniversitiesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val semesters: StateFlow<List<Semester>> = repository.allSemestersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjects: StateFlow<List<Subject>> = repository.allSubjectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val popularPapers: StateFlow<List<Paper>> = repository.popularPapers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentPapers: StateFlow<List<Paper>> = repository.recentPapers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val examSchedules: StateFlow<List<ExamSchedule>> = repository.examSchedulesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Personalized exam schedule matching student's University + Semester
    val studentExamSchedule: StateFlow<ExamSchedule?> = combine(
        examSchedules,
        studentProfile
    ) { schedules, profile ->
        if (!profile.universityId.isNullOrBlank() && !profile.semesterId.isNullOrBlank()) {
            schedules.firstOrNull { it.universityId == profile.universityId && it.semesterId == profile.semesterId }
                ?: schedules.firstOrNull { it.universityId == profile.universityId }
                ?: schedules.firstOrNull()
        } else {
            schedules.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Recommended papers based on student's University, Semester, and Subject
    val recommendedPapers: StateFlow<List<Paper>> = combine(
        popularPapers,
        studentProfile
    ) { papers, profile ->
        if (profile.isConfigured) {
            val matched = papers.filter { p ->
                val matchUni = profile.universityId == null || p.universityId == profile.universityId
                val matchSem = profile.semesterId == null || p.semesterId == profile.semesterId
                val matchSub = profile.subjectId == null || p.subjectId == profile.subjectId
                matchUni && matchSem && matchSub
            }
            if (matched.isNotEmpty()) matched else {
                // Fallback to matching university & semester
                papers.filter { p ->
                    (profile.universityId == null || p.universityId == profile.universityId) &&
                    (profile.semesterId == null || p.semesterId == profile.semesterId)
                }
            }
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Check if first-time onboarding is needed
        if (!studentProfile.value.isOnboardingCompleted) {
            _uiState.value = _uiState.value.copy(showOnboardingDialog = true)
        }
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isRefreshing = true, errorMessage = null)
            val refResult = repository.syncReferenceData()
            val papersResult = repository.syncPublishedPapers(limit = 500)

            val hasOnlineSync = refResult.isSuccess || papersResult.isSuccess
            val networkAvailable = isNetworkConnected()

            // If sync succeeded or device is online, we are NOT offline!
            val isOffline = !hasOnlineSync && !networkAvailable

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                isOffline = isOffline,
                errorMessage = if (isOffline) "Offline mode: Showing cached papers" else null
            )

            // Auto-schedule exam reminders if student profile and exam schedule exist
            studentExamSchedule.value?.let { schedule ->
                studentProfileManager.scheduleAutomatedExamAlerts(schedule)
            }
        }
    }

    private fun isNetworkConnected(): Boolean {
        return try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Throwable) {
            false
        }
    }

    fun openOnboardingDialog() {
        _uiState.value = _uiState.value.copy(showOnboardingDialog = true)
    }

    fun dismissOnboardingDialog() {
        _uiState.value = _uiState.value.copy(showOnboardingDialog = false)
    }

    fun saveStudentProfile(
        universityId: String?,
        universityName: String?,
        semesterId: String?,
        semesterName: String?,
        subjectId: String?,
        subjectName: String?
    ) {
        studentProfileManager.saveProfile(
            universityId = universityId,
            universityName = universityName,
            semesterId = semesterId,
            semesterName = semesterName,
            subjectId = subjectId,
            subjectName = subjectName
        )
        _uiState.value = _uiState.value.copy(showOnboardingDialog = false)

        // Find schedule and schedule notifications
        studentExamSchedule.value?.let { schedule ->
            studentProfileManager.scheduleAutomatedExamAlerts(schedule)
        }
    }

    fun skipOnboarding() {
        studentProfileManager.skipOnboarding()
        _uiState.value = _uiState.value.copy(showOnboardingDialog = false)
    }

    fun toggleFavorite(paper: Paper) {
        viewModelScope.launch {
            repository.toggleFavorite(paper.id, !paper.isFavorite)
        }
    }
}
