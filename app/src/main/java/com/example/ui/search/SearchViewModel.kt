package com.example.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Course
import com.example.data.model.LanguageItem
import com.example.data.model.Paper
import com.example.data.model.PaperType
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

data class SearchFilterState(
    val query: String = "",
    val universityId: String? = null,
    val courseId: String? = null,
    val semesterId: String? = null,
    val subjectId: String? = null,
    val yearId: String? = null,
    val paperTypeId: String? = null,
    val languageId: String? = null
) {
    val activeFilterCount: Int
        get() = listOfNotNull(
            universityId,
            courseId,
            semesterId,
            subjectId,
            yearId,
            paperTypeId,
            languageId
        ).size
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ArchiveRepository(application)

    private val _filterState = MutableStateFlow(SearchFilterState())
    val filterState: StateFlow<SearchFilterState> = _filterState.asStateFlow()

    val universities: StateFlow<List<University>> = repository.allUniversitiesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val courses: StateFlow<List<Course>> = repository.allCoursesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val semesters: StateFlow<List<Semester>> = repository.allSemestersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjects: StateFlow<List<Subject>> = repository.allSubjectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val years: StateFlow<List<YearItem>> = repository.allYearsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paperTypes: StateFlow<List<PaperType>> = repository.allPaperTypesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val languages: StateFlow<List<LanguageItem>> = repository.allLanguagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allPapers = repository.popularPapers // flow of all cached papers

    val searchResults: StateFlow<List<Paper>> = combine(
        allPapers,
        _filterState
    ) { papers, filters ->
        papers.filter { paper ->
            val matchesQuery = filters.query.isBlank() ||
                paper.title.contains(filters.query, ignoreCase = true) ||
                paper.subjectName.contains(filters.query, ignoreCase = true) ||
                paper.universityName.contains(filters.query, ignoreCase = true) ||
                paper.courseName.contains(filters.query, ignoreCase = true) ||
                paper.year.contains(filters.query, ignoreCase = true)

            val matchesUni = filters.universityId == null || paper.universityId == filters.universityId
            val matchesCourse = filters.courseId == null || paper.courseId == filters.courseId
            val matchesSem = filters.semesterId == null || paper.semesterId == filters.semesterId
            val matchesSubject = filters.subjectId == null || paper.subjectId == filters.subjectId
            val matchesYear = filters.yearId == null || paper.yearId == filters.yearId || paper.year == filters.yearId
            val matchesType = filters.paperTypeId == null || paper.paperTypeId == filters.paperTypeId
            val matchesLang = filters.languageId == null || paper.languageId == filters.languageId

            matchesQuery && matchesUni && matchesCourse && matchesSem && matchesSubject && matchesYear && matchesType && matchesLang
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(newQuery: String) {
        _filterState.value = _filterState.value.copy(query = newQuery)
    }

    fun setUniversityFilter(uniId: String?) {
        _filterState.value = _filterState.value.copy(universityId = uniId)
    }

    fun setCourseFilter(courseId: String?) {
        _filterState.value = _filterState.value.copy(courseId = courseId)
    }

    fun setSemesterFilter(semId: String?) {
        _filterState.value = _filterState.value.copy(semesterId = semId)
    }

    fun setSubjectFilter(subId: String?) {
        _filterState.value = _filterState.value.copy(subjectId = subId)
    }

    fun setYearFilter(yearId: String?) {
        _filterState.value = _filterState.value.copy(yearId = yearId)
    }

    fun setPaperTypeFilter(typeId: String?) {
        _filterState.value = _filterState.value.copy(paperTypeId = typeId)
    }

    fun setLanguageFilter(langId: String?) {
        _filterState.value = _filterState.value.copy(languageId = langId)
    }

    fun clearAllFilters() {
        _filterState.value = SearchFilterState(query = _filterState.value.query)
    }

    fun toggleFavorite(paper: Paper) {
        viewModelScope.launch {
            repository.toggleFavorite(paper.id, !paper.isFavorite)
        }
    }
}
