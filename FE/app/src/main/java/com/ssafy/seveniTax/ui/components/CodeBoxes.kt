package com.ssafy.seveniTax.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.seveniTax.ui.theme.*

@Composable
fun CodeBoxes(
    code: String,
    length: Int = 6,
    masked: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(length) { index ->
            val filled = index < code.length
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 62.dp)
                    .background(
                        if (filled) BrandPurple else BoxPlaceholder,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (filled) {
                    Text(
                        text = if (masked) "\u2022" else code[index].toString(),
                        fontSize = 24.sp,
                        color = Background
                    )
                }
            }
        }
    }
}
