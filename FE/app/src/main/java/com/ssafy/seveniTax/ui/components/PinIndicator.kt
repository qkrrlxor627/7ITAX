package com.ssafy.seveniTax.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.seveniTax.ui.theme.*

@Composable
fun PinIndicator(
    length: Int = 6,
    filled: Int,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(length) { index ->
            val isFilled = index < filled
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .then(
                        if (isFilled) Modifier.background(Accent)
                        else Modifier.background(Background).border(1.5.dp, Disabled, CircleShape)
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PinIndicatorPreview() {
    PinIndicator(length = 6, filled = 3)
}
