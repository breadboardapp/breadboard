package moe.apex.rule34.largeimageview


import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.webkit.URLUtil.isValidUrl
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import moe.apex.rule34.image.Image
import moe.apex.rule34.util.Heading


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InfoSheet(image: Image, visibilityState: MutableState<Boolean>) {
    if (image.metadata == null) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state: SheetState? = null
    val clip = LocalClipboardManager.current
    var previousSheetValue by remember { mutableStateOf(SheetValue.Hidden) }

    // We want to bypass the partially expanded state when closing but not when opening.
    state = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.PartiallyExpanded ) {
                if (previousSheetValue == SheetValue.Expanded) {
                    scope.launch {
                        state?.hide()
                        visibilityState.value = false
                    }
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
    ModalBottomSheet(
        modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues()),
        onDismissRequest = { visibilityState.value = false },
        sheetState = state,
        contentWindowInsets = { BottomSheetDefaults.windowInsets.only(WindowInsetsSides.Horizontal) }
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            image.metadata.artist?.let {
                Heading(text = "Artist")
                PaddedText(it)
                Spacer(Modifier.height(24.dp))
            }
            image.metadata.source?.let {
                Heading(text = "Source")
                if (isValidUrl(it)) PaddedUrlText(it) else PaddedText(it)
                Spacer(Modifier.height(24.dp))
            }
            Heading(text = "Rating")
            PaddedText(image.metadata.rating.label)
            Spacer(Modifier.height(24.dp))
            image.metadata.pixivUrl?.let {
                Heading(text = "Pixiv URL")
                PaddedUrlText(it)
                Spacer(Modifier.height(24.dp))
            }
            Heading(text = "Tags")
            ContextualFlowRow(
                itemCount = image.metadata.tags.size,
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            ) { index ->
                val tag = image.metadata.tags[index]
                SuggestionChip(
                    onClick = {
                        clip.setText(AnnotatedString(tag))
                        if (SDK_INT < VERSION_CODES.TIRAMISU) { // Android 13 has its own text copied popup
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                    label = { Text(tag) }
                )
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

    Text(
        text = annotatedString,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
