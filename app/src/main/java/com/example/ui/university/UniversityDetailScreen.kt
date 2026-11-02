package com.example.ui.university

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PaperCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversityDetailScreen(
    universityId: String,
    viewModel: UniversityViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPaperDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(universityId) {
        viewModel.loadUniversity(universityId)
    }

    val university by viewModel.selectedUniversity.collectAsState()
    val papers by viewModel.uniPapers.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val semesters by viewModel.semesters.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    val selectedCourseId by viewModel.selectedCourseId.collectAsState()
    val selectedSemesterId by viewModel.selectedSemesterId.collectAsState()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("university_detail_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = university?.name ?: stringResource(R.string.browse_universities),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
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
            // University Hero Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = university?.name ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (university?.shortName?.isNotBlank() == true) {
                                Text(
                                    text = university?.shortName ?: "",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (university?.description?.isNotBlank() == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = university?.description ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Hierarchy Filter Rows
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                // Course Chips
                if (courses.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCourseId == null,
                            onClick = { viewModel.setCourseFilter(null) },
                            label = { Text(stringResource(R.string.all_courses)) }
                        )
                        courses.forEach { course ->
                            FilterChip(
                                selected = selectedCourseId == course.id,
                                onClick = {
                                    viewModel.setCourseFilter(
                                        if (selectedCourseId == course.id) null else course.id
                                    )
                                },
                                label = { Text(course.name) }
                            )
                        }
                    }
                }

                // Semester Chips
                if (semesters.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedSemesterId == null,
                            onClick = { viewModel.setSemesterFilter(null) },
                            label = { Text(stringResource(R.string.all_semesters)) }
                        )
                        semesters.forEach { sem ->
                            FilterChip(
                                selected = selectedSemesterId == sem.id,
                                onClick = {
                                    viewModel.setSemesterFilter(
                                        if (selectedSemesterId == sem.id) null else sem.id
                                    )
                                },
                                label = { Text(sem.name) }
                            )
                        }
                    }
                }

                // Subject Chips
                if (subjects.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedSubjectId == null,
                            onClick = { viewModel.setSubjectFilter(null) },
                            label = { Text(stringResource(R.string.all_subjects)) }
                        )
                        subjects.forEach { sub ->
                            FilterChip(
                                selected = selectedSubjectId == sub.id,
                                onClick = {
                                    viewModel.setSubjectFilter(
                                        if (selectedSubjectId == sub.id) null else sub.id
                                    )
                                },
                                label = { Text(sub.name) }
                            )
                        }
                    }
                }
            }

            // Results List
            if (papers.isEmpty()) {
                EmptyStateView(
                    title = stringResource(R.string.no_papers_found),
                    message = "Try clearing filters to see all available papers."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.results_count, papers.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(papers) { paper ->
                        PaperCard(
                            paper = paper,
                            onClick = { onNavigateToPaperDetail(paper.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(paper) }
                        )
                    }
                }
            }
        }
    }
}
