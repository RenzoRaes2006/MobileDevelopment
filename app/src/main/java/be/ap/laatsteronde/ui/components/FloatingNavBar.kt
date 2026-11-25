package be.ap.laatsteronde.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.ap.laatsteronde.ui.theme.CarrotOrange
import be.ap.laatsteronde.ui.theme.DarkNavBarBackground
import be.ap.laatsteronde.ui.theme.NavBarBackground
import be.ap.laatsteronde.ui.theme.NavBarInactive
import be.ap.laatsteronde.ui.theme.isDarkTheme

/** One tab of the [FloatingNavBar]: a label and an icon. */
data class NavBarItem(@StringRes val label: Int, @DrawableRes val icon: Int)

/**
 * The dark pill with tabs (Figma "FloatingNavBar") of the customer (four tabs) and of the merchant
 * (two tabs). The chosen tab is an orange pill. The width comes from the caller through [modifier].
 */
@Composable
fun FloatingNavBar(
    items: List<NavBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val barColor = if (isDarkTheme()) DarkNavBarBackground else NavBarBackground
    Row(
        modifier = modifier
            .height(68.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(34.dp))
            .background(barColor, RoundedCornerShape(34.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .selectableGroup()
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            val contentColor = if (isSelected) NavBarBackground else NavBarInactive
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(
                        color = if (isSelected) CarrotOrange else Color.Transparent,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) })
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(item.label),
                        color = contentColor,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
