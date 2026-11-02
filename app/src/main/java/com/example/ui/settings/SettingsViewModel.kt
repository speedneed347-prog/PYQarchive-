package com.example.ui.settings

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Semester
import com.example.data.model.StudentProfile
import com.example.data.model.Subject
import com.example.data.model.University
import com.example.data.repository.ArchiveRepository
import com.example.data.repository.PdfManager
import com.example.data.repository.StudentProfileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

data class SettingsUiState(
    val currentLanguage: String = "en", // "en" or "bn"
    val currentTheme: AppThemeMode = AppThemeMode.SYSTEM,
    val storageUsageString: String = "0 B",
    val isSyncing: Boolean = false,
    val syncStatusMessage: String? = null,
    val showProfileDialog: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("pyq_settings", Context.MODE_PRIVATE)
    private val pdfManager = PdfManager(application)
    private val repository = ArchiveRepository(application)
    private val studentProfileManager = StudentProfileManager.getInstance(application)

    val studentProfile: StateFlow<StudentProfile> = studentProfileManager.profileFlow

    val universities: StateFlow<List<University>> = repository.allUniversitiesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val semesters: StateFlow<List<Semester>> = repository.allSemestersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjects: StateFlow<List<Subject>> = repository.allSubjectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            currentLanguage = prefs.getString("app_lang", "en") ?: "en",
            currentTheme = AppThemeMode.values().firstOrNull { it.name == prefs.getString("app_theme", null) } ?: AppThemeMode.SYSTEM,
            storageUsageString = pdfManager.formatFileSize(pdfManager.getUsedStorageBytes())
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun openProfileDialog() {
        _uiState.value = _uiState.value.copy(showProfileDialog = true)
    }

    fun dismissProfileDialog() {
        _uiState.value = _uiState.value.copy(showProfileDialog = false)
    }

    fun saveStudentProfile(
        uniId: String?,
        uniName: String?,
        semId: String?,
        semName: String?,
        subId: String?,
        subName: String?
    ) {
        studentProfileManager.saveProfile(
            universityId = uniId,
            universityName = uniName,
            semesterId = semId,
            semesterName = semName,
            subjectId = subId,
            subjectName = subName
        )
        _uiState.value = _uiState.value.copy(showProfileDialog = false)
    }

    fun setLanguage(langCode: String) {
        prefs.edit().putString("app_lang", langCode).apply()
        _uiState.value = _uiState.value.copy(currentLanguage = langCode)

        val app = getApplication<Application>()
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = app.resources.configuration
        config.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val list = LocaleList(locale)
            LocaleList.setDefault(list)
            config.setLocales(list)
        }
        @Suppress("DEPRECATION")
        app.resources.updateConfiguration(config, app.resources.displayMetrics)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = app.getSystemService(LocaleManager::class.java)
            localeManager?.applicationLocales = LocaleList.forLanguageTags(langCode)
        }
    }

    fun setTheme(mode: AppThemeMode) {
        prefs.edit().putString("app_theme", mode.name).apply()
        _uiState.value = _uiState.value.copy(currentTheme = mode)
    }

    fun clearCache() {
        viewModelScope.launch {
            pdfManager.clearAllCache()
            _uiState.value = _uiState.value.copy(
                storageUsageString = pdfManager.formatFileSize(pdfManager.getUsedStorageBytes()),
                syncStatusMessage = "Cache cleared."
            )
        }
    }

    fun refreshStorageInfo() {
        _uiState.value = _uiState.value.copy(
            storageUsageString = pdfManager.formatFileSize(pdfManager.getUsedStorageBytes())
        )
    }

    fun syncAllArchiveData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncStatusMessage = null)
            val refResult = repository.syncReferenceData()
            val paperResult = repository.syncPublishedPapers(limit = 1000)

            val msg = if (paperResult.isSuccess) {
                "Synchronized ${paperResult.getOrNull()} papers and reference data."
            } else {
                "Could not connect to pyqhub-new server. Check internet connection."
            }

            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncStatusMessage = msg,
                storageUsageString = pdfManager.formatFileSize(pdfManager.getUsedStorageBytes())
            )
        }
    }
}
