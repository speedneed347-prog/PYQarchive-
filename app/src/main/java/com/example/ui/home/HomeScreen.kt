package com.example.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ExamSchedule
import com.example.data.model.StudentProfile
import com.example.data.model.University
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.OfflineNoticeBanner
import com.example.ui.components.PaperCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToUniversities: () -> Unit,
    onNavigateToUniversityDetail: (String) -> Unit,
    onNavigateToPaperDetail: (String) -> Unit,
    onNavigateToExams: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val studentProfile by viewModel.studentProfile.collectAsState()
    val universities by viewModel.universities.collectAsState()
    val semesters by viewModel.semesters.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val popularPapers by viewModel.popularPapers.collectAsState()
    val recentPapers by viewModel.recentPapers.collectAsState()
    val recommendedPapers by viewModel.recommendedPapers.collectAsState()
    val studentExamSchedule by viewModel.studentExamSchedule.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("home_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_icon),
                            contentDescription = "PYQ Logo",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.home_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (studentProfile.isConfigured) {
                                    "${studentProfile.universityName ?: ""} • ${studentProfile.semesterName ?: ""}"
                                } else {
                                    "Academic Archive"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::openOnboardingDialog,
                        modifier = Modifier.testTag("home_edit_profile_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_academic_details),
                            tint = MaterialTheme.colorScheme.primary
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
            OfflineNoticeBanner(
                isOffline = uiState.isOffline,
                onRetry = viewModel::refreshData
            )

            if (uiState.isLoading && universities.isEmpty() && popularPapers.isEmpty()) {
                LoadingStateView(message = stringResource(R.string.loading))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Search Bar Tap Banner
                    item {
                        SearchBarBanner(onClick = onNavigateToSearch)
                    }

                    // Academic Profile Status Banner / Setup prompt
                    item {
                        StudentSpecificationHeader(
                            profile = studentProfile,
                            onEdit = viewModel::openOnboardingDialog
                        )
                    }

                    // Student's Personalized Upcoming Exam Countdown
                    if (studentExamSchedule != null) {
                        item {
                            PersonalizedExamBanner(
                                schedule = studentExamSchedule!!,
                                isPersonalized = studentProfile.isConfigured,
                                onClick = onNavigateToExams
                            )
                        }
                    }

                    // Recommended Papers (Specifically for this student's university/semester/subject)
                    if (recommendedPapers.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.recommended_for_you),
                                onViewAll = onNavigateToSearch,
                                leadingIcon = Icons.Default.AutoAwesome
                            )
                        }
                        items(recommendedPapers.take(6)) { paper ->
                            PaperCard(
                                paper = paper,
                                onClick = { onNavigateToPaperDetail(paper.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(paper) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Universities Carousel
                    item {
                        SectionHeader(
                            title = stringResource(R.string.browse_universities),
                            onViewAll = onNavigateToUniversities
                        )
                        UniversitiesCarousel(
                            universities = universities,
                            onSelectUniversity = onNavigateToUniversityDetail
                        )
                    }

                    // Recent Papers (if any viewed)
                    if (recentPapers.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.recent_papers),
                                onViewAll = null
                            )
                        }
                        items(recentPapers) { paper ->
                            PaperCard(
                                paper = paper,
                                onClick = { onNavigateToPaperDetail(paper.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(paper) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Popular Question Papers
                    item {
                        SectionHeader(
                            title = stringResource(R.string.popular_papers),
                            onViewAll = onNavigateToSearch,
                            leadingIcon = Icons.Default.TrendingUp
                        )
                    }

                    if (popularPapers.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                EmptyStateView(
                                    title = stringResource(R.string.no_popular_papers),
                                    message = "Tap below to fetch papers from the online archive"
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = viewModel::refreshData,
                                    modifier = Modifier.testTag("home_retry_sync_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Fetch Archive Papers")
                                }
                            }
                        }
                    } else {
                        items(popularPapers) { paper ->
                            PaperCard(
                                paper = paper,
                                onClick = { onNavigateToPaperDetail(paper.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(paper) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // First-Time Setup / Edit Academic Dialog
        if (uiState.showOnboardingDialog) {
            StudentOnboardingDialog(
                universities = universities,
                semesters = semesters,
                subjects = subjects,
                initialUniId = studentProfile.universityId,
                initialSemId = studentProfile.semesterId,
                initialSubId = studentProfile.subjectId,
                onSave = { uId, uName, sId, sName, subId, subName ->
                    viewModel.saveStudentProfile(uId, uName, sId, sName, subId, subName)
                },
                onDismiss = viewModel::dismissOnboardingDialog
            )
        }
    }
}

@Composable
fun StudentSpecificationHeader(
    profile: StudentProfile,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onEdit)
            .testTag("student_profile_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (profile.isConfigured) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (profile.isConfigured) Icons.Default.School else Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = if (profile.isConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (profile.isConfigured) {
                    Text(
                        text = stringResource(R.string.student_profile_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${profile.universityName ?: ""} • ${profile.semesterName ?: ""}${if (profile.subjectName != null) " • " + profile.subjectName else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = stringResource(R.string.student_profile_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = stringResource(R.string.student_profile_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                tint = if (profile.isConfigured) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun PersonalizedExamBanner(
    schedule: ExamSchedule,
    isPersonalized: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
            .testTag("exam_countdown_banner"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isPersonalized) stringResource(R.string.my_exam_countdown) else stringResource(R.string.upcoming_exams),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Alerts on",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "${schedule.universityName} • ${schedule.semesterName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.days_remaining, schedule.daysRemaining),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SearchBarBanner(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
            .testTag("home_search_bar"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.search_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    onViewAll: (() -> Unit)? = null,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (onViewAll != null) {
            TextButton(onClick = onViewAll) {
                Text(
                    text = stringResource(R.string.view_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun UniversitiesCarousel(
    universities: List<University>,
    onSelectUniversity: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(universities.take(10)) { uni ->
            Card(
                modifier = Modifier
                    .width(150.dp)
                    .clickable { onSelectUniversity(uni.id) }
                    .testTag("uni_chip_${uni.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uni.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
