package com.bw.vrtnumm.androidApp.ui.home.category

import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.viewModel
import com.bw.vrtnumm.androidApp.ui.home.DetailsDrawer
import com.bw.vrtnumm.androidApp.utils.*
import com.bw.vrtnumm.shared.db.Program

@Composable
fun ProgramCategory(
    category: String,
    modifier: Modifier,
    onProgramSelected: (Program) -> Unit
) {
    val viewModel: ProgramCategoryViewModel = viewModel(
        key = "category_list_$category",
        factory = viewModelProviderFactoryOf { ProgramCategoryViewModel(category) }
    )

    val viewState by viewModel.state.collectAsState()

    val drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)
    var details by remember { mutableStateOf<Program?>(null) }

    DetailsDrawer(
        drawerState = drawerState,
        heading = details?.title ?: "",
        details = details?.desc?.escHtml() ?: "", onPlayClicked = { details?.run { onProgramSelected(this) } }) {

        val width = Metrics.widthInDip(AmbientContext.current)
        if (width > 800) {
            ScrollableColumn() {
                StaggeredVerticalGrid(maxColumnWidth = (width / 2).dp) {
                    viewState.programs.forEach { program ->
                        ProgramItem(program = program, onProgramSelected = onProgramSelected, onMoreClicked = { p -> details = p; drawerState.open() })
                    }
                }
            }
        } else {
            LazyColumn() {
                items(viewState.programs) { program ->
                    ProgramItem(
                        program = program,
                        onProgramSelected = onProgramSelected,
                        onMoreClicked = { p -> details = p; drawerState.open() })
                }
            }
        }
    }

    onCommit(category) {
        if (drawerState.isOpen) drawerState.close()
    }
}

@Composable
fun ProgramItem(
    program: Program,
    onProgramSelected: (Program) -> Unit,
    onMoreClicked: ((Program) -> Unit)? = null
) {
    ListItem(
        icon = {
            VideoThumbnail(url = program.thumbnail)
        },
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
            if (onMoreClicked != null) {
                Icon(imageVector = Icons.Default.MoreVert, modifier = Modifier.clickable(onClick = { onMoreClicked(program) } ))
            }
        },
        modifier = Modifier.clickable(onClick = { onProgramSelected(program) })
    )
}