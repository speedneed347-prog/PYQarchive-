package com.example.ui.downloads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Paper
import com.example.data.repository.ArchiveRepository
import com.example.data.repository.PdfManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ArchiveRepository(application)
    val pdfManager = PdfManager(application)

    val downloadedPapers: StateFlow<List<Paper>> = repository.downloadedPapers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritePapers: StateFlow<List<Paper>> = repository.favoritePapers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteDownload(paperId: String) {
        viewModelScope.launch {
            pdfManager.deleteDownload(paperId)
        }
    }

    fun toggleFavorite(paper: Paper) {
        viewModelScope.launch {
            repository.toggleFavorite(paper.id, !paper.isFavorite)
        }
    }
}
