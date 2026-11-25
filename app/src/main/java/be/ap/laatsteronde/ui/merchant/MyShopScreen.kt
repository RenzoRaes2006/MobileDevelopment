package be.ap.laatsteronde.ui.merchant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.Coordinates
import be.ap.laatsteronde.data.ShopCategory
import be.ap.laatsteronde.ui.auth.FieldError
import be.ap.laatsteronde.ui.customer.HintChip
import be.ap.laatsteronde.ui.customer.LocationViewModel
import be.ap.laatsteronde.ui.customer.ProfileMenuButton
import be.ap.laatsteronde.ui.map.MarkerMap
import be.ap.laatsteronde.ui.theme.CardBorder
import be.ap.laatsteronde.ui.theme.DeepGreen
import be.ap.laatsteronde.ui.theme.MapBackground
import be.ap.laatsteronde.ui.theme.isDarkTheme

/**
 * "Mijn winkel" (Figma "10 - Mijn winkel"): the form to fill in and change the shop. A merchant
 * without a shop starts here. All logic is in [ShopViewModel].
 */
@Composable
fun MyShopScreen(
    viewModel: ShopViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MyShopContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onCategoryChange = viewModel::onCategoryChange,
        onPinPlaced = viewModel::onPinPlaced,
        onStreetChange = viewModel::onStreetChange,
        onHouseNumberChange = viewModel::onHouseNumberChange,
        onPostcodeChange = viewModel::onPostcodeChange,
        onMunicipalityChange = viewModel::onMunicipalityChange,
        onSave = viewModel::save,
        onLogout = onLogout,
        modifier = modifier
    )
}

/** The stateless part of the screen. */
@Composable
private fun MyShopContent(
    state: ShopUiState,
    onNameChange: (String) -> Unit,
    onCategoryChange: (ShopCategory) -> Unit,
    onPinPlaced: (Coordinates) -> Unit,
    onStreetChange: (String) -> Unit,
    onHouseNumberChange: (String) -> Unit,
    onPostcodeChange: (String) -> Unit,
    onMunicipalityChange: (String) -> Unit,
    onSave: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 108.dp) // room for the floating bar
        ) {
            Text(
                text = stringResource(R.string.shop_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 60.dp) // the profile button sits at the right
            )
            Text(
                text = stringResource(R.string.shop_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            LabeledField(R.string.shop_label_name) {
                FormTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.shop_label_name),
                    isError = state.nameError != null
                )
            }
            state.nameError?.let { FieldError(it) }

            Spacer(Modifier.height(16.dp))
            LabeledField(R.string.shop_label_category) {
                CategoryDropdown(
                    selected = state.category,
                    onSelect = onCategoryChange,
                    isError = state.categoryError != null
                )
            }
            state.categoryError?.let { FieldError(it) }

            Spacer(Modifier.height(16.dp))
            LabeledField(R.string.shop_label_location) {
                PinMap(state = state, onPinPlaced = onPinPlaced)
            }
            state.pinError?.let { FieldError(it) }

            Spacer(Modifier.height(16.dp))
            AddressCard(
                state = state,
                onStreetChange = onStreetChange,
                onHouseNumberChange = onHouseNumberChange,
                onPostcodeChange = onPostcodeChange,
                onMunicipalityChange = onMunicipalityChange
            )
            state.addressError?.let { FieldError(it) }
            state.errorMessage?.let { FieldError(it) }
            if (state.justSaved) {
                Text(
                    text = stringResource(R.string.shop_saved),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }

            Button(
                onClick = onSave,
                enabled = !state.isSaving && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .heightIn(min = 52.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.shop_button_save), style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Not in this Figma frame, but on "Vandaag": without it the merchant could not log out here.
        ProfileMenuButton(
            onLogout = onLogout,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 26.dp, end = 16.dp)
        )
    }
}

/** The map of 220 with the pin of the shop: tap on the map or drag the pin (Figma "Kaart · pin zetten"). */
@Composable
private fun PinMap(state: ShopUiState, onPinPlaced: (Coordinates) -> Unit) {
    val description = stringResource(R.string.shop_map_description)
    val shape = RoundedCornerShape(14.dp)
    // The map looks at the pin (or the default place when there is none yet), once the shop is loaded.
    // It is not recalculated for every new pin, so the map does not jump around while the merchant
    // places the pin; after a rotation the map is rebuilt and starts on the pin where it is now.
    val center = remember(state.isLoading) { state.pin ?: LocationViewModel.DEFAULT_LOCATION }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(shape)
            .background(MapBackground)
            .border(
                width = if (state.pinError != null) 2.dp else 1.dp,
                color = if (state.pinError != null) MaterialTheme.colorScheme.error else CardBorder,
                shape = shape
            )
            .semantics { contentDescription = description }
    ) {
        MarkerMap(
            position = state.pin,
            centerOn = center,
            markerIcon = R.drawable.marker_shop_pin,
            pinStyle = true,
            draggable = true,
            onPositionPicked = onPinPlaced,
            modifier = Modifier.fillMaxSize()
        )
        HintChip(
            text = stringResource(R.string.shop_map_hint),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, bottom = 12.dp)
        )
    }
}

/** The white card "Adres" with street, number, postcode and municipality (Figma "Adresblok"). */
@Composable
private fun AddressCard(
    state: ShopUiState,
    onStreetChange: (String) -> Unit,
    onHouseNumberChange: (String) -> Unit,
    onPostcodeChange: (String) -> Unit,
    onMunicipalityChange: (String) -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val fieldsEnabled = !state.isLookingUpAddress
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, CardBorder, shape)
            .padding(14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.shop_label_address),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            when {
                state.isLookingUpAddress -> Text(
                    text = stringResource(R.string.shop_address_searching),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.addressAutoFilled -> Text(
                    text = stringResource(R.string.shop_address_auto),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDarkTheme()) MaterialTheme.colorScheme.primary else DeepGreen
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledField(R.string.shop_label_street, size = 12, modifier = Modifier.weight(216f)) {
                FormTextField(
                    value = state.street,
                    onValueChange = onStreetChange,
                    label = stringResource(R.string.shop_label_street),
                    height = 44.dp, radius = 10.dp, textSize = 15,
                    enabled = fieldsEnabled,
                    isError = state.addressError != null && state.street.isBlank()
                )
            }
            LabeledField(R.string.shop_label_number, size = 12, modifier = Modifier.weight(104f)) {
                FormTextField(
                    value = state.houseNumber,
                    onValueChange = onHouseNumberChange,
                    label = stringResource(R.string.shop_label_number),
                    height = 44.dp, radius = 10.dp, textSize = 15,
                    enabled = fieldsEnabled
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledField(R.string.shop_label_postcode, size = 12, modifier = Modifier.weight(104f)) {
                FormTextField(
                    value = state.postcode,
                    onValueChange = onPostcodeChange,
                    label = stringResource(R.string.shop_label_postcode),
                    height = 44.dp, radius = 10.dp, textSize = 15,
                    enabled = fieldsEnabled,
                    isError = state.addressError != null && state.postcode.isBlank(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            LabeledField(R.string.shop_label_municipality, size = 12, modifier = Modifier.weight(216f)) {
                FormTextField(
                    value = state.municipality,
                    onValueChange = onMunicipalityChange,
                    label = stringResource(R.string.shop_label_municipality),
                    height = 44.dp, radius = 10.dp, textSize = 15,
                    enabled = fieldsEnabled,
                    isError = state.addressError != null && state.municipality.isBlank()
                )
            }
        }
    }
}
