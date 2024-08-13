package moe.apex.rule34.preferences

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import moe.apex.rule34.prefs
import moe.apex.rule34.ui.theme.ProcrasturbatingTheme
import moe.apex.rule34.util.SaveDirectorySelection
import moe.apex.rule34.util.TitleBar


@Composable
private fun Heading(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        modifier = modifier.padding(horizontal = 16.dp),
        text = text,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 0.sp,
        fontWeight = FontWeight.Medium
    )
}


@Composable
private fun TitleSummary(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp)
        )
        if (summary != null) {
            Text(
                summary,
                color = Color.Gray,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(navController: NavController) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val scope = rememberCoroutineScope()
    val storageLocationPromptLaunched = remember { mutableStateOf(false) }

    val prefs = LocalContext.current.prefs
    val currentSettings by prefs.getPreferences.collectAsState(Prefs.DEFAULT)
    val dataSaver = currentSettings.dataSaver
    val storageLocation = currentSettings.storageLocation
    val excludeAi = currentSettings.excludeAi

    ProcrasturbatingTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TitleBar(
                    title = "Settings",
                    scrollBehavior = scrollBehavior,
                    navController = navController
                )
            }
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(it)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(12.dp))
                Heading(
                    modifier = Modifier.padding(bottom = 12.dp),
                    text = "Data saver"
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(IntrinsicSize.Min)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            DividerDefaults.Thickness,
                            DividerDefaults.color,
                            RoundedCornerShape(24.dp)
                        ),
                    tonalElevation = 2.dp
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        for (setting in DataSaver.entries) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .selectable(
                                        selected = dataSaver == setting,
                                        onClick = { scope.launch { prefs.updateDataSaver(setting) } },
                                        role = Role.RadioButton
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    selected = dataSaver == setting,
                                    onClick = null
                                )
                                Text(text = setting.description)
                            }
                            HorizontalDivider()
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "When enabled, data saver will load lower resolution images by default. " +
                               "Downloads will always be in the maximum quality.",
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        color = Color.Gray
                    )
                }

                Spacer(Modifier.height(24.dp))

                Heading(text = "Downloads")
                TitleSummary(
                    modifier = Modifier
                        .clickable { storageLocationPromptLaunched.value = true }
                        .fillMaxWidth(),
                    title = "Save downloads to",
                    summary = if (storageLocation == Uri.EMPTY) "Tap to set"
                    else storageLocation.toString()
                )
                if (storageLocationPromptLaunched.value) {
                    SaveDirectorySelection(storageLocationPromptLaunched)
                }

                Spacer(Modifier.height(24.dp))

                Heading(text = "Searching")
                SwitchPref(
                    checked = excludeAi,
                    title = "Hide AI-generated images",
                    summary = "Attempt to remove AI-generated images by excluding the " +
                              "'ai_generated' tag in search queries by default."
                ) {
                    scope.launch { prefs.updateExcludeAi(it) }
                }
            }
        }
    }
}