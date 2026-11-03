package com.example.ui.pdf

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Paper
import com.example.data.repository.ArchiveRepository
import com.example.data.repository.PdfManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PdfViewerUiState(
    val paper: Paper? = null,
    val isLoading: Boolean = true,
    val downloadProgress: Int = 0,
    val localFile: File? = null,
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val currentPageBitmap: Bitmap? = null,
    val errorMessage: String? = null
)

class PdfViewerViewModel(application: Application) : AndroidViewModel(application) {

    val pdfManager = PdfManager(application)
    private val repository = ArchiveRepository(application)

    private val _uiState = MutableStateFlow(PdfViewerUiState())
    val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    private var targetScreenWidthPx = 1080

    fun initViewer(paperId: String, screenWidthPx: Int) {
        targetScreenWidthPx = screenWidthPx
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val paper = repository.fetchPaperDetails(paperId)
            if (paper == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Paper details not found.")
                return@launch
            }

            _uiState.value = _uiState.value.copy(paper = paper)

            // Check if already downloaded or cached
            val existingFile = pdfManager.getLocalFile(paperId)
            if (existingFile != null && existingFile.length() > 1024) {
                loadPdfFile(existingFile)
            } else {
                // Download to cache
                val result = pdfManager.downloadPdf(
                    paperId = paperId,
                    rawUrl = paper.pdfUrl,
                    isPermanentDownload = false,
                    onProgress = { progress ->
                        _uiState.value = _uiState.value.copy(downloadProgress = progress)
                    }
                )

                result.onSuccess { file ->
                    loadPdfFile(file)
                }.onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Failed to download PDF."
                    )
                }
            }
        }
    }

    private val pageCache = mutableMapOf<Int, Bitmap>()

    private suspend fun loadPdfFile(file: File) = withContext(Dispatchers.IO) {
        pageCache.clear()
        val pageCount = pdfManager.getPageCount(file)
        val firstPageBitmap = pdfManager.renderPdfPage(file, 0, targetScreenWidthPx)
        if (firstPageBitmap != null) {
            pageCache[0] = firstPageBitmap
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            localFile = file,
            totalPages = pageCount,
            currentPage = 0,
            currentPageBitmap = firstPageBitmap
        )

        // Prefetch page 1 if available
        if (pageCount > 1) {
            val secondPage = pdfManager.renderPdfPage(file, 1, targetScreenWidthPx)
            if (secondPage != null) {
                pageCache[1] = secondPage
            }
        }
    }

    fun goToPage(pageIndex: Int) {
        val file = _uiState.value.localFile ?: return
        val total = _uiState.value.totalPages
        if (pageIndex < 0 || pageIndex >= total) return
        if (pageIndex == _uiState.value.currentPage && _uiState.value.currentPageBitmap != null) return

        val cached = pageCache[pageIndex]
        if (cached != null) {
            _uiState.value = _uiState.value.copy(
                currentPage = pageIndex,
                currentPageBitmap = cached
            )
            prefetchNeighbors(file, pageIndex, total)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = pdfManager.renderPdfPage(file, pageIndex, targetScreenWidthPx)
            if (bitmap != null) {
                pageCache[pageIndex] = bitmap
            }
            _uiState.value = _uiState.value.copy(
                currentPage = pageIndex,
                currentPageBitmap = bitmap
            )
            prefetchNeighbors(file, pageIndex, total)
        }
    }

    private fun prefetchNeighbors(file: File, currentPage: Int, totalPages: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val next = currentPage + 1
            if (next < totalPages && !pageCache.containsKey(next)) {
                pdfManager.renderPdfPage(file, next, targetScreenWidthPx)?.let {
                    pageCache[next] = it
                }
            }
            val prev = currentPage - 1
            if (prev >= 0 && !pageCache.containsKey(prev)) {
                pdfManager.renderPdfPage(file, prev, targetScreenWidthPx)?.let {
                    pageCache[prev] = it
                }
            }
        }
    }

    fun nextPage() {
        goToPage(_uiState.value.currentPage + 1)
    }

    fun previousPage() {
        goToPage(_uiState.value.currentPage - 1)
    }
}
