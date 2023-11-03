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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import moe.apex.rule34.prefs
import moe.apex.rule34.ui.theme.ProcrasturbatingTheme
import moe.apex.rule34.util.SaveDirectorySelection


@Composable
fun Heading(
    modifier: Modifier = Modifier.padding(bottom=12.dp),
    text: String
) {
    Text(
        modifier = modifier,
        text = text,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 0.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun TitleSummary(
    modifier: Modifier = Modifier,
    title: String,
    summary: String
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title)
        Text(
            summary,
            color = Color.Gray,
            fontSize = 14.sp,
            lineHeight = 16.sp
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(navController: NavController) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val scope = rememberCoroutineScope()
    val prefs = LocalContext.current.prefs
    var dataSaver by remember { mutableStateOf(DataSaver.AUTO) }
    var storageLocation by remember { mutableStateOf(Uri.EMPTY) }
    val requester = remember { mutableStateOf(false) }


    ProcrasturbatingTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                LargeTopAppBar(
                    title = { Text("Settings") },
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.navigateUp() }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Home"
                            )
                        }
                    }
                )
            }
        ) {
            Column(
                Modifier
                    .padding(it)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
            ) {
                Heading(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
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
                        for (setting in DataSaver.values()) {
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
                            Divider()
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
                Spacer(Modifier.height(18.dp))
                Heading(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    text = "Downloads"
                )
                TitleSummary(
                    modifier = Modifier
                        .clickable(
                            onClick = { requester.value = true }
                        )
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .heightIn(min = 72.dp),
                    title = "Save downloads to",
                    summary = if (storageLocation == Uri.EMPTY) {
                        "Tap to set"
                    } else storageLocation.toString()
                )
                if (requester.value) {
                    SaveDirectorySelection(requester = requester)
                }
            }
        }

        LaunchedEffect(true) {
            prefs.getPreferences.collect {value ->
                dataSaver = value.dataSaver
                storageLocation = value.storageLocation
            }
        }
    }
}