package com.example.a2026_oneday_journal.mvi

import androidx.compose.ui.input.pointer.PointerInputChange

//싱글톤 패턴
//액션
sealed interface JournalIntent {
    data class OnTextChange(val userInputChange: String)
    object OnSave: JournalIntent
}