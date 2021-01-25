package com.bw.vrtnumm.androidApp.ui.home.library

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.animate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.doubleTapGestureFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.viewModel
import com.bw.vrtnumm.androidApp.BuildConfig
import com.bw.vrtnumm.androidApp.PlayerController
import com.bw.vrtnumm.androidApp.R
import com.bw.vrtnumm.androidApp.utils.Metrics
import com.bw.vrtnumm.androidApp.utils.VideoThumbnail
import com.bw.vrtnumm.androidApp.utils.escHtml
import com.bw.vrtnumm.androidApp.utils.sanitizedUrl
import com.bw.vrtnumm.shared.db.Program
import com.bw.vrtnumm.shared.transport.PlayerInfo
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import dev.chrisbanes.accompanist.coil.CoilImage
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Composable
fun Library(
    modifier: Modifier = Modifier,
    onProgramSelected: (Program) -> Unit
) {
    val viewModel: LibraryViewModel = viewModel()
    val viewState by viewModel.state.collectAsState()

    val isTablet = Metrics.isTablet(AmbientContext.current)

    LazyColumn(Modifier.padding(8.dp)) {
        if (viewState.watched.isNotEmpty()) {
            item {
                Column() {
                    Spacer(Modifier.preferredHeight(16.dp))
                    Text(stringResource(id = R.string.library_watched), style = MaterialTheme.typography.h6)

                    LazyRow() {
                        items(viewState.watched) { program ->
                            ProgramItem(program = program, onProgramSelected = onProgramSelected)
                        }
                    }
                }
            }
        }

        if (viewState.favourites.isNotEmpty()) {
            item {
                Column() {
                    Spacer(Modifier.preferredHeight(16.dp))
                    Text(stringResource(id = R.string.library_favourites), style = MaterialTheme.typography.h6)

                    LazyRow() {
                        items(viewState.favourites) { program ->
                            ProgramItem(program = program, onProgramSelected = onProgramSelected)
                        }
                    }
                }
            }
        }

        if (viewState.recent.isNotEmpty()) {
            item {
                Spacer(Modifier.preferredHeight(16.dp))
                Text(stringResource(id = R.string.library_new), style = MaterialTheme.typography.h6)
                Spacer(Modifier.preferredHeight(8.dp))
            }
            if (isTablet) {
                item {
                    LazyRow() {
                        items(viewState.recent) { program ->
                            ProgramItem(program = program, onProgramSelected = onProgramSelected)
                        }
                    }
                }
            } else {
                items(viewState.recent) { program ->
                    Column() {
                        CardItem(
                            program = program,
                            onProgramSelected = onProgramSelected,
                            onFavouriteToggled = { program -> viewModel.toggleFavourite(program) },
                            viewModel = viewModel
                        )
                        Spacer(Modifier.preferredHeight(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramItem(
    program: Program,
    onProgramSelected: (Program) -> Unit
) {
    Column(
        modifier = Modifier.padding(8.dp).clickable(onClick = { onProgramSelected(program) })
    ) {
        val width = VideoThumbnail(url = program.thumbnail)
        Text(program.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.preferredWidth(width.dp))
    }
}

@ExperimentalTime
@Composable
private fun CardItem(
    program: Program,
    onProgramSelected: (Program) -> Unit,
    onFavouriteToggled: (Program) -> Unit,
    viewModel: LibraryViewModel
) {
    var playerInfo by remember() { mutableStateOf<PlayerInfo?>(null) }

    Card(Modifier.clickable(onClick = { onProgramSelected(program) }).doubleTapGestureFilter { onFavouriteToggled(program) }) {
       Column() {
           val width = Metrics.widthInDip(AmbientContext.current)
           val height = (9*width)/16

           Box(modifier = Modifier.preferredHeight(height.dp).preferredWidth(width.dp)) {
               val pi = playerInfo
               if (pi == null)
                   CoilImage(
                       data = program.thumbnail.sanitizedUrl(640),
                       contentScale = ContentScale.Crop,
                       modifier = Modifier.fillMaxSize()
                   )
               else
                   VideoPreview(pi)
               Row() {
//                   program.categories.forEach { c ->
//                       ChoiceChipContent(text = c.name, modifier = Modifier.padding(start = 8.dp, top = 8.dp))
//                   }
//                   TODO reintroduce categories in programs
               }
           }

           ListItem(
               text = {
                   Text(program.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
               },
               secondaryText = {
                   Text(
                       program.desc.escHtml(),
                       maxLines = 3,
                       overflow = TextOverflow.Ellipsis
                   )
               },
               trailing = {
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
                       modifier = Modifier.clickable(onClick = {
                           onFavouriteToggled(program)
                       })
                   )
               },
           )
       }
    }

    onActive {
        if (BuildConfig.VIDEO_PREVIEWS) {
            viewModel.getPlayerInfo(program) { info ->
                playerInfo = info
            }
        }
    }

    onDispose {
    }
}

@Composable
fun VideoPreview(
    playerInfo: PlayerInfo
) {
    val context = AmbientContext.current
    val controller = remember {
        PlayerController(context).apply {
            prepare(playerInfo)
        }
    }

    AndroidView(viewBlock = {
        PlayerView(context).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            controller.setPlayerView(this)
            controller.mute()
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    })

    onDispose {
        controller.dispose()
    }
}