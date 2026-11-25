package be.ap.laatsteronde.ui.customer

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.ap.laatsteronde.R
import be.ap.laatsteronde.ui.theme.CarrotLight
import be.ap.laatsteronde.ui.theme.DarkSelectedTint
import be.ap.laatsteronde.ui.theme.NeutralCard
import be.ap.laatsteronde.ui.theme.OSMTheme
import be.ap.laatsteronde.ui.theme.TextBody
import be.ap.laatsteronde.ui.theme.isDarkTheme

/**
 * Explanation before the system asks for the location (Figma "3 - Locatie-uitleg").
 * "Locatie toestaan" goes on to the system question, "Zelf een locatie kiezen" skips it.
 */
@Composable
fun LocationExplanationScreen(
    onAllow: () -> Unit,
    onChooseOwnLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isDarkTheme()
    val bodyColor = if (dark) MaterialTheme.colorScheme.onSurface else TextBody

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 24.dp)
    ) {
        // The text scrolls on a low screen (landscape, big font); the two buttons stay at the bottom.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Illustration: a peach circle with the pin on a grey-green card.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        color = if (dark) MaterialTheme.colorScheme.surface else NeutralCard,
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .background(if (dark) DarkSelectedTint else CarrotLight, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_location_big),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.location_explain_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 32.dp)
            )
            Text(
                text = stringResource(R.string.location_explain_body),
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                color = bodyColor,
                modifier = Modifier.padding(top = 14.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 18.dp)
            ) {
                CheckItem(R.string.location_explain_bullet_sorted, bodyColor)
                CheckItem(R.string.location_explain_bullet_map, bodyColor)
            }
        }

        Button(
            onClick = onAllow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .heightIn(min = 52.dp)
        ) {
            Text(stringResource(R.string.location_explain_allow), style = MaterialTheme.typography.labelLarge)
        }
        OutlinedButton(
            onClick = onChooseOwnLocation,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .heightIn(min = 52.dp)
        ) {
            Text(stringResource(R.string.location_explain_choose), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** A green check mark with a line of text. */
@Composable
private fun CheckItem(text: Int, color: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "✓",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Preview(showBackground = true, heightDp = 844, widthDp = 390)
@Composable
private fun LocationExplanationScreenPreview() {
    OSMTheme(darkTheme = false) {
        LocationExplanationScreen(onAllow = {}, onChooseOwnLocation = {})
    }
}
