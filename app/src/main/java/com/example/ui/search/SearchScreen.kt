package com.example.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PaperCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToPaperDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filterState by viewModel.filterState.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("search_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.search_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input Row with Filter Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = filterState.query,
                    onValueChange = viewModel::updateQuery,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_query_input"),
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (filterState.query.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateQuery("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.width(10.dp))

                BadgedBox(
                    badge = {
                        if (filterState.activeFilterCount > 0) {
                            Badge { Text("${filterState.activeFilterCount}") }
                        }
                    }
                ) {
                    FilledTonalButton(
                        onClick = { showFilterSheet = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("filter_sheet_btn")
                    ) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            }

            // Active Filters Row (if any)
            if (filterState.activeFilterCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = viewModel::clearAllFilters,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(R.string.clear_filters), style = MaterialTheme.typography.labelSmall)
                    }

                    filterState.universityId?.let { id ->
                        val uniName = viewModel.universities.value.firstOrNull { it.id == id }?.name ?: id
                        ActiveFilterChip(label = uniName, onRemove = { viewModel.setUniversityFilter(null) })
                    }
                    filterState.courseId?.let { id ->
                        val courseName = viewModel.courses.value.firstOrNull { it.id == id }?.name ?: id
                        ActiveFilterChip(label = courseName, onRemove = { viewModel.setCourseFilter(null) })
                    }
                    filterState.semesterId?.let { id ->
                        val semName = viewModel.semesters.value.firstOrNull { it.id == id }?.name ?: id
                        ActiveFilterChip(label = semName, onRemove = { viewModel.setSemesterFilter(null) })
                    }
                    filterState.subjectId?.let { id ->
                        val subName = viewModel.subjects.value.firstOrNull { it.id == id }?.name ?: id
                        ActiveFilterChip(label = subName, onRemove = { viewModel.setSubjectFilter(null) })
                    }
                    filterState.yearId?.let { y ->
                        ActiveFilterChip(label = y, onRemove = { viewModel.setYearFilter(null) })
                    }
                    filterState.paperTypeId?.let { id ->
                        val typeName = viewModel.paperTypes.value.firstOrNull { it.id == id }?.name ?: id
                        ActiveFilterChip(label = typeName, onRemove = { viewModel.setPaperTypeFilter(null) })
                    }
                    filterState.languageId?.let { id ->
                        val langName = viewModel.languages.value.firstOrNull { it.id == id }?.name ?: id
                        ActiveFilterChip(label = langName, onRemove = { viewModel.setLanguageFilter(null) })
                    }
                }
            }

            // Results count label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.results_count, searchResults.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Results List
            if (searchResults.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Search,
                    title = stringResource(R.string.no_papers_found),
                    message = "Try adjusting your search keywords or removing filters."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(searchResults) { paper ->
                        PaperCard(
                            paper = paper,
                            onClick = { onNavigateToPaperDetail(paper.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(paper) }
                        )
                    }
                }
            }
        }

        // Filter Modal Bottom Sheet
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                FilterSheetContent(
                    viewModel = viewModel,
                    onApply = { showFilterSheet = false }
                )
            }
        }
    }
}

@Composable
fun ActiveFilterChip(
    label: String,
    onRemove: () -> Unit
) {
    FilterChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label, maxLines = 1) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(14.dp)
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheetContent(
    viewModel: SearchViewModel,
    onApply: () -> Unit
) {
    val filterState by viewModel.filterState.collectAsState()
    val universities by viewModel.universities.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val semesters by viewModel.semesters.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val years by viewModel.years.collectAsState()
    val paperTypes by viewModel.paperTypes.collectAsState()
    val languages by viewModel.languages.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.filter_results),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(onClick = viewModel::clearAllFilters) {
                Text(stringResource(R.string.clear_filters))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. University Dropdown
        DropdownFilterSelector(
            label = stringResource(R.string.university_label),
            options = universities.map { it.id to it.name },
            selectedId = filterState.universityId,
            allLabel = stringResource(R.string.all_universities),
            onSelect = viewModel::setUniversityFilter
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Course Dropdown
        DropdownFilterSelector(
            label = stringResource(R.string.course_label),
            options = courses.map { it.id to it.name },
            selectedId = filterState.courseId,
            allLabel = stringResource(R.string.all_courses),
            onSelect = viewModel::setCourseFilter
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Semester Dropdown
        DropdownFilterSelector(
            label = stringResource(R.string.semester_label),
            options = semesters.map { it.id to it.name },
            selectedId = filterState.semesterId,
            allLabel = stringResource(R.string.all_semesters),
            onSelect = viewModel::setSemesterFilter
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Subject Dropdown
        DropdownFilterSelector(
            label = stringResource(R.string.subject_label),
            options = subjects.map { it.id to it.name },
            selectedId = filterState.subjectId,
            allLabel = stringResource(R.string.all_subjects),
            onSelect = viewModel::setSubjectFilter
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Year Dropdown
        DropdownFilterSelector(
            label = stringResource(R.string.year_label),
            options = years.map { it.id to it.year },
            selectedId = filterState.yearId,
            allLabel = stringResource(R.string.all_years),
            onSelect = viewModel::setYearFilter
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 6. Paper Type Dropdown
        DropdownFilterSelector(
            label = stringResource(R.string.paper_type_label),
            options = paperTypes.map { it.id to it.name },
            selectedId = filterState.paperTypeId,
            allLabel = stringResource(R.string.all_paper_types),
            onSelect = viewModel::setPaperTypeFilter
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 7. Language Dropdown
        DropdownFilterSelector(
            label = stringResource(R.string.language_label),
            options = languages.map { it.id to it.name },
            selectedId = filterState.languageId,
            allLabel = stringResource(R.string.all_languages),
            onSelect = viewModel::setLanguageFilter
        )

        Spacer(modifier = Modifier.height(24.dp))

        FilledTonalButton(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(stringResource(R.string.filter_results), fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownFilterSelector(
    label: String,
    options: List<Pair<String, String>>,
    selectedId: String?,
    allLabel: String,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.first == selectedId }?.second ?: allLabel

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    }
                )
            }
        }
    }
}
