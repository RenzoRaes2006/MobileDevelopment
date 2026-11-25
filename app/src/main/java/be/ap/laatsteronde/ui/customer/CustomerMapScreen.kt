package be.ap.laatsteronde.ui.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.Coordinates
import be.ap.laatsteronde.ui.map.MarkerMap
import be.ap.laatsteronde.ui.theme.TextBody
import be.ap.laatsteronde.ui.theme.isDarkTheme

/**
 * The map tab (Figma "5 - Kaart"): the map with the position of the customer, a bar with the
 * municipality on top and the round "Naar mijn locatie" button. The shop markers and the popup
 * of the Figma come with the packages in a later week.
 *
 * Without a position (no permission, or not found) a tap on the map chooses a place.
 */
@Composable
fun CustomerMapScreen(
    location: LocationUiState,
    municipality: MunicipalityState,
    onLocationChosen: (Coordinates) -> Unit,
    onLocateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = location.status
    // Each tap on the locate button moves the map back to the position, also after looking around.
    var recenterKey by rememberSaveable { mutableIntStateOf(0) }
    val canChoose = status == LocationStatus.DENIED ||
        status == LocationStatus.UNAVAILABLE ||
        status == LocationStatus.CHOSEN
    val showsPosition = status == LocationStatus.FOUND || status == LocationStatus.CHOSEN

    Box(modifier = modifier.fillMaxSize()) {
        MarkerMap(
            position = if (showsPosition) location.location else null,
            centerOn = location.location,
            markerIcon = R.drawable.marker_my_location,
            recenterKey = recenterKey,
            onPositionPicked = if (canChoose) onLocationChosen else null,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            MunicipalityBar(
                title = (municipality as? MunicipalityState.Found)?.name
                    ?: stringResource(R.string.customer_map_title_unknown),
                subtitle = stringResource(
                    when (status) {
                        LocationStatus.FOUND -> R.string.customer_map_sub_found
                        LocationStatus.CHOSEN -> R.string.customer_map_sub_chosen
                        LocationStatus.DENIED, LocationStatus.UNAVAILABLE -> R.string.customer_map_sub_default
                        LocationStatus.UNKNOWN, LocationStatus.LOADING -> R.string.customer_map_sub_searching
                    }
                ),
                modifier = Modifier.weight(1f)
            )
            LocateButton(
                onClick = {
                    recenterKey++
                    onLocateClick()
                }
            )
        }

        if (status == LocationStatus.DENIED || status == LocationStatus.UNAVAILABLE) {
            HintChip(
                text = stringResource(R.string.location_choose_hint),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 76.dp)
            )
        }
    }
}

/** White pill with a pin, the municipality and a small line below it (Figma "Zoekbalk"). */
@Composable
private fun MunicipalityBar(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = modifier.height(52.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_pin_small),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Round white button "Naar mijn locatie" (Figma), 52 dp. */
@Composable
private fun LocateButton(onClick: () -> Unit) {
    val label = stringResource(R.string.customer_map_locate)
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = Modifier
            .size(52.dp)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_locate),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Small white label on the map, in the style of the hint "Tik of versleep de pin" of the Figma. */
@Composable
fun HintChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isDarkTheme()) MaterialTheme.colorScheme.onSurface else TextBody,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
