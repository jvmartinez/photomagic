package com.devsapiens.phonemagic.component.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsapiens.phonemagic.ui.theme.Coral
import com.devsapiens.phonemagic.ui.theme.LightBackground

enum class TypeLabel {
    Vertical,
    Horizontal
}
@Composable
fun ButtonWithLabelComponent(
    modifier: Modifier? = null,
    label: String,
    icon: Int,
    typeLabel: TypeLabel = TypeLabel.Vertical,
    action: () -> Unit
) {
    val modifierCurrency = modifier ?: Modifier.wrapContentWidth().wrapContentHeight()
    if (typeLabel == TypeLabel.Vertical) {
        Column(
            modifier = modifierCurrency
        ) {
            IconButton(
                modifier = Modifier.align(CenterHorizontally),
                onClick = { action() }
            ) {
                Image(
                    painter = painterResource(icon),
                    colorFilter = ColorFilter.tint(Coral),
                    contentDescription = "Reset"
                )
            }
            Text(
                text = label, modifier = Modifier
                    .width(100.dp)
                    .clickable {

                    },
                textAlign = TextAlign.Center,
                color = LightBackground
            )
        }
    } else {
        Row(
            modifier = modifierCurrency
        ) {
            IconButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = { action() }
            ) {
                Image(
                    painter = painterResource(icon),
                    colorFilter = ColorFilter.tint(Coral),
                    contentDescription = "Reset"
                )
            }
            Text(
                text = label, modifier = Modifier
                    .width(100.dp)
                    .align(Alignment.CenterVertically)
                    .clickable {
                        action()
                    },
                textAlign = TextAlign.Start,
                color = LightBackground
            )
        }
    }
}

@Preview
@Composable
fun ButtonWithLabelComponentPreview() {
    ButtonWithLabelComponent(
        label = "Reset",
        icon = 0,
        typeLabel = TypeLabel.Horizontal

    ) { }
}