package moe.apex.rule34.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.rememberLibraries
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.mikepenz.aboutlibraries.ui.compose.util.strippedLicenseContent
import moe.apex.rule34.R
import moe.apex.rule34.util.ExpressiveContainer
import moe.apex.rule34.util.LargeTitleBar
import moe.apex.rule34.util.ListItemPosition
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.MEDIUM_SPACER
import moe.apex.rule34.util.TitleSummary


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrariesScreen(navController: NavHostController) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val libraries by rememberLibraries(R.raw.aboutlibraries)
    val actualLibraries = libraries?.libraries ?: emptyList()

    MainScreenScaffold(
        topAppBar = {
            LargeTitleBar(
                title = "Third-party notices",
                scrollBehavior = scrollBehavior,
                navController = navController
            )
        }
    ) {
        var selectedLibrary by remember { mutableStateOf<Library?>(null) }
        if (selectedLibrary != null) {
            AlertDialog(
                modifier = Modifier.heightIn(max = 600.dp),
                onDismissRequest = { selectedLibrary = null },
                title = {
                    Text(
                        text = selectedLibrary!!.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                text = {
                    Column {
                        HorizontalDivider()
                        LazyColumn(contentPadding = PaddingValues(vertical = MEDIUM_SPACER.dp)) {
                            item {
                                Text(selectedLibrary!!.strippedLicenseContent.takeIf { it.isNotEmpty() }
                                    ?: "No license text.")
                            }
                        }
                        HorizontalDivider()
                    }
                },
                confirmButton = { TextButton({ selectedLibrary = null }) { Text("Close") } }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(vertical = MEDIUM_SPACER.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(actualLibraries.size, key = { it }) { index ->
                val library = actualLibraries[index]
                ExpressiveContainer(
                    modifier = Modifier.animateItem(),
                    position = when (index) {
                        0 -> ListItemPosition.TOP
                        actualLibraries.lastIndex -> ListItemPosition.BOTTOM
                        else -> ListItemPosition.MIDDLE
                    }
                ) {
                    TitleSummary(
                        modifier = Modifier.fillMaxWidth(),
                        title = library.name,
                        summary = "Version ${library.artifactVersion}, by ${library.author}\n" +
                                  library.licenses.joinToString { license -> license.name },
                        onClick = { selectedLibrary = library }
                    )
                }
            }
        }
    }
}