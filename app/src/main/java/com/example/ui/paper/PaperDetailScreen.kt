package com.example.ui.paper

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.MetaChip

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaperDetailScreen(
    paperId: String,
    viewModel: PaperDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPdfViewer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(paperId) {
        viewModel.loadPaper(paperId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val paper = uiState.paper

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("paper_detail_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.paper_details), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (paper != null) {
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                imageVector = if (paper.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Favorite",
                                tint = if (paper.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, paper.title)
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "${paper.title}\n${paper.universityName} • ${paper.subjectName}\nView on PYQ Archive"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Question Paper"))
                        }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingStateView(
                    message = stringResource(R.string.loading),
                    modifier = Modifier.padding(innerPadding)
                )
            }
            paper == null -> {
                EmptyStateView(
                    icon = Icons.Default.Description,
                    title = uiState.errorMessage ?: stringResource(R.string.no_papers_found),
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Title Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = paper.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (paper.universityName.isNotBlank()) MetaChip(text = paper.universityName, isAccent = true)
                                if (paper.courseName.isNotBlank()) MetaChip(text = paper.courseName)
                                if (paper.semesterName.isNotBlank()) MetaChip(text = paper.semesterName)
                                if (paper.subjectName.isNotBlank()) MetaChip(text = paper.subjectName)
                                if (paper.year.isNotBlank()) MetaChip(text = paper.year, isAccent = true)
                                if (paper.paperTypeName.isNotBlank()) MetaChip(text = paper.paperTypeName)
                                if (paper.languageName.isNotBlank()) MetaChip(text = paper.languageName)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToPdfViewer(paper.id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_view_pdf"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.open_pdf), fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.downloadPdfPermanently(
                                    onSuccess = {
                                        Toast.makeText(context, R.string.pdf_downloaded, Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            enabled = !uiState.isDownloading,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_download_pdf"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (uiState.isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (uiState.downloadedFile != null) Icons.Default.Check else Icons.Default.Download,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.downloadedFile != null) "Saved" else stringResource(R.string.download_pdf),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (uiState.isDownloading) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { uiState.downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${stringResource(R.string.downloading_pdf)} (${uiState.downloadProgress}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Open with External App (if downloaded)
                    if (uiState.downloadedFile != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = viewModel.pdfManager.openWithExternalApp(uiState.downloadedFile!!)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No PDF viewer app found.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.open_external))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Verified Paper Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = stringResource(R.string.paper_info),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            InfoRow(label = stringResource(R.string.university_label), value = paper.universityName)
                            InfoRow(label = stringResource(R.string.course_label), value = paper.courseName)
                            InfoRow(label = stringResource(R.string.semester_label), value = paper.semesterName)
                            InfoRow(label = stringResource(R.string.subject_label), value = paper.subjectName)
                            InfoRow(label = stringResource(R.string.year_label), value = paper.year)
                            InfoRow(label = stringResource(R.string.paper_type_label), value = paper.paperTypeName)
                            InfoRow(label = stringResource(R.string.language_label), value = paper.languageName)
                            InfoRow(label = stringResource(R.string.views_label), value = "${paper.viewCount} views")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Personal Notes Card (Room Persisted)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.personal_notes),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (uiState.isNotesSaved) {
                                    Text(
                                        text = "Saved",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = uiState.personalNotes,
                                onValueChange = viewModel::updateNotes,
                                placeholder = { Text(stringResource(R.string.notes_placeholder)) },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    viewModel.saveNotes()
                                    Toast.makeText(context, R.string.notes_saved, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.align(Alignment.End),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.save_notes))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
}
