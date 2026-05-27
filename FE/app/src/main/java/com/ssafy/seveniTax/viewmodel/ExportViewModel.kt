package com.ssafy.seveniTax.viewmodel

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.repository.TaxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

data class ExportUiState(
    val isExporting: Boolean = false,
    val successFileName: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val taxRepository: TaxRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun exportBookEntries(year: Int?) {
        exportFile(
            filePrefix = "book_entries_${year ?: "all"}",
            request = { taxRepository.exportBookEntries(year) }
        )
    }

    fun exportVat(year: Int?, half: Int?) {
        exportFile(
            filePrefix = "vat_${year ?: "all"}_${half ?: "all"}",
            request = { taxRepository.exportVat(year, half) }
        )
    }

    fun exportIncomeTax(year: Int?) {
        exportFile(
            filePrefix = "income_tax_${year ?: "all"}",
            request = { taxRepository.exportIncomeTax(year) }
        )
    }

    fun exportLocalTax(year: Int?) {
        exportFile(
            filePrefix = "local_tax_${year ?: "all"}",
            request = { taxRepository.exportLocalTax(year) }
        )
    }

    fun exportVatExcel(year: Int?, half: Int?) {
        exportFile(
            filePrefix = "vat_${year ?: "all"}_${half ?: "all"}",
            request = { taxRepository.exportVatExcel(year, half) }
        )
    }

    fun exportIncomeTaxExcel(year: Int?) {
        exportFile(
            filePrefix = "income_tax_${year ?: "all"}",
            request = { taxRepository.exportIncomeTaxExcel(year) }
        )
    }

    fun exportSimpleLedgerPdf(year: Int?) {
        exportFile(
            filePrefix = "simple_ledger_${year ?: "all"}",
            request = { taxRepository.exportSimpleLedgerPdf(year) }
        )
    }

    fun clearState() {
        _uiState.value = ExportUiState()
    }

    private fun exportFile(
        filePrefix: String,
        request: suspend () -> Response<ResponseBody>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, successFileName = null, errorMessage = null) }

            try {
                val response = request()
                val code = response.code()
                val body = response.body()
                val errorBody = if (!response.isSuccessful) response.errorBody()?.string() else null
                android.util.Log.d("ExportVM", "▶ export: HTTP $code | hasBody=${body != null} | prefix=$filePrefix")
                if (errorBody != null) android.util.Log.e("ExportVM", "  errorBody=$errorBody")

                if (!response.isSuccessful || body == null) {
                    val errMsg = errorBody?.take(100) ?: "파일 내보내기에 실패했습니다 (HTTP $code)"
                    android.util.Log.e("ExportVM", "  실패: $errMsg")
                    _uiState.update {
                        it.copy(isExporting = false, errorMessage = errMsg)
                    }
                    return@launch
                }

                val fileName = extractFileName(response, filePrefix)
                android.util.Log.d("ExportVM", "  저장: $fileName")
                saveToDownloads(fileName, body)
                _uiState.update {
                    it.copy(isExporting = false, successFileName = fileName, errorMessage = null)
                }
            } catch (e: Exception) {
                android.util.Log.e("ExportVM", "  에러: ${e.javaClass.simpleName} ${e.message}", e)
                _uiState.update {
                    it.copy(isExporting = false, errorMessage = "파일 저장 중 오류: ${e.message}")
                }
            }
        }
    }

    private fun extractFileName(
        response: Response<ResponseBody>,
        fallbackPrefix: String
    ): String {
        val disposition = response.headers()["Content-Disposition"].orEmpty()
        val headerFileName = disposition
            .substringAfter("filename=", "")
            .trim()
            .trim('"')
            .takeIf { it.isNotBlank() }

        return headerFileName ?: "$fallbackPrefix-${UUID.randomUUID()}.csv"
    }

    private fun saveToDownloads(fileName: String, body: ResponseBody) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Failed to create download entry")

        try {
            resolver.openOutputStream(uri)?.use { output ->
                body.byteStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Failed to open download output stream")

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }
}
