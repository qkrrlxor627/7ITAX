package com.ssafy.seveniTax.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssafy.seveniTax.ui.theme.Divider
import com.ssafy.seveniTax.ui.theme.TextPrimary
import com.ssafy.seveniTax.ui.theme.TextSecondary

@Composable
fun FormFieldReadOnly(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Divider)
    }
}

fun buildSsnDisplay(front: String, back: String): String {
    if (front.isBlank() && back.isBlank()) return ""

    return buildString {
        append(front)
        append("-")
        append(back)
        append("*".repeat(6))
    }
}

fun formatPhoneForm(raw: String): String {
    return when {
        raw.length <= 3 -> raw
        raw.length <= 7 -> "${raw.substring(0, 3)}-${raw.substring(3)}"
        else -> "${raw.substring(0, 3)}-${raw.substring(3, 7)}-${raw.substring(7)}"
    }
}
