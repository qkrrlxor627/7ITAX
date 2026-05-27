package com.ssafy.seveniTax.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.seveniTax.ui.theme.TextPrimary

@Composable
fun PinKeypad(
    onNumberClick: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9),
        listOf(-1, 0, -2)  // -1: 빈칸, -2: 삭제
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    when (key) {
                        -1 -> Spacer(modifier = Modifier.size(72.dp))
                        -2 -> TextButton(onClick = onDelete, modifier = Modifier.size(72.dp)) {
                            Text("←", fontSize = 24.sp, color = TextPrimary)
                        }
                        else -> TextButton(
                            onClick = { onNumberClick(key) },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Text(key.toString(), fontSize = 24.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PinKeypadPreview() {
    PinKeypad(onNumberClick = {}, onDelete = {})
}
