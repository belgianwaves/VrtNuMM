package com.bw.vrtnumm.androidApp.ui.home

import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bw.vrtnumm.androidApp.R

@Composable
fun DetailsDrawer(
    drawerState: BottomDrawerState,
    heading: String,
    details: String,
    onPlayClicked: () -> Unit,
    bodyContent: @Composable () -> Unit
) {
    BottomDrawerLayout(
        gesturesEnabled = false,
        drawerState = drawerState,
        bodyContent = bodyContent,
        drawerContent = {
            ConstraintLayout(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                val (icon, title, text, button) = createRefs()

                Icon(
                    imageVector = Icons.Default.Close,
                    modifier = Modifier.constrainAs(icon) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    }.clickable(onClick = { drawerState.close() }),
                )

                Text(
                    text = heading,
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.constrainAs(title) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }.padding(bottom = 8.dp),
                )

                ScrollableColumn(
                    modifier = Modifier.constrainAs(text) {
                        top.linkTo(title.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(button.top)
                        height = Dimension.fillToConstraints
                        width = Dimension.fillToConstraints
                    }
                ) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.subtitle1
                    )
                }

                Button(modifier = Modifier.constrainAs(button) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }.padding(top = 8.dp), onClick = { drawerState.close(); onPlayClicked() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PlayArrow)
                        Text(
                            text = stringResource(R.string.action_play)
                        )
                    }
                }
            }
        }
    )
}