package com.ssafy.seveniTax.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.seveniTax.ui.theme.Typography

@Composable
fun CardInputForm(
    cardNumber: String,
    expiry: String,
    cvc: String,
    onCardNumberChange: (String) -> Unit,
    onExpiryChange: (String) -> Unit,
    onCvcChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("카드 번호", style = Typography.labelLarge)
        TaxInput(
            value = cardNumber,
            onValueChange = { if (it.length <= 16) onCardNumberChange(it) },
            placeholder = "0000 0000 0000 0000",
            keyboardType = KeyboardType.Number
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("유효기간", style = Typography.labelLarge)
                TaxInput(
                    value = expiry,
                    onValueChange = { if (it.length <= 4) onExpiryChange(it) },
                    placeholder = "MM/YY",
                    keyboardType = KeyboardType.Number
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("CVC", style = Typography.labelLarge)
                TaxInput(
                    value = cvc,
                    onValueChange = { if (it.length <= 3) onCvcChange(it) },
                    placeholder = "CVC",
                    keyboardType = KeyboardType.Number
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CardInputFormPreview() {
    CardInputForm("", "", "", {}, {}, {})
}
