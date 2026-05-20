package com.stillfresh.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stillfresh.R

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    Image(
        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
        contentDescription = "Still Fresh Logo",
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    )
}
