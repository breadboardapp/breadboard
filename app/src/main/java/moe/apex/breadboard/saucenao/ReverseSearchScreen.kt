package moe.apex.breadboard.saucenao

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.apex.breadboard.navigation.ApiKeysSettings
import moe.apex.breadboard.navigation.SauceNaoResults
import moe.apex.breadboard.preferences.LocalPreferences
import moe.apex.breadboard.ui.theme.searchField
import moe.apex.breadboard.util.BasicExpressiveContainer
import moe.apex.breadboard.util.FeaturedImageTitleSummary
import moe.apex.breadboard.util.ListItemPosition
import moe.apex.breadboard.util.SMALL_SPACER
import moe.apex.breadboard.util.TINY_SPACER
import moe.apex.breadboard.util.SMALL_LARGE_SPACER
import moe.apex.breadboard.util.MainScreenScaffold
import moe.apex.breadboard.util.SmallVerticalSpacer
import moe.apex.breadboard.util.isWebLink
import moe.apex.breadboard.util.largerShape
import moe.apex.breadboard.util.showToast


@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReverseSearchScreen(
    navController: NavController,
    initialImageUrl: String? = null,
    initialFileUri: String? = null
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val prefs = LocalPreferences.current
    val scope = rememberCoroutineScope()

    var imageUrl by rememberSaveable { mutableStateOf(initialImageUrl ?: "") }
    var selectedFileUri by rememberSaveable { mutableStateOf(initialFileUri?.toUri()) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            imageUrl = "" // We'll just clear the text field when selecting an image
        }
    }

    var debouncedImageUrl by remember { mutableStateOf(imageUrl) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isEmpty()) {
            debouncedImageUrl = ""
        } else {
            delay(500)
            debouncedImageUrl = imageUrl
        }
    }

    val isUrlValid = remember(debouncedImageUrl) { debouncedImageUrl.isWebLink() }
    var isUrlPreviewLoaded by remember(debouncedImageUrl) { mutableStateOf(false) }

    if (isUrlValid) {
        // Mostly intended so we can still preview Gelbooru URLs. TODO: Test if they actually work
        val headersBuilder = remember {
            NetworkHeaders.Builder()
                .set("Referer", "https://${debouncedImageUrl.toUri().host}")
                .build()
        }
        val model = remember { ImageRequest.Builder(context)
            .data(debouncedImageUrl)
            .httpHeaders(headersBuilder)
            .crossfade(true)
            .build()
        }
        AsyncImage(
            model = model,
            contentDescription = null,
            onSuccess = { isUrlPreviewLoaded = true },
            onError = { isUrlPreviewLoaded = false }
        )
    }

    fun onSearch() {
        if (prefs.saucenaoApiKey.isEmpty()) {
            navController.navigate(ApiKeysSettings)
            showToast(context, "Set a SauceNAO API key first.")
            return
        }

        if (imageUrl.isNotEmpty()) {
            navController.navigate(SauceNaoResults(imageUrl = imageUrl))
        } else if (selectedFileUri != null) {
            navController.navigate(SauceNaoResults(fileUri = selectedFileUri.toString()))
        } else {
            showToast(context, "Enter a URL or select an image")
        }
    }

    MainScreenScaffold(
        title = "SauceNAO"
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = SMALL_LARGE_SPACER.dp)
                .fillMaxWidth()
        ) {
            // URL input field
            TextField(
                value = imageUrl,
                onValueChange = {
                    imageUrl = it
                    if (it.isNotEmpty()) selectedFileUri = null
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.searchField,
                placeholder = {
                    Text(
                        text = "Enter an image URL",
                        style = MaterialTheme.typography.searchField
                    )
                },
                singleLine = true,
                shape = CircleShape,
                colors = TextFieldDefaults.colors().copy(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright
                ),
                prefix = { Spacer(Modifier.width(TINY_SPACER.dp)) },
                trailingIcon = {
                    Row(Modifier.padding(end = SMALL_SPACER.dp)) {
                        if (imageUrl.isNotEmpty()) {
                            IconButton(
                                onClick = { imageUrl = "" }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear URL"
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val clipboardText =
                                            clipboard.nativeClipboard.primaryClip?.getItemAt(
                                                0
                                            )?.text?.toString()
                                        if (clipboardText != null) {
                                            imageUrl = clipboardText
                                            selectedFileUri = null
                                        } else {
                                            showToast(context, "Nothing on the clipboard")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentPaste,
                                    contentDescription = "Paste from clipboard"
                                )
                            }
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch() }
                )
            )

            SmallVerticalSpacer()

            Surface(
                shape = largerShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(Modifier.padding(SMALL_LARGE_SPACER.dp)) {
                    Text(
                        text = "or choose from your device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    SmallVerticalSpacer()

                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.width(SMALL_SPACER.dp))
                        Text("Select image")
                    }

                    Spacer(Modifier.height(TINY_SPACER.dp))

                    Button(
                        onClick = { onSearch() },
                        modifier = Modifier.fillMaxWidth(),
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Search")
                    }
                }
            }

            // Thumbnail preview of selected file or URL
            val showPreview = selectedFileUri != null || (isUrlValid && isUrlPreviewLoaded)
            val currentPreviewModel = selectedFileUri ?: (if (isUrlValid && isUrlPreviewLoaded) debouncedImageUrl else null)

            // Store the last valid preview state so it doesn't disappear before the exit animation
            var lastPreviewModel by remember { mutableStateOf<Any?>(null) }
            var lastIsFile by remember { mutableStateOf(false) }

            if (currentPreviewModel != null) {
                lastPreviewModel = currentPreviewModel
                lastIsFile = selectedFileUri != null
            }

            AnimatedVisibility(
                visible = showPreview,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                lastPreviewModel?.let { model ->
                    BasicExpressiveContainer(
                        position = ListItemPosition.SINGLE_ELEMENT,
                        modifier = Modifier.padding(top = SMALL_SPACER.dp)
                    ) {
                        FeaturedImageTitleSummary(
                            featuredImage = {
                                AsyncImage(
                                    model = model,
                                    contentDescription = if (lastIsFile) "Selected image" else "Image URL preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.clip(MaterialTheme.shapes.medium)
                                )
                            },
                            summary = "This image will be sent to SauceNAO.\n" +
                                      "Refer to SauceNAO's privacy policy to understand how your " +
                                      "data is used.",
                            trailingIcon = if (lastIsFile) {
                                {
                                    IconButton(
                                        onClick = { selectedFileUri = null }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Clear,
                                            contentDescription = "Remove image",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
