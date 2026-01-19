package moe.apex.rule34.preferences

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.apex.rule34.ui.theme.BreadboardTheme
import moe.apex.rule34.ui.theme.prefTitle
import moe.apex.rule34.ui.theme.searchField
import moe.apex.rule34.util.BasicExpressiveContainer
import moe.apex.rule34.util.ExpressiveContainer
import moe.apex.rule34.util.ListItemPosition
import moe.apex.rule34.util.MEDIUM_SPACER
import moe.apex.rule34.util.SMALL_LARGE_SPACER
import moe.apex.rule34.util.SMALL_SPACER
import moe.apex.rule34.util.SmallVerticalSpacer
import moe.apex.rule34.util.Summary
import moe.apex.rule34.util.TitleSummary
import moe.apex.rule34.util.TitledModalBottomSheet
import moe.apex.rule34.util.VerticalSpacer
import moe.apex.rule34.util.largerShape
import moe.apex.rule34.util.navBarHeight
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt


private enum class ImportExport {
    IMPORT,
    EXPORT
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoButton(
    title: String,
    text: String,
) {
    var showInfoSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showInfoSheet) {
        TitledModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            sheetState = sheetState,
            title = title
        ) {
            ExpressiveContainer(
                modifier = Modifier.padding(bottom = navBarHeight * 2),
                position = ListItemPosition.SINGLE_ELEMENT) {
                Summary(Modifier.padding(SMALL_LARGE_SPACER.dp), text)
            }
        }
    }
    IconButton(
        onClick = { showInfoSheet = true },
        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = "What's this?"
        )
    }
}


@Composable
fun SwitchPref(
    checked: Boolean,
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    infoText: String? = null,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled) { onToggle(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TitleSummary(
            modifier = Modifier.weight(1f),
            title = title,
            summary = summary,
            enabled = enabled,
            trailingIcon = infoText?.let {
                { InfoButton(title, it) }
            }
        )
        Switch(
            enabled = enabled,
            checked = checked,
            onCheckedChange = onToggle,
            modifier = Modifier.padding(end = SMALL_LARGE_SPACER.dp),
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = BreadboardTheme.colors.outlineStrong,
                uncheckedBorderColor = BreadboardTheme.colors.outlineStrong,
                checkedIconColor = MaterialTheme.colorScheme.primary
                /* Switches essentially have 3 "layers". The defaults are:
                   - Track: primary
                   - Thumb: onPrimary
                   - Icon:  onPrimaryContainer
                   Android 16 QPR1 can provide a dark onPrimaryContainer in dark mode
                   which has poor contrast with the onPrimary thumb it is contained within.
                   We'll work around this by using the primary colour for the icon.
                   It looks good anyway. */
            ),
            thumbContent = {
                Icon(
                    imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Clear,
                    contentDescription = if (checked) "Enabled" else "Disabled",
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        )
    }
}


@Composable
fun <T: PrefEnum<*>> EnumPref(
    title: String,
    summary: String?,
    infoText: String? = null,
    enumItems: Collection<T>,
    selectedItem: T,
    onSelection: (T) -> Unit
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
                                modifier = Modifier.padding(start = MEDIUM_SPACER.dp, end = SMALL_LARGE_SPACER.dp),
                                selected = selectedItem == setting,
                                onClick = null
                            )
                            Text(text = setting.label)
                        }
                    }
                }
            }
        )
    }

    TitleSummary(
        modifier = Modifier.fillMaxWidth(),
        title = title,
        summary = summary,
        trailingIcon = infoText?.let {
            { InfoButton(title, it) }
        }
    ) {
        showDialog = true
    }
}


@Composable
fun SliderPref(
    title: String? = null,
    label: ((Float) -> String)? = { it.roundToInt().toString() },
    initialValue: Float,
    displayValueRange: ClosedFloatingPointRange<Float>,
    allowedValueRange: ClosedFloatingPointRange<Float> = displayValueRange,
    onValueChangeFinished: (Float) -> Unit,
) {
    var sliderValue by remember { mutableFloatStateOf(initialValue) }
    Column(
        modifier = Modifier.padding(SMALL_LARGE_SPACER.dp)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.prefTitle
            )
            Spacer(Modifier.height(SMALL_SPACER.dp))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SMALL_SPACER.dp)
        ) {
            Slider(
                modifier = Modifier.weight(1f),
                value = sliderValue,
                valueRange = displayValueRange,
                onValueChange = {
                    sliderValue = it.coerceIn(allowedValueRange)
                },
                onValueChangeFinished = { onValueChangeFinished(sliderValue) }
            )
            if (label != null) {
                Text(
                    text = label(sliderValue),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.width(48.dp)
                )
            }
        }
    }
}


@Composable
fun SliderPref(
    title: String? = null,
    label: ((Float) -> String)? = { it.roundToInt().toString() },
    initialValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChangeFinished: (Float) -> Unit,
) {
    SliderPref(
        title = title,
        label = label,
        initialValue = initialValue,
        displayValueRange = valueRange,
        allowedValueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished
    )
}


@Composable
fun InfoSection(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SMALL_LARGE_SPACER.dp),
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = Icons.Outlined.Info, // Outlined looks better than (filled) rounded here.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        VerticalSpacer()
        Summary(text = text)
    }
}


@Composable
fun ImportDialog(
    allowedCategories: List<PrefCategory>,
    onDismissRequest: () -> Unit,
    onConfirm: (List<PrefCategory>, Boolean) -> Unit
) {
    val enabledCategories = remember { allowedCategories.toMutableStateList() }
    var merge by remember { mutableStateOf(false) }

    ImportExportDialog(
        type = ImportExport.IMPORT,
        enabledCategories = enabledCategories,
        allowedCategories = allowedCategories,
        onDismissRequest = onDismissRequest,
        merge = merge,
        onMergeToggle = { merge = it },
        onConfirm = { onConfirm(enabledCategories, merge) }
    )
}


@Composable
fun ExportDialog(
    enabledCategories: MutableList<PrefCategory>,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    ImportExportDialog(
        type = ImportExport.EXPORT,
        enabledCategories = enabledCategories,
        allowedCategories = PrefCategory.entries,
        onDismissRequest = onDismissRequest,
        onConfirm = { onConfirm() }
    )
}


@Composable
private fun ImportExportDialog(
    type: ImportExport,
    enabledCategories: MutableList<PrefCategory>,
    allowedCategories: List<PrefCategory>,
    onDismissRequest: () -> Unit,
    merge: Boolean = false,
    onMergeToggle: (Boolean) -> Unit = { },
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                enabled = enabledCategories.size != 1, // The BUILD category is always enabled but hidden
                onClick = onConfirm
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
        title = { Text("Select categories to ${if (type == ImportExport.IMPORT) "import" else "export"}" ) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                for (category in PrefCategory.entries.filter { it != PrefCategory.BUILD }) {
                    // Always export BUILD category (version code)
                    CheckboxSelectable(
                        toggleablePrefCategory = ToggleablePrefCategory(
                            available = category in allowedCategories,
                            enabled = enabledCategories.contains(category),
                            category = category,
                            onToggle = {
                                if (it) enabledCategories.add(category)
                                else enabledCategories.remove(category)
                            }
                        )
                    )
                }
                if (type == ImportExport.IMPORT) {
                    SmallVerticalSpacer()
                    /* This allowedToMerge is only UI-side as it only serves to make the user
                       more aware of what can and can't be merged.
                       We'll still pass in the unmodified `merge` value even if `allowedToMerge` is
                       false. But that doesn't matter because it'll just get ignored if there are
                       no mergeable prefs enabled for import. */
                    val allowedToMerge by remember { derivedStateOf {
                        PrefCategory.SETTING in enabledCategories ||
                                PrefCategory.FAVOURITE_IMAGES in enabledCategories
                    } }
                    BasicExpressiveContainer(position = ListItemPosition.SINGLE_ELEMENT) {
                        SwitchPref(
                            checked = merge && allowedToMerge,
                            title = "Merge data",
                            summary = "Merge incoming favourites and tags with current data " +
                                      "rather than overwriting.",
                            enabled = allowedToMerge,
                            onToggle = onMergeToggle
                        )
                    }
                }
            }
        }
    )
}


@Composable
private fun CheckboxSelectable(toggleablePrefCategory: ToggleablePrefCategory) {
    val baseModifier = Modifier
        .clip(RoundedCornerShape(100))
        .fillMaxWidth()
        .semantics { role = Role.Checkbox }

    Row(
        modifier = if (toggleablePrefCategory.available) {
            baseModifier.clickable { toggleablePrefCategory.onToggle(!toggleablePrefCategory.enabled) }
        } else {
            baseModifier.alpha(0.44f)
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            enabled = toggleablePrefCategory.available,
            checked = toggleablePrefCategory.enabled,
            onCheckedChange = toggleablePrefCategory.onToggle
        )
        Text(
            text = toggleablePrefCategory.category.label
        )
    }
}


@Composable
fun PreferenceTextBox(
    value: String,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    autoCorrectEnabled: Boolean = false,
    obscured: Boolean = false,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = LocalTextStyle.current.copy(fontSize = (LocalTextStyle.current.fontSize.value - 3).sp)) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, autoCorrectEnabled = autoCorrectEnabled),
        visualTransformation = if (obscured) {
            VisualTransformation { input ->
                TransformedText(
                    text = AnnotatedString("•".repeat(input.length)),
                    offsetMapping = OffsetMapping.Identity
                )
            }
        } else VisualTransformation.None,
        textStyle = MaterialTheme.typography.searchField
    )
}


@Composable
fun <T: PrefEnum<*>> ReorderablePref(
    title: String,
    dialogTitle: String? = null,
    summary: String?,
    items: List<T>,
    onReorder: (List<T>) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    val hapticFeedback = LocalHapticFeedback.current
    val list = items.toMutableStateList()
    val listState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(listState) { from, to ->
        list.add(to.index, list.removeAt(from.index))
        hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        onReorder(list)
    }

    if (showDialog) {
        AlertDialog(
            title = { Text(dialogTitle ?: title) },
            confirmButton = {
                TextButton({ showDialog = false }) {
                    Text("Close")
                }
            },
            onDismissRequest = { showDialog = false },
            text = {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.clip(largerShape)
                ) {
                    items(list, { it.label}) {
                        ReorderableItem(reorderableLazyListState, key = it.label) { isDragging ->
                            val color by animateColorAsState(
                                targetValue = if (list.indexOf(it) == 0) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else if (isDragging) {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            )
                            Surface(
                                color = color,
                                modifier = Modifier.clip(largerShape)
                            ) {
                                Row(
                                    modifier = Modifier.height(64.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    it.enabledIcon?.let {
                                        Icon(
                                            imageVector = it,
                                            contentDescription = null,
                                            modifier = Modifier.padding(horizontal = SMALL_LARGE_SPACER.dp)
                                        )
                                    }
                                    Text(
                                        text = it.label,
                                        modifier = Modifier.weight(1f, true)
                                    )
                                    IconButton(
                                        modifier = Modifier
                                            .padding(horizontal = SMALL_SPACER.dp)
                                            .draggableHandle(
                                                onDragStarted = {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.GestureThresholdActivate
                                                    )
                                                },
                                                onDragStopped = {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.GestureEnd
                                                    )
                                                },
                                        ),
                                        onClick = { },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DragHandle,
                                            contentDescription = "Reorder"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    TitleSummary(
        modifier = Modifier.fillMaxWidth(),
        title = title,
        summary = summary
    ) {
        showDialog = true
    }
}


data class ToggleablePrefCategory(
    var available: Boolean = true,
    var enabled: Boolean,
    val category: PrefCategory,
    val onToggle: (Boolean) -> Unit
)
