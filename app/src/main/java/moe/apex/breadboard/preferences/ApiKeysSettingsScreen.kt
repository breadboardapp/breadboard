package moe.apex.breadboard.preferences

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import moe.apex.breadboard.image.ImageBoardRequirement
import moe.apex.breadboard.prefs
import moe.apex.breadboard.util.LargeTitleBar
import moe.apex.breadboard.util.LazyExpressiveGroup
import moe.apex.breadboard.util.MainScreenScaffold
import moe.apex.breadboard.util.MEDIUM_SPACER
import moe.apex.breadboard.util.SMALL_SPACER
import moe.apex.breadboard.util.TINY_SPACER
import moe.apex.breadboard.util.TitleSummary
import moe.apex.breadboard.util.launchInWebBrowser


private enum class ApiKeyField(val label: String, val hide: Boolean) {
    USER("User ID/name", false),
    KEY("API key", true)
}


private enum class ApiKeyDialogType(val fields: List<ApiKeyField>) {
    SIMPLE(listOf(ApiKeyField.KEY)),
    FULL(listOf(ApiKeyField.USER, ApiKeyField.KEY))
}


private data class ApiKeyDialogData(
    val type: ApiKeyDialogType,
    val defaultValues: Map<ApiKeyField, String>,
    val credentialsUrl: String?,
    val onDismissRequest: () -> Unit,
    val onSubmit: (Map<ApiKeyField, String>) -> Unit
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeysSettingsScreen(navController: NavHostController) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val scope = rememberCoroutineScope()

    var apiKeyDialogData: ApiKeyDialogData? by remember { mutableStateOf(null) }

    val preferencesRepository = LocalContext.current.prefs
    val currentSettings = LocalPreferences.current

    if (apiKeyDialogData != null) {
        FlexibleAuthDialog(apiKeyDialogData!!)
    }

    MainScreenScaffold(
        topAppBar = {
            LargeTitleBar(
                title = "API keys",
                scrollBehavior = scrollBehavior,
                navController = navController
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(MEDIUM_SPACER.dp)
        ) {
            LazyExpressiveGroup(
                title = "Image sources",
                desiredTopPadding = null
            ) {
                for (imageBoard in ImageSource.entries) {
                    item {
                        val requirement = imageBoard.imageBoard.apiKeyRequirement
                        val auth = currentSettings.imageBoardAuths[imageBoard]

                        TitleSummary(
                            title = imageBoard.label,
                            summary = when (requirement) {
                                ImageBoardRequirement.NOT_NEEDED -> "${imageBoard.label} does not require an API key."
                                ImageBoardRequirement.RECOMMENDED -> "${imageBoard.label} provides a better experience with an API key. " +
                                        if (auth == null) "Tap to set." else ""
                                ImageBoardRequirement.REQUIRED -> "${imageBoard.label} requires an API key. " +
                                        if (auth == null) "Tap to set." else ""
                            },
                            enabled = requirement != ImageBoardRequirement.NOT_NEEDED,
                            trailingIcon = if (imageBoard == currentSettings.imageSource) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = "This is your current image source.",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else null,
                            onClick = {
                                apiKeyDialogData = ApiKeyDialogData(
                                    type = ApiKeyDialogType.FULL,
                                    defaultValues = mapOf(
                                        ApiKeyField.USER to (auth?.user ?: ""),
                                        ApiKeyField.KEY to (auth?.apiKey ?: "")
                                    ),
                                    credentialsUrl = imageBoard.imageBoard.apiKeyCreationUrl,
                                    onDismissRequest = { apiKeyDialogData = null },
                                    onSubmit = { input ->
                                        scope.launch {
                                            preferencesRepository.setAuth(
                                                source = imageBoard,
                                                username = input[ApiKeyField.USER],
                                                apiKey = input[ApiKeyField.KEY]
                                            )
                                        }.invokeOnCompletion {
                                            apiKeyDialogData = null
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }

            LazyExpressiveGroup("SauceNAO") {
                item {
                    TitleSummary(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Set SauceNAO API key",
                        summary = if (currentSettings.saucenaoApiKey.isNotEmpty()) {
                            "API key is set."
                        } else {
                            "An API key is required to use reverse image search with SauceNAO. " +
                            "Tap to set."
                        }
                    ) {
                        apiKeyDialogData = ApiKeyDialogData(
                            type = ApiKeyDialogType.SIMPLE,
                            defaultValues = mapOf(ApiKeyField.KEY to currentSettings.saucenaoApiKey),
                            credentialsUrl = "https://saucenao.com/user.php?page=search-api",
                            onDismissRequest = { apiKeyDialogData = null },
                            onSubmit = { input ->
                                scope.launch {
                                    preferencesRepository.updatePref(
                                        PreferenceKeys.SAUCENAO_API_KEY,
                                        input[ApiKeyField.KEY] ?: ""
                                    )
                                }.invokeOnCompletion {
                                    apiKeyDialogData = null
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun FlexibleAuthDialog(data: ApiKeyDialogData) {
    var inQueryParamMode by remember { mutableStateOf(false) }
    val queryRegex = remember { Regex("&api_key=([^&]+)&user_id=(\\d+)") }
    var queryString by remember { mutableStateOf("") }
    var isQueryStringValid by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val userInput = remember { data.type.fields.map { it to (data.defaultValues[it] ?: "") }.toMutableStateMap() }

    AlertDialog(
        onDismissRequest = data.onDismissRequest,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Set API key")

                // Only allow query param entry on full type
                if (data.type == ApiKeyDialogType.FULL) {
                    Box {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = MaterialTheme.shapes.small
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (inQueryParamMode) "Enter credentials normally" else "Enter query string") },
                                onClick = {
                                    inQueryParamMode = !inQueryParamMode
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column {
                AnimatedContent(inQueryParamMode) {
                    if (it) {
                        PreferenceTextBox(
                            value = queryString,
                            label = "Query string",
                            obscured = false,
                            keyboardType = KeyboardType.Uri,
                            isError = !isQueryStringValid && queryString.isNotEmpty()
                        ) { newValue ->
                            queryString = newValue.trim()
                            isQueryStringValid = queryRegex.containsMatchIn(queryString)
                        }
                    } else {
                        if (data.type == ApiKeyDialogType.SIMPLE) {
                            PreferenceTextBox(
                                value = userInput[ApiKeyField.KEY] ?: "",
                                label = ApiKeyField.KEY.label,
                                obscured = ApiKeyField.KEY.hide
                            ) {
                                userInput[ApiKeyField.KEY] = it.trim()
                            }
                        } else {
                            Column {
                                for (entry in data.type.fields) {
                                    PreferenceTextBox(
                                        value = userInput[entry] ?: "",
                                        label = entry.label,
                                        obscured = entry.hide
                                    ) {
                                        userInput[entry] = it.trim()
                                    }
                                }
                            }
                        }
                    }
                }

                data.credentialsUrl?.let { url ->
                    val apiKeyCreationText = buildAnnotatedString {
                        val link = LinkAnnotation.Url(
                            url,
                            TextLinkStyles(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.secondary,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        ) {
                            launchInWebBrowser(context, url)
                        }

                        withLink(link) {
                            append("Find your credentials...")
                        }
                    }

                    Text(
                        text = apiKeyCreationText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(
                            start = TINY_SPACER.dp,
                            top = SMALL_SPACER.dp
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = if (inQueryParamMode) {
                    isQueryStringValid || queryString.isBlank()
                } else {
                    userInput.values.all { it.isEmpty() } || userInput.values.none { it.isBlank() }
                },
                onClick = {
                    if (inQueryParamMode) {
                        if (queryString.isBlank()) {
                            data.onSubmit(emptyMap())
                            return@Button
                        }

                        val match = queryRegex.find(queryString)
                        if (match != null) {
                            data.onSubmit(
                                mapOf(
                                    ApiKeyField.USER to match.groupValues[2],
                                    ApiKeyField.KEY to match.groupValues[1]
                                )
                            )
                        }
                    } else {
                        data.onSubmit(userInput)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = data.onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
