package com.ssafy.seveniTax.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.seveniTax.ui.theme.Accent
import com.ssafy.seveniTax.ui.theme.Background
import com.ssafy.seveniTax.ui.theme.Disabled
import com.ssafy.seveniTax.ui.theme.Typography

enum class ButtonVariant { Primary, Secondary }

@Composable
fun TaxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.Primary
) {
    val shape = RoundedCornerShape(18.dp)
    val textStyle = Typography.labelLarge.merge(
        TextStyle(fontSize = (Typography.labelLarge.fontSize.value + 2f).sp)
    )

    when (variant) {
        ButtonVariant.Primary -> Button(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Background,
                disabledContainerColor = Disabled,
                disabledContentColor = Background.copy(alpha = 0.8f)
            )
        ) {
            Text(text = text, style = textStyle, color = Background)
        }

        ButtonVariant.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = enabled,
            shape = shape,
            border = BorderStroke(1.dp, if (enabled) Accent else Disabled),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Accent,
                disabledContentColor = Disabled
            )
        ) {
            Text(text = text, style = textStyle)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TaxButtonPreview() {
    TaxButton(text = "확인", onClick = {})
}
