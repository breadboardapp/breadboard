package moe.apex.rule34.preferences

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import moe.apex.rule34.prefs
import moe.apex.rule34.ui.theme.BreadboardTheme
import moe.apex.rule34.util.VerticalSpacer
import moe.apex.rule34.util.Heading
import moe.apex.rule34.util.LargeVerticalSpacer
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.NavBarHeightVerticalSpacer
import moe.apex.rule34.util.SaveDirectorySelection


@Composable
private fun Summary(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        text = text,
        color = Color.Gray,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier
    )
}


@Composable
private fun TitleSummary(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null
) {
    Column(
        modifier = modifier.heightIn(min = 72.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = (if (summary == null) 14.dp else 2.dp))
        )

        if (summary != null) {
            Summary(
                text = summary,
                modifier = Modifier.padding(bottom = 14.dp, start = 16.dp, end = 16.dp)
            )
        }
    }
}


@Composable
private fun SwitchPref(
    checked: Boolean,
    title: String,
    summary: String? = null,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TitleSummary(Modifier.weight(1f), title, summary)
        Switch(checked, onToggle, Modifier.padding(end = 16.dp))
    }
}


@Composable
private fun EnumPref(
    title: String,
    summary: String?,
    enumItems: Array<PrefEnum<*>>,
    selectedItem: PrefEnum<*>,
    onSelection: (PrefEnum<*>) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = { },
            title = { Text(title) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    for (setting in enumItems) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(100))
                                .selectable(
                                    selected = selectedItem == setting,
                                    onClick = {
                                        onSelection(setting)
                                        showDialog = false
                                    },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                modifier = Modifier.padding(start = 12.dp, end = 16.dp),
                                selected = selectedItem == setting,
                                onClick = null
                            )
                            Text(text = setting.description)
                        }
                    }
                }
            }
        )
    }

    TitleSummary(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        title = title,
        summary = summary
    )
}


@Composable
private fun InfoSection(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = Color.Gray
        )
        VerticalSpacer()
        Summary(text = text)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen() {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val scope = rememberCoroutineScope()
    val storageLocationPromptLaunched = remember { mutableStateOf(false) }
    val preferencesRepository = LocalContext.current.prefs
    val currentSettings = LocalPreferences.current

    BreadboardTheme {
        MainScreenScaffold("Settings", scrollBehavior) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(it)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
            ) {
                VerticalSpacer()

                Heading(text = "Data saver")
                EnumPref(
                    title = "Data saver",
                    summary = currentSettings.dataSaver.description,
                    enumItems = DataSaver.entries.toTypedArray(),
                    selectedItem = currentSettings.dataSaver,
                    onSelection = { scope.launch { preferencesRepository.updateDataSaver(it as DataSaver) } }
                )

                LargeVerticalSpacer()

                Heading(text = "Downloads")
                TitleSummary(
                    modifier = Modifier
                        .clickable { storageLocationPromptLaunched.value = true }
                        .fillMaxWidth(),
                    title = "Save downloads to",
                    summary = if (currentSettings.storageLocation == Uri.EMPTY) "Tap to set"
                    else currentSettings.storageLocation.toString()
                )
                if (storageLocationPromptLaunched.value) {
                    SaveDirectorySelection(storageLocationPromptLaunched)
                }

                LargeVerticalSpacer()

                Heading(text = "Searching")
                EnumPref(
                    title = "Image source",
                    summary = currentSettings.imageSource.description,
                    enumItems = ImageSource.entries.toTypedArray(),
                    selectedItem = currentSettings.imageSource,
                    onSelection = { scope.launch { preferencesRepository.updateImageSource(it as ImageSource) } }
                )
                SwitchPref(
                    checked = currentSettings.excludeAi,
                    title = "Hide AI-generated images",
                    summary = "Attempt to remove AI-generated images by excluding the " +
                              "'ai_generated' tag in search queries by default."
                ) {
                    scope.launch { preferencesRepository.updateExcludeAi(it) }
                }

                HorizontalDivider(Modifier.padding(vertical = 48.dp))

                InfoSection(text = "When data saver is enabled, images will load in a lower resolution " +
                                   "by default. Downloads will always be in the maximum resolution.")
                AnimatedVisibility(currentSettings.imageSource == ImageSource.DANBOORU) {
                    Column {
                        LargeVerticalSpacer()
                        InfoSection(
                            text = "Danbooru limits searches to 2 tags (which includes ratings), " +
                                    "so filtering by rating is not possible. Remember to enable all " +
                                    "ratings if you're using Danbooru, or consider using a different " +
                                    "source."
                        )
                    }
                }
                NavBarHeightVerticalSpacer()
            }
        }
    }
}