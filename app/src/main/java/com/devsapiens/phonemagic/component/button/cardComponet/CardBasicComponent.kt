package com.devsapiens.phonemagic.component.button.cardComponet

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsapiens.phonemagic.R
import com.devsapiens.phonemagic.ui.theme.Coral
import com.devsapiens.phonemagic.ui.theme.LightBackground
import com.devsapiens.phonemagic.ui.theme.Navy800

@Composable
fun CardBasicComponent(
    modifier: Modifier = Modifier,
    label: String,
    iconRes: Int,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val bgColor = if (selected) Coral else Navy800
    Card(
        modifier = modifier
            .width(120.dp)
            .padding(12.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(40.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(LightBackground)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = LightBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview
@Composable
fun CardBasicComponentPreview() {
    CardBasicComponent(label = "Exposición", iconRes = R.drawable.ic_exposure_24, selected = true)
}