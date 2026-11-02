package com.example.ui.paper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Paper
import com.example.data.repository.ArchiveRepository
import com.example.data.repository.PdfManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class PaperDetailUiState(
    val paper: Paper? = null,
    val isLoading: Boolean = true,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val downloadedFile: File? = null,
    val personalNotes: String = "",
    val isNotesSaved: Boolean = false,
    val errorMessage: String? = null
)

class PaperDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ArchiveRepository(application)
    val pdfManager = PdfManager(application)

    private val _uiState = MutableStateFlow(PaperDetailUiState())
    val uiState: StateFlow<PaperDetailUiState> = _uiState.asStateFlow()

    fun loadPaper(paperId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val p = repository.fetchPaperDetails(paperId)
            if (p != null) {
                // Record local view (strictly in Room)
                repository.recordLocalPaperView(paperId)
                val localFile = pdfManager.getLocalFile(paperId)

                _uiState.value = _uiState.value.copy(
                    paper = p,
                    isLoading = false,
                    personalNotes = p.personalNotes,
                    downloadedFile = localFile
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Question paper could not be found."
                )
            }
        }
    }

    fun toggleFavorite() {
        val current = _uiState.value.paper ?: return
        viewModelScope.launch {
            val newFav = !current.isFavorite
            repository.toggleFavorite(current.id, newFav)
            _uiState.value = _uiState.value.copy(
                paper = current.copy(isFavorite = newFav)
            )
        }
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(personalNotes = notes, isNotesSaved = false)
    }

    fun saveNotes() {
        val current = _uiState.value.paper ?: return
        viewModelScope.launch {
            repository.savePersonalNotes(current.id, _uiState.value.personalNotes)
            _uiState.value = _uiState.value.copy(isNotesSaved = true)
        }
    }

    fun downloadPdfPermanently(onSuccess: (File) -> Unit, onError: (String) -> Unit) {
        val current = _uiState.value.paper ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true, downloadProgress = 0)
            val result = pdfManager.downloadPdf(
                paperId = current.id,
                rawUrl = current.pdfUrl,
                isPermanentDownload = true,
                onProgress = { progress ->
                    _uiState.value = _uiState.value.copy(downloadProgress = progress)
                }
            )

            result.onSuccess { file ->
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadProgress = 100,
                    downloadedFile = file
                )
                onSuccess(file)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isDownloading = false)
                onError(error.message ?: "Failed to download PDF.")
            }
        }
    }
}
