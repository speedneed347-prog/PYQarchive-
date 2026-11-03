package com.example.ui.downloads

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Paper
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PaperCard
import com.example.util.DocumentScannerHelper
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    onNavigateToPdfViewer: (String) -> Unit,
    onNavigateToPaperDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val scannerHelper = remember { DocumentScannerHelper(context) }
    var isScanning by remember { mutableStateOf(false) }

    val downloadedPapers by viewModel.downloadedPapers.collectAsState()
    val favoritePapers by viewModel.favoritePapers.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.downloads_title),
        stringResource(R.string.favorite)
    )

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        isScanning = false
        if (result.resultCode == Activity.RESULT_OK) {
            coroutineScope.launch {
                val scanResult = scannerHelper.handleScanResult(result.data)
                scanResult.onSuccess { savedPaper ->
                    Toast.makeText(context, "Question paper scanned and saved!", Toast.LENGTH_SHORT).show()
                    onNavigateToPdfViewer(savedPaper.id)
                }.onFailure { err ->
                    Toast.makeText(context, "Scanning failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val startScanner = {
        if (activity != null) {
            isScanning = true
            scannerHelper.startScanning(
                activity = activity,
                onIntentSenderReady = { intentSender ->
                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                },
                onError = { err ->
                    isScanning = false
                    Toast.makeText(context, "Scanner unavailable: ${err.message}", Toast.LENGTH_LONG).show()
                }
            )
        } else {
            Toast.makeText(context, "Cannot launch scanner from current context.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("downloads_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.downloads_title), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(
                        onClick = startScanner,
                        modifier = Modifier.testTag("btn_top_scan")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Scan Document",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = startScanner,
                icon = {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null)
                    }
                },
                text = { Text("Scan Paper") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_scan_paper")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = "$title (${if (index == 0) downloadedPapers.size else favoritePapers.size})",
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (selectedTabIndex == 0) {
                // Downloads Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Google Document Scanner Banner Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("scan_banner_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Google Document Scanner",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Scan physical papers with auto-edge detection, crop & save to PDF",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = startScanner,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Scan")
                                }
                            }
                        }
                    }

                    if (downloadedPapers.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Default.Download,
                                title = stringResource(R.string.no_downloads),
                                message = stringResource(R.string.no_downloads_desc)
                            )
                        }
                    } else {
                        items(downloadedPapers) { paper ->
                            DownloadedPaperItem(
                                paper = paper,
                                viewModel = viewModel,
                                onViewPdf = { onNavigateToPdfViewer(paper.id) },
                                onViewDetail = { onNavigateToPaperDetail(paper.id) },
                                onDelete = { viewModel.deleteDownload(paper.id) }
                            )
                        }
                    }
                }
            } else {
                // Favorites Tab
                if (favoritePapers.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Bookmark,
                        title = "No favorites saved",
                        message = "Tap the bookmark icon on any paper to save it here for fast revision."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(favoritePapers) { paper ->
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
}

@Composable
fun DownloadedPaperItem(
    paper: Paper,
    viewModel: DownloadsViewModel,
    onViewPdf: () -> Unit,
    onViewDetail: () -> Unit,
    onDelete: () -> Unit
) {
    val file = paper.localPdfPath?.let { File(it) }
    val sizeStr = file?.let { viewModel.pdfManager.formatFileSize(it.length()) } ?: ""

    Card(
        modifier = Modifier.fillMaxWidth().testTag("download_item_${paper.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = paper.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${paper.universityName} • ${paper.subjectName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (sizeStr.isNotBlank()) {
                Text(
                    text = stringResource(R.string.file_size, sizeStr),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    OutlinedButton(
                        onClick = onViewPdf,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.open_pdf))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = onViewDetail,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Info")
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
