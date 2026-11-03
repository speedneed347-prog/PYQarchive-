package com.example.ui.pdf

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    paperId: String,
    viewModel: PdfViewerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("pdf_viewer_screen")) {
        val screenWidthPx = with(density) { maxWidth.toPx().toInt() }

        LaunchedEffect(paperId) {
            viewModel.initViewer(paperId, screenWidthPx)
        }

        val uiState by viewModel.uiState.collectAsState()

        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var horizontalDragAccumulator by remember { mutableFloatStateOf(0f) }

        // Reset zoom & pan when page changes
        LaunchedEffect(uiState.currentPage) {
            scale = 1f
            offset = Offset.Zero
            horizontalDragAccumulator = 0f
        }

        val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, 4f)
            if (scale > 1f) {
                offset += offsetChange
            } else {
                offset = Offset.Zero
            }
        }

        val lazyListState = rememberLazyListState()
        LaunchedEffect(uiState.currentPage) {
            if (uiState.totalPages > 0) {
                lazyListState.animateScrollToItem(uiState.currentPage)
            }
        }

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = uiState.paper?.title ?: stringResource(R.string.pdf_viewer),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                if (uiState.totalPages > 0) {
                                    Text(
                                        text = stringResource(R.string.page_counter, uiState.currentPage + 1, uiState.totalPages),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            if (uiState.localFile != null) {
                                IconButton(onClick = {
                                    try {
                                        val intent = viewModel.pdfManager.openWithExternalApp(uiState.localFile!!)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No external PDF viewer found.", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Open in External App")
                                }

                                IconButton(onClick = {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        uiState.localFile!!
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share PDF Document"))
                                }) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share PDF")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Horizontal Page Selector Strip (Requirement 1)
                    if (uiState.totalPages > 1) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            Column {
                                LazyRow(
                                    state = lazyListState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pdf_page_slider_strip"),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(uiState.totalPages) { pageIndex ->
                                        val isCurrent = pageIndex == uiState.currentPage
                                        Surface(
                                            onClick = { viewModel.goToPage(pageIndex) },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isCurrent) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            },
                                            border = BorderStroke(
                                                width = if (isCurrent) 2.dp else 1.dp,
                                                color = if (isCurrent) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                                }
                                            ),
                                            tonalElevation = if (isCurrent) 4.dp else 0.dp,
                                            modifier = Modifier
                                                .defaultMinSize(minWidth = 48.dp, minHeight = 44.dp)
                                                .testTag("page_box_${pageIndex + 1}")
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = "${pageIndex + 1}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                    color = if (isCurrent) {
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (uiState.totalPages > 1) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledIconButton(
                                onClick = viewModel::previousPage,
                                enabled = uiState.currentPage > 0,
                                modifier = Modifier.testTag("btn_prev_page")
                            ) {
                                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous Page")
                            }

                            Text(
                                text = stringResource(R.string.page_counter, uiState.currentPage + 1, uiState.totalPages),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )

                            FilledIconButton(
                                onClick = viewModel::nextPage,
                                enabled = uiState.currentPage < uiState.totalPages - 1,
                                modifier = Modifier.testTag("btn_next_page")
                            ) {
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Page")
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    // Horizontal swipe gesture to slide to next/prev page when not zoomed in
                    .pointerInput(uiState.currentPage, uiState.totalPages, scale) {
                        if (scale <= 1.1f) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (horizontalDragAccumulator < -60f && uiState.currentPage < uiState.totalPages - 1) {
                                        viewModel.nextPage()
                                    } else if (horizontalDragAccumulator > 60f && uiState.currentPage > 0) {
                                        viewModel.previousPage()
                                    }
                                    horizontalDragAccumulator = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    horizontalDragAccumulator += dragAmount
                                }
                            )
                        }
                    }
                    // Double tap to zoom in or zoom out
                    .pointerInput(scale) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                    offset = Offset.Zero
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.downloading_pdf),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (uiState.downloadProgress > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { uiState.downloadProgress / 100f },
                                    modifier = Modifier.width(200.dp)
                                )
                                Text(
                                    text = "${uiState.downloadProgress}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    uiState.errorMessage != null -> {
                        EmptyStateView(
                            title = stringResource(R.string.pdf_error),
                            message = uiState.errorMessage!!
                        )
                    }

                    uiState.currentPageBitmap != null -> {
                        // Smooth animated horizontal page sliding
                        AnimatedContent(
                            targetState = uiState.currentPage to uiState.currentPageBitmap,
                            transitionSpec = {
                                if (targetState.first >= initialState.first) {
                                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                        slideOutHorizontally { width -> -width } + fadeOut()
                                    )
                                } else {
                                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                        slideOutHorizontally { width -> width } + fadeOut()
                                    )
                                }.using(SizeTransform(clip = true))
                            },
                            label = "PdfPageSlideAnimation",
                            modifier = Modifier.fillMaxSize()
                        ) { (_, pageBitmap) ->
                            if (pageBitmap != null) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = pageBitmap.asImageBitmap(),
                                        contentDescription = "PDF Page ${uiState.currentPage + 1}",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(
                                                scaleX = scale,
                                                scaleY = scale,
                                                translationX = offset.x,
                                                translationY = offset.y
                                            )
                                            .transformable(state = transformableState)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
