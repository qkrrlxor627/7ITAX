package com.ssafy.seveniTax.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.seveniTax.data.model.book.BookEntryResponse
import com.ssafy.seveniTax.data.repository.BookEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookEntryDetailViewModel @Inject constructor(
    private val bookEntryRepository: BookEntryRepository
) : ViewModel() {

    private val _entry = MutableStateFlow<BookEntryResponse?>(null)
    val entry: StateFlow<BookEntryResponse?> = _entry.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadEntry(entryId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = bookEntryRepository.getEntry(entryId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    _entry.value = response.body()?.data
                }
            } catch (_: Exception) { }
            finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmEntry(entryId: Long) {
        viewModelScope.launch {
            try {
                val response = bookEntryRepository.confirmEntry(entryId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    _entry.value = response.body()?.data
                }
            } catch (_: Exception) { }
        }
    }
}
