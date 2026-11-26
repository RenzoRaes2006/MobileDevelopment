package be.ap.laatsteronde.ui.merchant

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.CameraPhoto
import be.ap.laatsteronde.data.GalleryPhoto
import be.ap.laatsteronde.ui.auth.FieldError
import be.ap.laatsteronde.ui.theme.OSMTheme

/**
 * "Nieuw pakket" (Figma "Pakket aanmaken"): the form to put a package online. It has no navigation
 * bar; the cross at the top and "Pakket online zetten" both lead back to "Vandaag" ([onDone]).
 * The photo comes from the camera (TakePicturePreview) or the gallery (PickVisualMedia); neither needs a
 * permission (H4). All logic is in [PackageFormViewModel].
 */
@Composable
fun NewPackageScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PackageFormViewModel = viewModel(factory = PackageFormViewModel.factory(LocalContext.current))
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The camera app gives a small bitmap, or null when the merchant went back without a photo.
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { viewModel.onPhotoPicked(CameraPhoto(it)) }
    }
    // The photo picker of Android gives the address of the chosen photo, or null when nothing was chosen.
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onPhotoPicked(GalleryPhoto(it)) }
    }

    // After a successful save the list on Vandaag shows the new package.
    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    NewPackageContent(
        state = state,
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onPriceChange = viewModel::onPriceChange,
        onOriginalValueChange = viewModel::onOriginalValueChange,
        onQuantityChange = viewModel::onQuantityChange,
        onPickupFromChange = viewModel::onPickupFromChange,
        onPickupToChange = viewModel::onPickupToChange,
        onCamera = {
            try {
                cameraLauncher.launch(null)
            } catch (e: ActivityNotFoundException) {
                viewModel.onCameraUnavailable() // a phone without a camera app
            }
        },
        onGallery = {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onRemovePhoto = viewModel::onPhotoRemoved,
        onSave = viewModel::save,
        onClose = onDone,
        modifier = modifier
    )
}

/** The stateless part of the screen. */
@Composable
private fun NewPackageContent(
    state: PackageFormUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onOriginalValueChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onPickupFromChange: (String) -> Unit,
    onPickupToChange: (String) -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val decimalKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    Column(modifier = modifier.fillMaxSize()) {
        // Header: the cross and the title.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.package_close),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(role = Role.Button, onClick = onClose)
            )
            Text(
                text = stringResource(R.string.package_new_title),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            FieldLabel(R.string.package_label_photo)
            PackagePhotoPicker(
                photoBase64 = state.photoBase64,
                isProcessing = state.isProcessingPhoto,
                photoError = state.photoError,
                onCamera = onCamera,
                onGallery = onGallery,
                onRemove = onRemovePhoto
            )

            Spacer(Modifier.height(16.dp))
            LabeledField(R.string.package_label_title) {
                FormTextField(
                    value = state.title,
                    onValueChange = onTitleChange,
                    label = stringResource(R.string.package_label_title),
                    isError = state.titleError != null
                )
            }
            state.titleError?.let { FieldError(it) }

            Spacer(Modifier.height(16.dp))
            LabeledField(R.string.package_label_description) {
                FormTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = stringResource(R.string.package_label_description),
                    height = 100.dp,
                    multiline = true,
                    isError = state.descriptionError != null
                )
            }
            state.descriptionError?.let { FieldError(it) }

            // Price and original value next to each other; the messages come under both fields.
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField(R.string.package_label_price, modifier = Modifier.weight(1f)) {
                    FormTextField(
                        value = state.price,
                        onValueChange = onPriceChange,
                        label = stringResource(R.string.package_label_price),
                        isError = state.priceError != null,
                        keyboardOptions = decimalKeyboard
                    )
                }
                LabeledField(R.string.package_label_original, modifier = Modifier.weight(1f)) {
                    FormTextField(
                        value = state.originalValue,
                        onValueChange = onOriginalValueChange,
                        label = stringResource(R.string.package_label_original),
                        isError = state.originalValueError != null,
                        keyboardOptions = decimalKeyboard
                    )
                }
            }
            state.priceError?.let { FieldError(it) }
            state.originalValueError?.let { FieldError(it) }

            Spacer(Modifier.height(16.dp))
            LabeledField(R.string.package_label_quantity) {
                FormTextField(
                    value = state.quantity,
                    onValueChange = onQuantityChange,
                    label = stringResource(R.string.package_label_quantity),
                    isError = state.quantityError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            state.quantityError?.let { FieldError(it) }

            Spacer(Modifier.height(16.dp))
            FieldLabel(R.string.package_label_window)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField(R.string.package_label_from, size = 12, modifier = Modifier.weight(1f)) {
                    FormTextField(
                        value = state.pickupFrom,
                        onValueChange = onPickupFromChange,
                        label = stringResource(R.string.package_label_from),
                        isError = state.timeError != null
                    )
                }
                LabeledField(R.string.package_label_to, size = 12, modifier = Modifier.weight(1f)) {
                    FormTextField(
                        value = state.pickupTo,
                        onValueChange = onPickupToChange,
                        label = stringResource(R.string.package_label_to),
                        isError = state.timeError != null
                    )
                }
            }
            state.timeError?.let { FieldError(it) }
            state.errorMessage?.let { FieldError(it) }
            Spacer(Modifier.height(16.dp))
        }

        // The button stays at the bottom while the form scrolls.
        Button(
            onClick = onSave,
            enabled = !state.isSaving && !state.isProcessingPhoto,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .heightIn(min = 52.dp)
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.package_button_publish), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 844, widthDp = 390)
@Composable
private fun NewPackageContentPreview() {
    OSMTheme(darkTheme = false) {
        NewPackageContent(
            state = PackageFormUiState(
                title = "Brood en koffiekoeken",
                description = "Een mix van brood en koffiekoeken van vandaag.",
                price = "14,00",
                originalValue = "12,00",
                quantity = "10",
                pickupFrom = "18:00",
                pickupTo = "19:30",
                priceError = R.string.error_package_price_not_lower
            ),
            onTitleChange = {}, onDescriptionChange = {}, onPriceChange = {}, onOriginalValueChange = {},
            onQuantityChange = {}, onPickupFromChange = {}, onPickupToChange = {},
            onCamera = {}, onGallery = {}, onRemovePhoto = {}, onSave = {}, onClose = {}
        )
    }
}
