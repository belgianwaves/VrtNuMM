package com.bw.vrtnumm.androidApp.ui.home.discover

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.viewModel
import com.bw.vrtnumm.androidApp.ui.home.ItemsTabs
import com.bw.vrtnumm.androidApp.ui.home.category.ProgramCategory
import com.bw.vrtnumm.shared.db.Program

@Composable
fun Discover(
    modifier: Modifier = Modifier,
    onProgramSelected: (Program) -> Unit
) {
    val viewModel: DiscoverViewModel = viewModel()
    val viewState by viewModel.state.collectAsState()

    val selectedCategory = viewState.selectedCategory

    if (viewState.categories.isNotEmpty() && selectedCategory != null) {
        Column(modifier) {
            Spacer(Modifier.preferredHeight(8.dp))

            ItemsTabs(
                items = viewState.categories.toSet(),
                selectedItem = selectedCategory,
                onItemSelected = viewModel::onCategorySelected,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.preferredHeight(8.dp))

            Crossfade(current = selectedCategory, animation = tween(500)) {
                ProgramCategory(it, Modifier.fillMaxSize(), onProgramSelected)
            }
        }
    } else {
    }
}




