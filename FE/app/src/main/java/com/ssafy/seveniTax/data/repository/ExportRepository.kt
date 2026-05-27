package com.ssafy.seveniTax.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.ssafy.seveniTax.data.remote.ExportApi
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

class ExportRepository @Inject constructor(
    private val exportApi: ExportApi,
    @ApplicationContext private val context: Context
) {

    suspend fun exportBookEntries(year: Int): Result<String> {
        return downloadAndSave(
            response = exportApi.exportBookEntries(year),
            fileName = "간편장부_${year}.csv"
        )
    }

    suspend fun exportVat(year: Int, half: Int): Result<String> {
        return downloadAndSave(
            response = exportApi.exportVat(year, half),
            fileName = "부가세_${year}_${half}기.csv"
        )
    }

    suspend fun exportIncomeTax(year: Int): Result<String> {
        return downloadAndSave(
            response = exportApi.exportIncomeTax(year),
            fileName = "종합소득세_${year}.csv"
        )
    }

    private fun downloadAndSave(response: Response<ResponseBody>, fileName: String): Result<String> {
        if (!response.isSuccessful || response.body() == null) {
            return Result.failure(Exception("다운로드에 실패했습니다"))
        }

        return try {
            val bytes = response.body()!!.bytes()
            saveToDownloads(bytes, fileName)
            Result.success(fileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveToDownloads(data: ByteArray, fileName: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw Exception("파일 생성에 실패했습니다")

        resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(data)
        } ?: throw Exception("파일 저장에 실패했습니다")
    }
}
