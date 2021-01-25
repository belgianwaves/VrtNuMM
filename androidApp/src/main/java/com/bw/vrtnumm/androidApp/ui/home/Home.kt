package com.bw.vrtnumm.androidApp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.TabConstants.defaultTabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bw.vrtnumm.androidApp.R
import com.bw.vrtnumm.androidApp.ui.home.discover.Discover
import com.bw.vrtnumm.androidApp.ui.home.library.Library
import com.bw.vrtnumm.androidApp.ui.home.live.Live
import com.bw.vrtnumm.androidApp.ui.home.search.Search
import com.bw.vrtnumm.shared.db.Program
import com.bw.vrtnumm.shared.transport.Channel
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Composable
fun Home(
    viewModel: HomeViewModel,
    onProgramSelected: (Program) -> Unit,
    onChannelSelected: (Channel) -> Unit
) {
    val viewState by viewModel.state.collectAsState()

    Surface(Modifier.fillMaxSize()) {
        HomeContent(
            homeCategories = viewState.homeCategories,
            selectedHomeCategory = viewState.selectedHomeCategory,
            onCategorySelected = viewModel::onHomeCategorySelected,
            modifier = Modifier.fillMaxSize(),
            onProgramSelected = onProgramSelected,
            onChannelSelected = onChannelSelected
        )
    }
}

@ExperimentalTime
@Composable
fun HomeContent(
    selectedHomeCategory: HomeCategory,
    homeCategories: List<HomeCategory>,
    modifier: Modifier = Modifier,
    onCategorySelected: (HomeCategory) -> Unit,
    onProgramSelected: (Program) -> Unit,
    onChannelSelected: (Channel) -> Unit
) {
    Column(modifier = modifier) {
        if (homeCategories.isNotEmpty()) {
            HomeCategoryTabs(
                categories = homeCategories,
                selectedCategory = selectedHomeCategory,
                onCategorySelected = onCategorySelected
            )
        }

        when (selectedHomeCategory) {
            HomeCategory.Library -> {
                Library(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    onProgramSelected = onProgramSelected
                )
            }
            HomeCategory.Discover -> {
                Discover(Modifier.fillMaxWidth().weight(1f), onProgramSelected)
            }
            HomeCategory.Live -> {
                Live(Modifier.fillMaxWidth().weight(1f), onChannelSelected)
            }
            HomeCategory.Search -> {
                Search(Modifier.fillMaxWidth().weight(1f), onProgramSelected)
            }
        }
    }
}

@Composable
private fun HomeCategoryTabs(
    categories: List<HomeCategory>,
    selectedCategory: HomeCategory,
    onCategorySelected: (HomeCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = categories.indexOfFirst { it == selectedCategory }
    val indicator = @Composable { tabPositions: List<TabPosition> ->
        HomeCategoryTabIndicator(
            Modifier.defaultTabIndicatorOffset(tabPositions[selectedIndex])
        )
    }

    TabRow(
        selectedTabIndex = selectedIndex,
        indicator = indicator,
        modifier = modifier
    ) {
        categories.forEachIndexed { index, category ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(
                        text = when (category) {
                            HomeCategory.Library -> stringResource(R.string.home_library)
                            HomeCategory.Discover -> stringResource(R.string.home_discover)
                            HomeCategory.Live -> stringResource(R.string.home_live)
                            HomeCategory.Search -> stringResource(R.string.home_search)
                        },
                        style = MaterialTheme.typography.subtitle2
                    )
                },
                modifier = Modifier.preferredHeight(40.dp)
            )
        }
    }
}

@Composable
fun HomeCategoryTabIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary
) {
    Spacer(
        modifier.padding(horizontal = 24.dp)
            .preferredHeight(4.dp)
            .background(color, RoundedCornerShape(topLeftPercent = 100, topRightPercent = 100))
    )
}