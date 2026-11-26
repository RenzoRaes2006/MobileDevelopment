package be.ap.laatsteronde.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.decodePhoto
import be.ap.laatsteronde.ui.theme.Kraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The photo of a package ([photoBase64]) with rounded corners, in the size given by [modifier], or the grey-brown place with a picture
 * icon when there is no photo (or it cannot be read). The photo is decoded on Dispatchers.Default and only
 * once per photo: produceState remembers the result until [photoBase64] changes, not at every recomposition.
 * [contentDescription] tells TalkBack what the photo shows; the empty place says "Nog geen foto".
 */
@Composable
fun PackagePhoto(
    photoBase64: String?,
    contentDescription: String,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    val image by produceState<ImageBitmap?>(initialValue = null, photoBase64) {
        value = photoBase64?.let { text ->
            withContext(Dispatchers.Default) { decodePhoto(text)?.asImageBitmap() }
        }
    }
    val photo = image
    if (photo != null) {
        Image(
            bitmap = photo,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop, // fill the square, cut off what sticks out
            modifier = modifier.clip(shape)
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.background(Kraft, shape)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_image),
                contentDescription = stringResource(R.string.package_photo_placeholder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
