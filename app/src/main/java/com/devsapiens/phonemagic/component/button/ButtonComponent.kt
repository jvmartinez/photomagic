package com.devsapiens.phonemagic.component.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devsapiens.phonemagic.ui.theme.Coral
import com.devsapiens.phonemagic.ui.theme.LightBackground

@Composable
fun ButtonComponent(
    modifier: Modifier = Modifier,
    config: ConfigButton<Unit>,
) {
    Button(
        onClick = { config.onClick(Unit) },
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        colors = ButtonDefaults.buttonColors(
            containerColor = config.bgColor ?: Coral,
            contentColor = config.textColor ?: LightBackground,
            disabledContainerColor = config.bgColor ?: LightBackground,
            disabledContentColor = config.textColor ?: Coral,
        ),
        border = BorderStroke(
            width = 2.dp,
            color = LightBackground
        ),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(text = config.title)
    }
}
