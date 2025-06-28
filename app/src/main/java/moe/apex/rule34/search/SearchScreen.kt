package moe.apex.rule34.search

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.apex.rule34.R
import moe.apex.rule34.history.SearchHistoryEntry
import moe.apex.rule34.image.ImageBoardLocalFilterType
import moe.apex.rule34.image.ImageRating
import moe.apex.rule34.navigation.Results
import moe.apex.rule34.preferences.ImageSource
import moe.apex.rule34.preferences.LocalPreferences
import moe.apex.rule34.preferences.PreferenceKeys
import moe.apex.rule34.prefs
import moe.apex.rule34.tag.TagSuggestion
import moe.apex.rule34.ui.theme.searchField
import moe.apex.rule34.util.CHIP_SPACING
import moe.apex.rule34.util.HorizontallyScrollingChipsWithLabels
import moe.apex.rule34.util.ListItemPosition
import moe.apex.rule34.util.MainScreenScaffold
import moe.apex.rule34.util.BOTTOM_APP_BAR_HEIGHT
import moe.apex.rule34.util.NavBarHeightVerticalSpacer
import moe.apex.rule34.util.SearchHistoryListItem
import moe.apex.rule34.util.ExpressiveTagEntryContainer
import moe.apex.rule34.util.TitledModalBottomSheet
import moe.apex.rule34.util.VerticalSpacer
import moe.apex.rule34.util.availableRatingsForCurrentSource
import moe.apex.rule34.util.availableRatingsForSource
import moe.apex.rule34.util.copyText
import moe.apex.rule34.util.largerShape
import moe.apex.rule34.util.pluralise
import moe.apex.rule34.util.showToast
import moe.apex.rule34.viewmodel.BreadboardViewModel
import moe.apex.rule34.viewmodel.getIndexByName
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SearchScreen(navController: NavController, focusRequester: FocusRequester, viewModel: BreadboardViewModel) {
    /* We use shouldShowSuggestions for determining autocomplete section visibility because if we
       used mostRecentSuggestions.isNotEmpty(), it would temporarily show the "No results" message
       while disappearing and that looks bad. */
    val tagChipList = viewModel.tagSuggestions
    var shouldShowSuggestions by remember { mutableStateOf(false) }
    var searchString by remember { mutableStateOf("") }
    var cleanedSearchString by remember { mutableStateOf("") }
    val mostRecentSuggestions = remember { mutableStateListOf<TagSuggestion>() }

    var showSearchHistoryPopup by rememberSaveable { mutableStateOf(false) }
    var showSourceChangeDialog by remember { mutableStateOf(false) }
    var sourceChangeDialogData by remember { mutableStateOf<SourceDialogData?>(null) }
    var showSourceRatingBox by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (showSourceRatingBox) 180f else 0f)

    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val prefs = LocalPreferences.current
    val currentSource = prefs.imageSource

    var searchJob: Job? = null
    val scope = rememberCoroutineScope()


    fun addToFilter(tag: TagSuggestion) {
        val index = tagChipList.getIndexByName(tag.value)

        if (index == null) {
            tagChipList.add(tag)
        } else {
            // For some reason `tagChipList[index] = tag` doesn't update in the UI
            tagChipList.removeAt(index)
            tagChipList.add(index, tag)
        }
    }


    fun danbooruLimitCheck(): Boolean {
        if (
            tagChipList.size > 2 &&
            prefs.imageSource == ImageSource.DANBOORU &&
            prefs.authFor(ImageSource.DANBOORU) == null
        ) {
            showToast(context, "Danbooru only supports up to 2 tags without an API key")
            return false
        }
        return true
    }


    fun getSuggestions(bypassDelay: Boolean = false, source: ImageSource = currentSource) {
        searchJob?.cancel()
        searchJob = scope.launch(Dispatchers.IO) {
            if (cleanedSearchString.isNotEmpty()) delay(if (bypassDelay) 0 else 200)
            if (cleanedSearchString !in listOf("", "-")) {
                try {
                    val suggestions = source.imageBoard.loadAutoComplete(cleanedSearchString)
                    /* This check shouldn't be needed but avoids a race condition whereby clearing
                       the query in the time between getting suggestions and displaying them will cause
                       the old suggestions to be displayed. */
                    if (cleanedSearchString.isEmpty()) return@launch
                    mostRecentSuggestions.clear()
                    mostRecentSuggestions.addAll(suggestions)
                    shouldShowSuggestions = true
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showToast(context, "Error fetching results")
                    }
                    Log.e("App", "Error fetching autocomplete results", e)
                }
            } else {
                shouldShowSuggestions = false
            }
        }
    }


    @Composable
    fun TagListEntry(
        modifier: Modifier = Modifier,
        tag: TagSuggestion,
        position: ListItemPosition
    ) {
        ExpressiveTagEntryContainer(
            modifier = modifier,
            label = tag.label,
            supportingLabel = tag.category,
            position = position
        ) {
            searchString = ""
            cleanedSearchString = ""
            shouldShowSuggestions = false
            addToFilter(tag)
        }
    }


    @Composable
    fun AutoCompleteTagResults() {
        Column(
            modifier = Modifier
                .consumeWindowInsets(PaddingValues(0.dp, 0.dp, 0.dp, (BOTTOM_APP_BAR_HEIGHT + 16).dp))
                .imePadding()
        ) {
            Box(
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    )
                    .clip(largerShape)
            ) {
                val resultsState = rememberLazyListState()
                LaunchedEffect(mostRecentSuggestions.toList()) {
                    scope.launch {
                        resultsState.animateScrollToItem(0)
                    }
                }
                LazyColumn(
                    state = resultsState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (mostRecentSuggestions.isEmpty()) {
                        item {
                            Text(
                                fontSize = 16.sp,
                                text = "No results :(",
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                            )
                        }
                    } else {
                        mostRecentSuggestions.forEachIndexed { index, t ->
                            item(key = t.label) {
                                TagListEntry(
                                    tag = t,
                                    modifier = Modifier.animateItem(),
                                    position = when (index) {
                                        0 -> if (mostRecentSuggestions.size == 1) ListItemPosition.SINGLE_ELEMENT else ListItemPosition.TOP
                                        mostRecentSuggestions.lastIndex -> ListItemPosition.BOTTOM
                                        else -> ListItemPosition.MIDDLE
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun performSearch() {
        if (tagChipList.isEmpty())
            return showToast(context, "Please select some tags")

        if (
            prefs.ratingsFilter.isEmpty() ||
            (prefs.ratingsFilter.size == 1 && prefs.ratingsFilter[0] == ImageRating.SENSITIVE && prefs.imageSource == ImageSource.YANDERE)
        )
            return showToast(context, "Please select some ratings")

        if (!prefs.ratingsFilter.containsAll(availableRatingsForSource(prefs.imageSource))) {
            if (
                prefs.imageSource.imageBoard.localFilterType == ImageBoardLocalFilterType.REQUIRED &&
                !prefs.filterRatingsLocally
            ) {
                return showToast(context, "Enable the 'Filter ratings locally' option to filter ratings on this source.")
            } else if (
                prefs.imageSource.imageBoard.localFilterType == ImageBoardLocalFilterType.RECOMMENDED &&
                !prefs.filterRatingsLocally &&
                prefs.authFor(prefs.imageSource) == null
            ) {
                return showToast(context, "Set an API key or enable the 'Filter ratings locally' option to filter ratings on this source.")
            }
        }

        // Danbooru has the 2-tag limit and filtering by multiple negated tags simply does not work on Yande.re
        if (!danbooruLimitCheck())
            return

        if (prefs.saveSearchHistory) {
            scope.launch {
                context.prefs.addSearchHistoryEntry(
                    SearchHistoryEntry(
                        timestamp = Date().time,
                        source = prefs.imageSource,
                        tags = tagChipList.toSet(),
                        // Preserve the display order of ratings regardless of the order in search
                        ratings = ImageRating.entries.filter { it in prefs.ratingsFilter }.toSet()
                    )
                )
            }
        }

        val ratingsFilter = if (prefs.filterRatingsLocally) emptyList()
                            else ImageRating.buildQueryListFor(*prefs.ratingsFilter.toTypedArray())

        val tags = tagChipList.map { it.formattedLabel }.toMutableList().apply {
            for (rating in ratingsFilter) {
                addAll(rating)
            }
        }

        navController.navigate(Results(prefs.imageSource, tags))
    }

    fun beginSearch() {
        if (searchString.isEmpty()) return performSearch()
        if (mostRecentSuggestions.isEmpty()) return showToast(context, "No matching tags")

        addToFilter(mostRecentSuggestions[0])
        searchString = ""
        shouldShowSuggestions = false
    }

    MainScreenScaffold(
        title = "Search",
        additionalActions = {
            if (prefs.saveSearchHistory) {
                IconButton(
                    onClick = { showSearchHistoryPopup = true }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_history),
                        contentDescription = "Search history"
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                TextField(
                    modifier = Modifier
                        .weight(1f, true)
                        .focusRequester(focusRequester),
                    value = searchString,
                    textStyle = MaterialTheme.typography.searchField,
                    onValueChange = {
                        searchString = it
                        cleanedSearchString = searchString
                            .trim()
                            .replace(" ", "_")
                        getSuggestions()
                    },
                    placeholder = {
                        Text(
                            text = "Search ${prefs.imageSource.label}",
                            style = MaterialTheme.typography.searchField
                        )
                    },
                    shape = CircleShape,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Search
                        // Maybe also look into https://issuetracker.google.com/issues/359257538
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { beginSearch() },
                        onSearch = { beginSearch() }
                    ),
                    colors = TextFieldDefaults.colors().copy(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    prefix = { Spacer(Modifier.width(4.dp)) },
                    trailingIcon = {
                        Row(Modifier.padding(end = 4.dp)) {
                            IconButton(
                                onClick = {
                                    val tags = clipboard.getClip()
                                        .takeIf { it?.clipData?.description?.getMimeType(0) == "text/plain" }
                                        ?.clipData?.getItemAt(0)
                                        ?.let {
                                            it.text.split(" ")
                                                .filter { tag -> tag.trim().isNotEmpty() }
                                        }
                                    if (tags.isNullOrEmpty()) {
                                        showToast(context, "No tags to paste")
                                        return@IconButton
                                    }
                                    var count = 0
                                    for (t in tags) {
                                        val isExcluded = t.startsWith("-")
                                        val tagName = t.removePrefix("-")
                                        if (tagName.isNotEmpty()) {
                                            val tag =
                                                TagSuggestion(tagName, tagName, "", isExcluded)
                                            addToFilter(tag)
                                            count++
                                        }
                                    }
                                    showToast(
                                        context,
                                        "Pasted $count ${"tag".pluralise(count, "tags")}"
                                    )
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_paste),
                                    contentDescription = "Paste"
                                )
                            }
                            IconButton(
                                modifier = Modifier.rotate(chevronRotation),
                                onClick = { showSourceRatingBox = !showSourceRatingBox }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = "Filter"
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.size(8.dp))

                FloatingActionButton(
                    onClick = { performSearch() },
                    elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                ) {
                    Icon(
                        imageVector = Icons.Sharp.Search,
                        contentDescription = "Search"
                    )
                }
            }

            VerticalSpacer()

            AnimatedVisibility(showSourceRatingBox) {
                var opacity by remember { mutableFloatStateOf(1f) }
                var scale by remember { mutableFloatStateOf(1f) }
                LaunchedEffect(showSourceRatingBox) {
                    if (showSourceRatingBox) {
                        opacity = 1f
                        scale = 1f
                    }
                }
                PredictiveBackHandler(enabled = true) { progress ->
                    try {
                        progress.collect { backEvent ->
                            opacity = (1 - backEvent.progress * 5).coerceAtLeast(0f)
                            scale = 1 - (backEvent.progress / 2)
                        }
                        showSourceRatingBox = false
                    } catch (_: Exception) {
                    }
                }
                val sourceRows: List<@Composable () -> Unit> = ImageSource.entries.map {
                    {
                        FilterChip(
                            selected = prefs.imageSource == it,
                            label = { Text(it.label) },
                            onClick = {
                                if (it == currentSource) return@FilterChip
                                fun confirm() {
                                    scope.launch {
                                        context.prefs.updatePref(PreferenceKeys.IMAGE_SOURCE, it)
                                    }
                                    tagChipList.clear()
                                    if (shouldShowSuggestions) getSuggestions(
                                        bypassDelay = true,
                                        source = it
                                    )
                                }
                                if (tagChipList.isEmpty()) return@FilterChip confirm()
                                sourceChangeDialogData = SourceDialogData(
                                    from = currentSource,
                                    to = it,
                                    onConfirm = ::confirm
                                )
                                showSourceChangeDialog = true
                            }
                        )
                    }
                }
                val ratingRows: List<@Composable () -> Unit> =
                    availableRatingsForCurrentSource.map {
                        {
                            FilterChip(
                                selected = it in prefs.ratingsFilter,
                                label = { Text(it.label) },
                                onClick = {
                                    scope.launch {
                                        if (it in prefs.ratingsFilter) {
                                            context.prefs.removeFromSet(
                                                PreferenceKeys.RATINGS_FILTER,
                                                it
                                            )
                                        } else {
                                            context.prefs.addToSet(
                                                PreferenceKeys.RATINGS_FILTER,
                                                it
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                Column(Modifier.padding(horizontal = 16.dp)) {
                    HorizontallyScrollingChipsWithLabels(
                        modifier = Modifier
                            .alpha(opacity)
                            .scale(scale),
                        labels = listOf("Source", "Ratings"),
                        content = listOf(sourceRows, ratingRows)
                    )
                    VerticalSpacer()
                }
            }

            AnimatedVisibility(tagChipList.isNotEmpty()) {
                Column {
                    Row(
                        modifier = Modifier
                            .height(FilterChipDefaults.Height)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING.dp)
                    ) {
                        Spacer(Modifier.width((16 - CHIP_SPACING).dp))
                        for (t in tagChipList) {
                            FilterChip(
                                label = { Text(t.value) },
                                selected = !t.isExcluded,
                                onClick = {
                                    tagChipList.remove(t)
                                }
                            )
                        }
                        if (tagChipList.isNotEmpty()) {
                            AssistChip(
                                modifier = Modifier.aspectRatio(1f),
                                onClick = {
                                    val tags = tagChipList.joinToString(" ") { it.formattedLabel }
                                    copyText(
                                        context = context,
                                        clipboardManager = clipboard,
                                        text = tags,
                                        message = "Copied ${tagChipList.size} ${
                                            "tag".pluralise(
                                                tagChipList.size,
                                                "tags"
                                            )
                                        }"
                                    )
                                },
                                label = { },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_copy),
                                        contentDescription = "Copy all"
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width((16 - CHIP_SPACING).dp))
                    }
                    VerticalSpacer()
                }
            }
            AnimatedVisibility(shouldShowSuggestions) {
                AutoCompleteTagResults()
            }
        }
    }

    if (showSourceChangeDialog) {
        val data = sourceChangeDialogData!!
        AlertDialog(
            onDismissRequest = { showSourceChangeDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        data.onConfirm()
                        showSourceChangeDialog = false
                    }
                ) {
                    Text("Okay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSourceChangeDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Change image source") },
            text = {
                Text(
                    text = "Changing image source will clear your search tags. " +
                            "Are you sure you want to change the source from " +
                            "${data.from.label} to ${data.to.label}?"
                )
            }
        )
    }

    if (showSearchHistoryPopup) {
        TitledModalBottomSheet(
            onDismissRequest = { showSearchHistoryPopup = false },
            sheetState = historySheetState,
            title = "Search history"
        ) {
            LazyColumn(
                modifier = Modifier
                    .animateContentSize()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp, 16.dp)),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (prefs.searchHistory.isEmpty()) {
                    item {
                        Text(
                            text = "Nothing to see here.",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(prefs.searchHistory.reversed(), key = { it.timestamp }) {
                        SearchHistoryListItem(item = it) {
                            tagChipList.clear()
                            tagChipList.addAll(it.tags)
                            searchString = ""
                            shouldShowSuggestions = false
                            scope.launch {
                                context.prefs.updatePref(PreferenceKeys.IMAGE_SOURCE, it.source)
                                context.prefs.replaceImageRatings(it.ratings)
                                historySheetState.hide()
                                showSearchHistoryPopup = false
                            }
                        }
                    }
                }
                item { NavBarHeightVerticalSpacer() }
            }
        }
    }
}


private data class SourceDialogData(
    val from: ImageSource,
    val to: ImageSource,
    val onConfirm: () -> Unit,
)