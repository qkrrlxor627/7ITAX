package com.example.a2026_oneday_journal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun JournalContent(modifier: Modifier){
    Column(modifier = modifier.padding(16.dp)) {

        Text("오늘의 한줄 / ${LocalDate.now().toString()}")

        Text("테스트 1")
        Text("테스트 1")
        Text("테스트 1")
        Text("테스트 1")
        Text("테스트 1")
        Text("테스트 1")

    }
}