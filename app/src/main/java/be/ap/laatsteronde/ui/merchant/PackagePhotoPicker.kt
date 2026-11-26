package be.ap.laatsteronde.ui.merchant

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import be.ap.laatsteronde.R
import be.ap.laatsteronde.ui.auth.FieldError
import be.ap.laatsteronde.ui.components.PackagePhoto
import be.ap.laatsteronde.ui.theme.CardBorder

/**
 * The photo part of "Nieuw pakket" (Figma "12 - Pakket aanmaken", "Fotozone"): a dashed area with the
 * chosen photo or "Nog geen foto gekozen", and under it the buttons Camera and Galerij (H4).
 * The photo itself is in the ViewModel; this part only shows it and passes on the taps.
 */
@Composable
fun PackagePhotoPicker(
    photoBase64: String?,
    isProcessing: Boolean,
    @StringRes photoError: Int?,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .dashedBorder()
        ) {
            if (photoBase64 != null) {
                PackagePhoto(
                    photoBase64 = photoBase64,
                    contentDescription = stringResource(R.string.package_photo_chosen),
                    cornerRadius = 14.dp,
                    modifier = Modifier.fillMaxSize()
                )
                // A cross in the corner removes the photo; the white circle keeps it visible on any photo.
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.package_photo_remove),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
            } else if (!isProcessing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_image),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = stringResource(R.string.package_photo_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            if (isProcessing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Text(
                        text = stringResource(R.string.package_photo_processing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            PhotoSourceButton(R.drawable.ic_camera, R.string.package_photo_camera, onCamera, !isProcessing, Modifier.weight(1f))
            PhotoSourceButton(R.drawable.ic_image, R.string.package_photo_gallery, onGallery, !isProcessing, Modifier.weight(1f))
        }
        photoError?.let { FieldError(it) }
    }
}

/** One of the two buttons under the photo (Figma "Knop - Camera", "Knop - Galerij"): 48 dp high, white with a border. */
@Composable
private fun PhotoSourceButton(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit, enabled: Boolean, modifier: Modifier) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/** The dashed rounded border of the photo area, as in the Figma (Compose has no dashed border of its own). */
private fun Modifier.dashedBorder(): Modifier = drawBehind {
    val strokeWidth = 1.5.dp.toPx()
    val radius = 14.dp.toPx()
    drawRoundRect(
        color = CardBorder,
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
        )
    )
}
