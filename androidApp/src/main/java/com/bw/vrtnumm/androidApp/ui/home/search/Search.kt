package com.bw.vrtnumm.androidApp.ui.home.search

import android.app.Activity
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AmbientTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.viewModel
import com.bw.vrtnumm.androidApp.ui.home.category.ProgramItem
import com.bw.vrtnumm.shared.db.Program

@Composable
fun Search(
    modifier: Modifier,
    onProgramSelected: (Program) -> Unit
) {
    val viewModel: SearchViewModel = viewModel()
    val viewState by viewModel.state.collectAsState()

    val context = AmbientContext.current

    Column() {
        var query by remember { mutableStateOf<String>("") }

        val textStyle = AmbientTextStyle.current.copy(color = MaterialTheme.colors.onSurface)
        TextField(value = query, maxLines = 1, textStyle = textStyle, onValueChange = { query = it; viewModel.onQueryChanged(it) }, label = { Text("query") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.preferredHeight(8.dp))

        LazyColumn() {
            items(viewState.programs) { program ->
                ProgramItem(
                    program = program,
                    onProgramSelected = { onProgramSelected(program); hideKeyboard(context as Activity) })
            }
        }
    }
}

private fun hideKeyboard(activity: Activity) {
    val imm: InputMethodManager = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    var view = activity.currentFocus
    if (view != null) {
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}