package be.ap.laatsteronde.ui.customer

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import be.ap.laatsteronde.ui.auth.FieldError
import be.ap.laatsteronde.ui.theme.Kraft

/**
 * The Discover tab (Figma "4 - Ontdekken"): the header with the municipality (a tap lets the customer
 * change the place) and the profile button, and under it the list "Dichtbij jou" with all packages
 * of today, nearest first. The customer can only look at them for now. The filters and the carousel
 * "Laatste ronde" of the Figma come later.
 */
@Composable
fun DiscoverScreen(
    municipality: MunicipalityState,
    packages: CustomerPackagesUiState,
    onChangeLocation: () -> Unit,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp)
        ) {
            DiscoverHeader(
                municipality = municipality,
                onChangeLocation = onChangeLocation,
                modifier = Modifier.padding(end = 60.dp) // the profile button sits at the right
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.customer_packages_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.customer_packages_sorted),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when {
                packages.isLoading -> Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                packages.offers.isEmpty() && packages.errorMessage != null -> {
                    FieldError(packages.errorMessage)
                    OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
                        Text(stringResource(R.string.customer_packages_retry))
                    }
                }
                packages.offers.isEmpty() -> Text(
                    text = stringResource(R.string.customer_packages_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        packages.offers.forEach { offer -> PackageOfferCard(offer) }
                    }
                    // A message about a refresh that failed; the old list stays visible.
                    packages.errorMessage?.let { FieldError(it, modifier = Modifier.padding(top = 8.dp)) }
                }
            }
        }

        ProfileMenuButton(
            onLogout = onLogout,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 26.dp, end = 16.dp)
        )
    }
}

/** The small label and the municipality in big letters; a tap lets the customer choose another place. */
@Composable
private fun DiscoverHeader(
    municipality: MunicipalityState,
    onChangeLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val name = (municipality as? MunicipalityState.Found)?.name
        Text(
            text = stringResource(
                if (municipality is MunicipalityState.Unknown) R.string.customer_discover_label_unknown
                else R.string.customer_discover_label
            ),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (municipality is MunicipalityState.Loading) {
            // Grey block while we look up the municipality (Figma "Toestanden: laden").
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(width = 160.dp, height = 30.dp)
                    .background(Kraft, RoundedCornerShape(12.dp))
            )
            // Keeps the room of the name, so the list does not jump when the name arrives.
            Spacer(Modifier.height(18.dp))
        } else {
            val changeLabel = stringResource(R.string.customer_change_location)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .semantics { contentDescription = changeLabel }
                    .clickable(role = Role.Button, onClick = onChangeLocation)
            ) {
                Text(
                    text = name ?: stringResource(R.string.customer_discover_unknown_name),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp,
                        lineHeight = 48.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(20.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down_large),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
