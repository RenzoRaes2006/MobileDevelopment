package be.ap.laatsteronde.ui.merchant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import be.ap.laatsteronde.R
import java.util.Calendar
import java.util.Locale

/**
 * A pick-up time ("Van" or "Tot") on "Nieuw pakket". It looks like the text field of the Figma ("18:00"),
 * but a tap opens a clock to choose the hour and minutes, so the merchant cannot type a wrong time.
 * [value] is "HH:mm" or empty; the chosen time goes to [onTimeChosen] as "HH:mm".
 */
@OptIn(ExperimentalMaterial3Api::class) // TimePicker is still marked experimental in Material 3 1.3
@Composable
internal fun PickupTimeField(
    value: String,
    label: String,
    isError: Boolean,
    onTimeChosen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // rememberSaveable: an open clock stays open when the screen turns.
    var showClock by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val description = stringResource(
        R.string.package_time_field_description,
        label,
        value.ifEmpty { stringResource(R.string.package_time_placeholder) }
    )

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(fieldBorder(isError, MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.error), shape)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button) { showClock = true }
            .padding(start = 12.dp, end = 14.dp)
    ) {
        Text(
            text = value.ifEmpty { stringResource(R.string.package_time_placeholder) },
            style = MaterialTheme.typography.bodyLarge,
            color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_down),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp).align(Alignment.CenterEnd)
        )
    }

    if (showClock) {
        val (startHour, startMinute) = startTimeFor(value)
        // The state of the clock survives a rotation (it has its own saver).
        val clock = rememberTimePickerState(initialHour = startHour, initialMinute = startMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showClock = false },
            title = { Text(label) },
            text = { TimePicker(state = clock, colors = laatsteRondeClockColors()) },
            confirmButton = {
                TextButton(onClick = {
                    showClock = false
                    onTimeChosen("%02d:%02d".format(Locale.ROOT, clock.hour, clock.minute))
                }) { Text(stringResource(R.string.package_time_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showClock = false }) { Text(stringResource(R.string.package_time_cancel)) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/** Where the clock starts: the time already chosen, or else the next full hour (a window usually starts soon). */
private fun startTimeFor(value: String): Pair<Int, Int> {
    val parts = normalizeTime(value)?.split(":")
    if (parts != null) return parts[0].toInt() to parts[1].toInt()
    val nextHour = (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1) % 24
    return nextHour to 0
}

/** The colors of the clock in the house style. The default colors of Material are purple-tinted. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun laatsteRondeClockColors() = TimePickerDefaults.colors(
    clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
    clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
    clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
    selectorColor = MaterialTheme.colorScheme.primary,
    containerColor = MaterialTheme.colorScheme.surface,
    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
)
