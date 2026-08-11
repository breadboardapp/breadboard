package moe.apex.breadboard.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moe.apex.breadboard.BuildConfig


private data class WhatsNewEntry(
    val title: String,
    val summary: String,
    // val imageRes: Int? = null,
    // val additionalContent: (LazyListScope.() -> Unit)? = null
)


private val followingFeed = WhatsNewEntry(
    title = "Following feed",
    summary = "With Breadboard 3.3 you can now follow your favourite artists!\n\n" +
              "Your following feed lives in the Browse tab and is the perfect place to keep " +
              "up with your favourite artists' work."
)


private val artistProfiles = WhatsNewEntry(
    title = "Artist profiles",
    summary = "Breadboard's new artist profiles are a beautiful and convenient way to " +
              "learn about the creators behind your favourite art.\n\n" +
              "An artist's profile shows their other names, social and support links, " +
              "and their most popular posts, plus lets you follow them and share their link.\n\n" +
              "Tap an artist's tag in the info sheet to visit their profile!"
)


private val sauceNao = WhatsNewEntry(
    title = "SauceNAO integration",
    summary = "Breadboard now supports reverse image search with SauceNAO.\n\n" +
              "Got an image saved that you don't know the artist for? " +
              "Just upload it on Breadboard's new SauceNAO tab and find out everything " +
              "you need to know!"
)


private val generalImprovements = WhatsNewEntry(
    title = "And more",
    summary = "Improved Settings organisation, a fresh font, bug fixes, and more."
)


private val whatsNewEntries = listOf(followingFeed, artistProfiles, sauceNao, generalImprovements)


object WhatsNewState {
    private var _visible = MutableStateFlow(false)
    val visible = _visible.asStateFlow()

    fun show() = _visible.update { true }
    fun hide() = _visible.update { false }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WhatsNewBottomSheet(state: SheetState, onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()

    TitledModalBottomSheet(
        title = "What's new",
        sheetState = state,
        onDismissRequest = onDismissRequest
    ) {
        Box {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = MEDIUM_SPACER.dp,
                    end = MEDIUM_SPACER.dp,
                    bottom = navBarHeight + MEDIUM_SPACER.dp + 48.dp + SMALL_LARGE_SPACER.dp
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    MediumEmphasisCenteredLabel(
                        text = "Breadboard has been updated to version ${BuildConfig.VERSION_NAME}."
                    )
                }

                for (entry in whatsNewEntries) {
                    LazyExpressiveGroup {
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(SMALL_SPACER.dp),
                                modifier = Modifier.padding(SMALL_LARGE_SPACER.dp)
                            ) {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontSize = 20.sp,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Summary(text = entry.summary)
                            }
                        }
                    }
                }
            }
            Button(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = navBarHeight + MEDIUM_SPACER.dp,
                        start = SMALL_LARGE_SPACER.dp,
                        end = SMALL_LARGE_SPACER.dp
                    )
                    .fillMaxWidth(),
                onClick = {
                    scope.launch {
                        state.hide()
                    }.invokeOnCompletion {
                        onDismissRequest()
                    }
                },
                shapes = ButtonDefaults.shapes()
            ) {
                Text("Let's go!")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNew() {
    val stateVisible by WhatsNewState.visible.collectAsStateWithLifecycle()
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    /* The migration can set stateVisible to true before composition has even started,
       which results in the sheet being visible by default and therefore not having an animation.
       This extra layer avoids that by ensuring its only set after composition. */
    var visible by remember { mutableStateOf(false) }

    SideEffect(stateVisible) {
        visible = stateVisible
    }

    if (visible) {
        LaunchedEffect(Unit) {
            sheetState.expand()
        }

        WhatsNewBottomSheet(sheetState) {
            WhatsNewState.hide()
        }
    }
}
