package moe.apex.rule34.largeimageview


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import moe.apex.rule34.DeepLinkActivity
import moe.apex.rule34.MainActivity
import moe.apex.rule34.image.Image
import moe.apex.rule34.navigation.ImageView
import moe.apex.rule34.navigation.Results
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.util.CHIP_SPACING
import moe.apex.rule34.util.CombinedClickableSuggestionChip
import moe.apex.rule34.util.Heading
import moe.apex.rule34.util.LargeVerticalSpacer
import moe.apex.rule34.util.TitledModalBottomSheet
import moe.apex.rule34.util.copyText
import moe.apex.rule34.util.isWebLink
import moe.apex.rule34.util.pluralise


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InfoSheet(navController: NavController, image: Image, onDismissRequest: () -> Unit) {
    if (image.metadata == null) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state: SheetState? = null
    val clip = LocalClipboard.current
    var previousSheetValue by remember { mutableStateOf(SheetValue.Hidden) }

    fun hideAndThen(block: () -> Unit = { }) {
        scope.launch {
            state?.hide()
        }.invokeOnCompletion {
            onDismissRequest()
            block()
        }
    }

    fun chipClick(tag: String) {
        hideAndThen {
            /* Don't do new searches inside the DeepLinkActivity. We should only
               ever do them inside the main one. */
            if (context is DeepLinkActivity) {
                val intent = createSearchIntent(context, image.imageSource, tag)
                context.startActivity(intent)
            } else {
                navController.navigate(Results(image.imageSource, listOf(tag)))
            }
        }
    }

    fun chipLongClick(tag: String) {
        scope.launch {
            copyText(context, clip, tag)
        }
    }

    // We want to bypass the partially expanded state when closing but not when opening.
    state = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.PartiallyExpanded ) {
                if (previousSheetValue == SheetValue.Expanded) {
                    hideAndThen()
                    return@rememberModalBottomSheetState false
                } else {
                    previousSheetValue = newValue
                    return@rememberModalBottomSheetState true
                }
            }
            else {
                previousSheetValue = newValue
                return@rememberModalBottomSheetState true
            }
        }
    )

    /* The padding and window insets allow the content to draw behind the nav bar while ensuring
       the sheet doesn't expand to behind the status bar. */
    TitledModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = state,
        title = "About this image"
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {
            image.metadata.parentId?.let {
                TextButton(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = {
                        hideAndThen {
                            navController.navigate(ImageView(image.imageSource, it))
                        }
                    }
                ) {
                    Text("View parent image")
                }
            }
            if (image.metadata.hasChildren == true) {
                image.id?.let {
                    TextButton(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        onClick = {
                            hideAndThen {
                                if (context is DeepLinkActivity) {
                                    val intent = createSearchIntent(context, image.imageSource, "parent:$it")
                                    context.startActivity(intent)
                                } else {
                                    navController.navigate(Results(image.imageSource, listOf("parent:$it")))
                                }
                            }
                        }
                    ) {
                        Text("View related images")
                    }
                }
            }
            image.metadata.artist?.let {
                Heading(text = "Artist")
                CombinedClickableSuggestionChip(
                    modifier = Modifier.padding(start = 16.dp),
                    label = { Text(it) },
                    onClick = { chipClick(it) },
                    onLongClick = { chipLongClick(it) }
                )
                LargeVerticalSpacer()
            }
            image.metadata.source?.let {
                Heading(text = "Source")
                if (it.isWebLink()) PaddedUrlText(it) else PaddedText(it)
                LargeVerticalSpacer()
            }
            Heading(text = "Rating")
            PaddedText(image.metadata.rating.label)
            LargeVerticalSpacer()
            image.metadata.pixivUrl?.let {
                Heading(text = "Pixiv URL")
                PaddedUrlText(it)
                LargeVerticalSpacer()
            }
            image.metadata.groupedTags.map {
                Heading(text = it.category.label.pluralise(it.tags.size, it.category.pluralisedLabel))
                ContextualFlowRow(
                    itemCount = it.tags.size,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING.dp, Alignment.Start)
                ) { index ->
                    val tag = it.tags[index]
                    CombinedClickableSuggestionChip(
                        label = { Text(tag) },
                        onClick = { chipClick(tag) },
                        onLongClick = { chipLongClick(tag) }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() * 2))
        }
    }
}


@Composable
private fun PaddedText(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = text
    )
}


@Composable
private fun PaddedUrlText(text: String) {
    val annotatedString = buildAnnotatedString {
        withLink(LinkAnnotation.Url(
            url = text,
            styles = TextLinkStyles(SpanStyle(
                color = MaterialTheme.colorScheme.secondary,
                textDecoration = TextDecoration.Underline
            ))
        )) {
            append(text)
        }
    }

    SelectionContainer {
        Text(
            text = annotatedString,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}


private fun createSearchIntent(context: Context, imageSource: ImageSource, query: String): Intent {
    return createSearchIntent(context, imageSource, listOf(query))
}


private fun createSearchIntent(context: Context, imageSource: ImageSource, queries: List<String>): Intent {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.putExtra("source", imageSource.name)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.putExtra("query", queries.toTypedArray())
    intent.setComponent(
        ComponentName(
            context,
            MainActivity::class.java
        )
    )
    return intent
}
