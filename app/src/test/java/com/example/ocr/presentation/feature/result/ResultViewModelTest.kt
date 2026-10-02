package com.example.ocr.presentation.feature.result

import com.example.ocr.core.common.Resource
import com.example.ocr.domain.model.OCRDocument
import com.example.ocr.domain.repository.OCRRepository
import com.example.ocr.domain.usecase.DeleteDocumentUseCase
import com.example.ocr.domain.usecase.ExportWordUseCase
import com.example.ocr.domain.usecase.GetDocumentByIdUseCase
import com.example.ocr.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class ResultViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository: OCRRepository = mockk()
    private lateinit var viewModel: ResultViewModel

    private val doc = OCRDocument(id = 7, title = "Scan", fullText = "xin chào", imagePath = "")

    @Before
    fun setUp() {
        viewModel = ResultViewModel(
            GetDocumentByIdUseCase(repository),
            ExportWordUseCase(repository),
            DeleteDocumentUseCase(repository),
        )
    }

    @Test
    fun `loadDocument emits Success when document exists`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDocumentById(7) } returns doc

        viewModel.loadDocument(7)
        advanceUntilIdle()

        assertEquals(ResultUiState.Success(doc), viewModel.uiState.value)
    }

    @Test
    fun `loadDocument emits Error when document is missing`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDocumentById(any()) } returns null

        viewModel.loadDocument(99)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ResultUiState.Error)
    }

    @Test
    fun `downloadWord emits Done with the exported file`() = runTest(mainRule.dispatcher) {
        val file = File("scan.docx")
        coEvery { repository.getDocumentById(7) } returns doc
        coEvery { repository.exportToWord(doc) } returns Resource.Success(file)

        viewModel.loadDocument(7)
        advanceUntilIdle()
        viewModel.downloadWord()
        advanceUntilIdle()

        assertEquals(ExportState.Done(file), viewModel.exportState.value)
    }

    @Test
    fun `downloadWord emits Failed with server message`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDocumentById(7) } returns doc
        coEvery { repository.exportToWord(doc) } returns Resource.Error("Server lỗi 500")

        viewModel.loadDocument(7)
        advanceUntilIdle()
        viewModel.downloadWord()
        advanceUntilIdle()

        assertEquals(ExportState.Failed("Server lỗi 500"), viewModel.exportState.value)
    }

    @Test
    fun `downloadWord does nothing before a document is loaded`() = runTest(mainRule.dispatcher) {
        viewModel.downloadWord()
        advanceUntilIdle()

        assertEquals(ExportState.Idle, viewModel.exportState.value)
        coVerify(exactly = 0) { repository.exportToWord(any()) }
    }

    @Test
    fun `deleteDocument removes it and invokes callback`() = runTest(mainRule.dispatcher) {
        coEvery { repository.getDocumentById(7) } returns doc
        coEvery { repository.deleteDocument(7) } returns Unit
        var deleted = false

        viewModel.loadDocument(7)
        advanceUntilIdle()
        viewModel.deleteDocument { deleted = true }
        advanceUntilIdle()

        coVerify { repository.deleteDocument(7) }
        assertTrue(deleted)
    }
}
