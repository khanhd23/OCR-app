package com.example.ocr.presentation.feature.history

import com.example.ocr.domain.model.OCRDocument
import com.example.ocr.domain.repository.OCRRepository
import com.example.ocr.domain.usecase.DeleteDocumentUseCase
import com.example.ocr.domain.usecase.GetAllDocumentsUseCase
import com.example.ocr.domain.usecase.SearchDocumentsUseCase
import com.example.ocr.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository: OCRRepository = mockk()
    private val allDocs = MutableStateFlow(
        listOf(
            OCRDocument(id = 1, title = "Hoá đơn", fullText = "tổng tiền", imagePath = ""),
            OCRDocument(id = 2, title = "Ghi chú", fullText = "họp lúc 9h", imagePath = ""),
        )
    )
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        every { repository.getAllDocuments() } returns allDocs
        viewModel = HistoryViewModel(
            GetAllDocumentsUseCase(repository),
            DeleteDocumentUseCase(repository),
            SearchDocumentsUseCase(repository),
        )
    }

    @Test
    fun `shows all documents when query is blank`() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { viewModel.documents.collect {} }
        advanceUntilIdle()

        assertEquals(allDocs.value, viewModel.documents.value)
    }

    @Test
    fun `search is debounced and shows only matching results`() = runTest(mainRule.dispatcher) {
        val match = allDocs.value.take(1)
        coEvery { repository.searchDocuments("hoá") } returns match
        backgroundScope.launch { viewModel.documents.collect {} }

        viewModel.onSearchQueryChange("h")
        viewModel.onSearchQueryChange("ho")
        viewModel.onSearchQueryChange("hoá")
        advanceTimeBy(299)
        runCurrent()
        coVerify(exactly = 0) { repository.searchDocuments(any()) }

        advanceUntilIdle()

        coVerify(exactly = 1) { repository.searchDocuments("hoá") }
        assertEquals(match, viewModel.documents.value)
    }

    @Test
    fun `deleteDocument delegates to repository`() = runTest(mainRule.dispatcher) {
        coEvery { repository.deleteDocument(2) } returns Unit

        viewModel.deleteDocument(2)
        advanceUntilIdle()

        coVerify { repository.deleteDocument(2) }
    }
}
