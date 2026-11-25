package be.ap.laatsteronde.ui.merchant

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.ap.laatsteronde.R
import be.ap.laatsteronde.data.ShopCategory
import be.ap.laatsteronde.ui.theme.TextBody
import be.ap.laatsteronde.ui.theme.isDarkTheme

/** The name of a category as the merchant sees it. */
@StringRes
internal fun ShopCategory.labelRes(): Int = when (this) {
    ShopCategory.BAKER -> R.string.shop_category_baker
    ShopCategory.SUPERMARKET -> R.string.shop_category_supermarket
    ShopCategory.RESTAURANT -> R.string.shop_category_restaurant
}

private fun fieldBorder(isError: Boolean, outline: androidx.compose.ui.graphics.Color, error: androidx.compose.ui.graphics.Color) =
    BorderStroke(if (isError) 2.dp else 1.dp, if (isError) error else outline)

/**
 * A text field as in the Figma form: a white box with a thin border and the text inside, with
 * the label above it (that is a separate [Text] in the screen, so [label] is only for screen readers).
 * [height], [radius] and [textSize] differ between the big fields and the ones in the address card.
 * With [multiline] the box is at least [height] high and grows with the text (the description of a package).
 */
@Composable
internal fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    radius: Dp = 12.dp,
    textSize: Int = 16,
    isError: Boolean = false,
    enabled: Boolean = true,
    multiline: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = !multiline,
        maxLines = if (multiline) MULTILINE_MAX_LINES else 1,
        textStyle = TextStyle(
            fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
            fontSize = textSize.sp,
            color = textColor
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = keyboardOptions,
        modifier = modifier
            .fillMaxWidth()
            .then(if (multiline) Modifier.heightIn(min = height) else Modifier.height(height))
            .semantics { contentDescription = label },
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = if (multiline) Alignment.TopStart else Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (multiline) Modifier.heightIn(min = height) else Modifier.height(height))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(radius))
                    .border(
                        fieldBorder(isError, MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.error),
                        RoundedCornerShape(radius)
                    )
                    .padding(horizontal = 12.dp, vertical = if (multiline) 14.dp else 0.dp)
            ) {
                innerTextField()
            }
        }
    )
}

/** A multiline field grows with its text, up to this many lines; after that it scrolls. */
private const val MULTILINE_MAX_LINES = 6

/** The small label above a field. [size] is 14 for the big fields and 12 for the address card (a softer color there). */
@Composable
internal fun FieldLabel(@StringRes text: Int, modifier: Modifier = Modifier, size: Int = 14) {
    val color = if (size == 14 || isDarkTheme()) MaterialTheme.colorScheme.onSurface else TextBody
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.titleSmall.copy(fontSize = size.sp),
        color = color,
        modifier = modifier.padding(bottom = if (size == 14) 6.dp else 4.dp)
    )
}

/** The dropdown "Categorie": looks like a field with a chevron and opens a menu with the categories. */
@Composable
internal fun CategoryDropdown(
    selected: ShopCategory?,
    onSelect: (ShopCategory) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    val label = stringResource(R.string.shop_label_category)
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(
                    fieldBorder(isError, MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.error),
                    RoundedCornerShape(12.dp)
                )
                .semantics { contentDescription = label }
                .clickable(role = Role.DropdownList) { open = true }
                .padding(start = 12.dp, end = 17.dp)
        ) {
            Text(
                text = stringResource(selected?.labelRes() ?: R.string.shop_category_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 28.dp)
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.CenterEnd)
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ShopCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(stringResource(category.labelRes())) },
                    onClick = {
                        open = false
                        onSelect(category)
                    }
                )
            }
        }
    }
}

/** Column that puts a label above a field. */
@Composable
internal fun LabeledField(
    @StringRes label: Int,
    modifier: Modifier = Modifier,
    size: Int = 14,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        FieldLabel(label, size = size)
        content()
    }
}
