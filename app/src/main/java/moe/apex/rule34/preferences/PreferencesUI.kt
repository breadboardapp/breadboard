package moe.apex.rule34.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.apex.rule34.ui.theme.BreadboardTheme
import moe.apex.rule34.ui.theme.prefTitle
import moe.apex.rule34.ui.theme.searchField
import moe.apex.rule34.util.Heading
import moe.apex.rule34.util.ListItemPosition
import moe.apex.rule34.util.VerticalSpacer


private const val DISABLED_OPACITY = 0.38f
private enum class ImportExport {
    IMPORT,
    EXPORT
}


@Composable
private fun ExpressivePreferenceContainer(
    position: ListItemPosition,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.padding(horizontal = 12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(
            topStart = position.topSize,
            topEnd = position.topSize,
            bottomStart = position.bottomSize,
            bottomEnd = position.bottomSize
        )
    ) {
        content()
    }
}


interface PreferencesGroupScope {
    fun item(content: @Composable () -> Unit)
}


private class PreferencesGroupScopeImpl : PreferencesGroupScope {
    val items = mutableListOf<@Composable () -> Unit>()

    override fun item(content: @Composable () -> Unit) {
        items.add(content)
    }
}


@Composable
fun PreferencesGroup(
    title: String,
    content: @Composable PreferencesGroupScope.() -> Unit
) {
    val scope = PreferencesGroupScopeImpl()
    scope.content()

    Heading(
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        text = title
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        scope.items.forEachIndexed { index, itemContent ->
            ExpressivePreferenceContainer(
                position = when {
                    scope.items.size == 1 -> ListItemPosition.SINGLE_ELEMENT
                    index == 0 -> ListItemPosition.TOP
                    index == scope.items.lastIndex -> ListItemPosition.BOTTOM
                    else -> ListItemPosition.MIDDLE
                }
            ) {
                itemContent()
            }
        }
    }
}


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
fun TitleSummary(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val baseModifier = modifier.heightIn(min = 76.dp)
    val finalModifier = onClick?.let { baseModifier.clickable(enabled) { it() } } ?: baseModifier

    Column(
        modifier = finalModifier,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.prefTitle,
            modifier = Modifier
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = (if (summary == null) 16.dp else 2.dp)
                )
                .alpha(if (enabled) 1f else DISABLED_OPACITY),
        )

        if (summary != null) {
            Summary(
                text = summary,
                modifier = Modifier
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                    .alpha(if (enabled) 1f else DISABLED_OPACITY)
            )
        }
    }
}


@Composable
fun SwitchPref(
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
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            modifier = Modifier.padding(end = 16.dp),
            colors = SwitchDefaults.colors().copy(
                uncheckedThumbColor = BreadboardTheme.colors.outlineStrong,
                uncheckedBorderColor = BreadboardTheme.colors.outlineStrong
            ),
            thumbContent = {
                Icon(
                    imageVector = if (checked) Icons.Default.Check else Icons.Default.Clear,
                    contentDescription = if (checked) "Enabled" else "Disabled",
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        )
    }
}


@Composable
fun EnumPref(
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
        summary = summary
    ) {
        showDialog = true
    }
}


@Composable
fun InfoSection(text: String) {
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


@Composable
fun ImportDialog(
    allowedCategories: List<PrefCategory>,
    onDismissRequest: () -> Unit,
    onConfirm: (List<PrefCategory>) -> Unit
) {
    val enabledCategories = allowedCategories.toMutableStateList()
    ImportExportDialog(
        type = ImportExport.IMPORT,
        enabledCategories = enabledCategories,
        allowedCategories = allowedCategories,
        onDismissRequest = onDismissRequest,
        onConfirm = { onConfirm(enabledCategories) }
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
    onConfirm: (List<PrefCategory>) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                enabled = enabledCategories.size != 1, // The BUILD category is always enabled but hidden
                onClick = { onConfirm(enabledCategories) }
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
            Column {
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
    obscured: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = LocalTextStyle.current.copy(fontSize = (LocalTextStyle.current.fontSize.value - 3).sp)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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


data class ToggleablePrefCategory(
    var available: Boolean = true,
    var enabled: Boolean,
    val category: PrefCategory,
    val onToggle: (Boolean) -> Unit
)
