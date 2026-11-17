package be.ap.laatsteronde.ui.merchant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import be.ap.laatsteronde.R

/** Temporary start screen of a merchant: only a text and a logout button. */
@Composable
fun MerchantHomeScreen(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.merchant_home_placeholder),
            textAlign = TextAlign.Center
        )
        Button(onClick = onLogout, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.merchant_logout))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MerchantHomeScreenPreview() {
    MerchantHomeScreen(onLogout = {})
}
