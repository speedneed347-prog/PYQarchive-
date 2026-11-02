package com.example.ui.university

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Course
import com.example.data.model.Paper
import com.example.data.model.Semester
import com.example.data.model.Subject
import com.example.data.model.University
import com.example.data.model.YearItem
import com.example.data.repository.ArchiveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UniversityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ArchiveRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val rawUniversities = repository.allUniversitiesFlow

    val filteredUniversities: StateFlow<List<University>> = combine(
        rawUniversities,
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.shortName.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // For University Detail
    private val _selectedUniId = MutableStateFlow<String?>(null)

    val selectedUniversity: StateFlow<University?> = combine(
        rawUniversities,
        _selectedUniId
    ) { list, id ->
        list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedCourseId = MutableStateFlow<String?>(null)
    val selectedCourseId = _selectedCourseId.asStateFlow()

    private val _selectedSemesterId = MutableStateFlow<String?>(null)
    val selectedSemesterId = _selectedSemesterId.asStateFlow()

    private val _selectedSubjectId = MutableStateFlow<String?>(null)
    val selectedSubjectId = _selectedSubjectId.asStateFlow()

    val courses: StateFlow<List<Course>> = repository.allCoursesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val semesters: StateFlow<List<Semester>> = repository.allSemestersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjects: StateFlow<List<Subject>> = repository.allSubjectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val years: StateFlow<List<YearItem>> = repository.allYearsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uniPapers = MutableStateFlow<List<Paper>>(emptyList())
    val uniPapers: StateFlow<List<Paper>> = combine(
        _uniPapers,
        _selectedCourseId,
        _selectedSemesterId,
        _selectedSubjectId
    ) { papers, course, sem, sub ->
        papers.filter { p ->
            (course == null || p.courseId == course) &&
            (sem == null || p.semesterId == sem) &&
            (sub == null || p.subjectId == sub)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun loadUniversity(uniId: String) {
        _selectedUniId.value = uniId
        viewModelScope.launch {
            repository.getPapersByUniversityFlow(uniId).collect { papers ->
                _uniPapers.value = papers
            }
        }
    }

    fun setCourseFilter(courseId: String?) {
        _selectedCourseId.value = courseId
    }

    fun setSemesterFilter(semesterId: String?) {
        _selectedSemesterId.value = semesterId
    }

    fun setSubjectFilter(subjectId: String?) {
        _selectedSubjectId.value = subjectId
    }

    fun toggleFavorite(paper: Paper) {
        viewModelScope.launch {
            repository.toggleFavorite(paper.id, !paper.isFavorite)
        }
    }
}
