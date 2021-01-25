package com.bw.vrtnumm.androidApp.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.emptyContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bw.vrtnumm.androidApp.ui.theme.Keyline1

private val emptyTabIndicator: @Composable (List<TabPosition>) -> Unit = {}

@Composable
fun ItemsTabs(
    items: Set<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = items.indexOfFirst { it == selectedItem }
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        divider = emptyContent(), /* Disable the built-in divider */
        edgePadding = Keyline1,
        indicator = emptyTabIndicator,
        modifier = modifier
    ) {
        items.forEachIndexed { index, item ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onItemSelected(item) }
            ) {
                ChoiceChipContent(
                    text = item,
                    selected = index == selectedIndex,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ChoiceChipContent(
    text: String,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = when {
            selected -> MaterialTheme.colors.primary.copy(alpha = 0.08f)
            else -> Color.Transparent
        },
        contentColor = when {
            selected -> MaterialTheme.colors.primary
            else -> MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
        },
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            width = 1.dp,
            color = when {
                selected -> MaterialTheme.colors.primary
                else -> MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
            }
        ),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle2,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}