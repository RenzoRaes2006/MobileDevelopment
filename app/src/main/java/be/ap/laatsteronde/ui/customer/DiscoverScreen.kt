package be.ap.laatsteronde.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import be.ap.laatsteronde.ui.theme.Kraft

/**
 * The Discover tab. For now only the header of the Figma frame "4 - Ontdekken": the small label,
 * the municipality in big letters (a tap lets the customer change the place) and the profile
 * button. The filters and the lists of packages come in a later week (K3).
 */
@Composable
fun DiscoverScreen(
    municipality: MunicipalityState,
    onChangeLocation: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 20.dp, end = 76.dp)
        ) {
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
        ProfileMenuButton(
            onLogout = onLogout,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 26.dp, end = 16.dp)
        )
        Text(
            text = stringResource(R.string.customer_discover_placeholder),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
