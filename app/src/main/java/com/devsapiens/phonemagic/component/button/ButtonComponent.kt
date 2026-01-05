package com.devsapiens.phonemagic.component.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsapiens.phonemagic.ui.theme.LightBackground
import com.devsapiens.phonemagic.ui.theme.Navy700
import com.devsapiens.phonemagic.ui.theme.Navy900

@Composable
fun ButtonComponent(
    modifier: Modifier? = null,
    config: ConfigButton<Unit>,
) {
    val modifierDefault = modifier ?: Modifier.fillMaxWidth()
    Button(
        onClick = { config.onClick(Unit) },
        modifier = modifierDefault,
        colors = ButtonDefaults.buttonColors(
            containerColor = config.bgColor ?: Navy900,
            contentColor = config.textColor ?: LightBackground,
            disabledContainerColor = config.bgColor ?: LightBackground,
            disabledContentColor = config.textColor ?: Navy900,
        ),
        border = BorderStroke(
            width = 2.dp,
            color = config.borderColor ?: Navy700
        ),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(text = config.title)
    }
}

@Preview
@Composable
fun ButtonComponentPreview() {
    ButtonComponent(
        config = ConfigButton(
            title = "Reset",
            onClick = { }
        )
    )
}