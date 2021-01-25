package com.bw.vrtnumm.androidApp.ui.home.program

import androidx.compose.animation.animate
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieAnimationSpec
import com.airbnb.lottie.compose.rememberLottieAnimationState
import com.bw.vrtnumm.androidApp.R
import com.bw.vrtnumm.androidApp.ui.home.DetailsDrawer
import com.bw.vrtnumm.androidApp.ui.home.ItemsTabs
import com.bw.vrtnumm.androidApp.utils.*
import com.bw.vrtnumm.shared.db.Episode

@Composable
fun Program(
    modifier: Modifier,
    programUrl: String,
    onEpisodeSelected: (Episode) -> Unit,
    onBackPressed: () -> Unit
) {
    val viewModel: ProgramViewModel = viewModel(
        key = "programUrl_list_$programUrl",
        factory = viewModelProviderFactoryOf { ProgramViewModel(programUrl) }
    )

    val viewState by viewModel.state.collectAsState()

    val program = viewState.program
    val selectedEpisode = viewState.selectedEpisode
    val selectedSeason = viewState.selectedSeason

    var previousSelectedEpisode by remember { mutableStateOf<Episode?>(null) }

    if ((program != null) && (selectedEpisode != null) && (selectedSeason != null)) {
        if (previousSelectedEpisode == null) {
            onEpisodeSelected(selectedEpisode)
        }

        val singleEpisode = (viewState.episodes.size == 1) && (viewState.seasons.size == 1)

        val closure = { episode: Episode -> onEpisodeSelected(episode); viewModel.onEpisodeSelected(episode) }

        Column(verticalArrangement = Arrangement.Top, modifier = modifier) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, top = 16.dp)) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    modifier = Modifier.clickable(onClick = onBackPressed).padding(end = 8.dp)
                )

                Text(
                    text = program.title,
                    style = MaterialTheme.typography.h5
                )

                val favourite = program.favourite

                val tint = animate(
                    if (favourite) {
                        MaterialTheme.colors.primary
                    } else {
                        MaterialTheme.colors.onSurface.copy(alpha = AmbientContentAlpha.current)
                    }
                )

                Icon(
                    tint = tint,
                    imageVector = if (favourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    modifier = Modifier.padding(start = 8.dp).clickable(onClick = {
                        viewModel.toggleFavourite(program)
                    })
                )
            }
            if (!singleEpisode) {
                Text(
                    text = selectedEpisode.title,
                    modifier = Modifier.padding(start = 16.dp),
                    style = MaterialTheme.typography.subtitle1
                )

                Spacer(Modifier.preferredHeight(4.dp))

                ItemsTabs(
                    items = viewState.seasons,
                    selectedItem = selectedSeason,
                    onItemSelected = viewModel::onSeasonSelected,
                    modifier = Modifier.fillMaxWidth()
                )

                val drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)
                var details by remember { mutableStateOf(selectedEpisode) }

                Spacer(Modifier.preferredHeight(4.dp))

                DetailsDrawer(
                    drawerState = drawerState,
                    heading = getTitleOrShortDesc(details),
                    details = details.desc.escHtml(), onPlayClicked = { closure(details) }
                ) {
                    val width = Metrics.widthInDip(AmbientContext.current)
                    if (width > 800) {
                        ScrollableColumn() {
                            StaggeredVerticalGrid(maxColumnWidth = (width / 2).dp) {
                                viewState.episodes.forEach { episode ->
                                    EpisodeItem(
                                        episode = episode,
                                        selected = (episode == selectedEpisode),
                                        onEpisodeSelected = closure,
                                        onMoreClicked = { e ->
                                            details = e
                                            drawerState.open()
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = Math.max(0, viewState.episodes.indexOf(selectedEpisode)))
                        LazyColumn(state = lazyListState) {
                            items(viewState.episodes) { episode ->
                                EpisodeItem(
                                    episode = episode,
                                    selected = (episode == selectedEpisode),
                                    onEpisodeSelected = closure,
                                    onMoreClicked = { e ->
                                        details = e
                                        drawerState.open()
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.preferredHeight(4.dp))

                ScrollableColumn() {
                    Text(
                        text = program.desc.escHtml(),
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.subtitle1
                    )
                }
            }
        }
    } else {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Loader()
        }
    }

    onCommit(selectedEpisode) {
        previousSelectedEpisode = selectedEpisode
    }
}

@Composable
fun Loader() {
    val animationSpec = remember { LottieAnimationSpec.RawRes(R.raw.preloader) }
    val animationState = rememberLottieAnimationState(autoPlay = true, repeatCount = Integer.MAX_VALUE)

    LottieAnimation(
        animationSpec,
        animationState,
        modifier = Modifier.preferredSize(50.dp)
    )
}

@Composable
private fun EpisodeItem(
    episode: Episode,
    selected: Boolean,
    onEpisodeSelected: (Episode) -> Unit,
    onMoreClicked: (Episode) -> Unit
) {
    ListItem(
        icon = {
            VideoThumbnail(url = episode.videoThumbnailUrl, selected = selected)
        },
        text = {
            val color = animate(
                if (selected) {
                    MaterialTheme.colors.primary
                } else {
                    MaterialTheme.colors.onSurface.copy(alpha = AmbientContentAlpha.current)
                }
            )
            Text(
                getTitleOrShortDesc(episode), maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = color
            )
        },
        secondaryText = {
            Text(
                episode.desc.escHtml(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailing = {
            Icon(imageVector = Icons.Default.MoreVert, modifier = Modifier.clickable(onClick = { onMoreClicked(episode) } ))
        },
        modifier = Modifier.clickable(onClick = { onEpisodeSelected(episode) })
    )
}

private fun getTitleOrShortDesc(episode: Episode): String = if (episode.title.toLowerCase().contains("aflevering")) episode.shortDescription else episode.title