package moe.apex.breadboard.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import moe.apex.breadboard.navigation.ArtistProfile
import moe.apex.breadboard.preferences.ImageSource
import moe.apex.breadboard.preferences.LocalPreferences
import moe.apex.breadboard.preferences.PreferenceKeys
import moe.apex.breadboard.prefs
import moe.apex.breadboard.ui.theme.shouldUseDarkTheme
import moe.apex.breadboard.util.DISABLED_OPACITY
import moe.apex.breadboard.util.ExpressiveTagEntryContainer
import moe.apex.breadboard.util.LargeTitleBar
import moe.apex.breadboard.util.LargeVerticalSpacer
import moe.apex.breadboard.util.ListItemPosition
import moe.apex.breadboard.util.MEDIUM_SPACER
import moe.apex.breadboard.util.MainScreenScaffold
import moe.apex.breadboard.util.SMALL_LARGE_SPACER
import moe.apex.breadboard.util.Summary
import moe.apex.breadboard.util.TINY_SPACER
import moe.apex.breadboard.util.generateColours
import moe.apex.breadboard.viewmodel.getGlobalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowedArtistsScreen(navController: NavHostController) {
    val viewModel = getGlobalViewModel()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val scope = rememberCoroutineScope()
    val userPreferencesRepository = LocalContext.current.prefs
    val prefs = LocalPreferences.current
    val followedTags = prefs.followedTags.toList().sorted()
    val darkTheme = shouldUseDarkTheme()

    MainScreenScaffold(
        topAppBar = {
            LargeTitleBar(
                title = "Followed artists",
                scrollBehavior = scrollBehavior,
                navController = navController
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(
                start = MEDIUM_SPACER.dp,
                end = MEDIUM_SPACER.dp,
                top = SMALL_LARGE_SPACER.dp, // Unbalanced but it looks better with the text.
                bottom = MEDIUM_SPACER.dp
            ),
        ) {
            item {
                Summary(
                    modifier = Modifier.padding(horizontal = TINY_SPACER.dp),
                    text = "Posts from these artists will appear in your Following feed.\n" +
                           "Tap an artist's name to visit their profile, or unfollow them by " +
                           "tapping the icon.",
                )
                LargeVerticalSpacer()
            }
            if (followedTags.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(DISABLED_OPACITY),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PersonOff,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp)
                        )
                        Text(
                            text = "You aren't following anyone yet.",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                return@LazyColumn
            }

            itemsIndexed(followedTags, key = { _, tag -> tag }) { index, tag ->
                val monogramChar = remember(tag) {
                    tag.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                }
                val colourPair = remember(tag) {
                    generateColours(darkTheme, tag)
                }

                ExpressiveTagEntryContainer(
                    modifier = Modifier.animateItem(),
                    label = tag,
                    position = ListItemPosition.fromIndex(followedTags, index),
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colourPair.first),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = monogramChar,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = colourPair.second
                            )
                        }
                    },
                    onClick = {
                        navController.navigate(ArtistProfile(tag, ImageSource.DANBOORU))
                    },
                    trailingContent = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    userPreferencesRepository.removeFromSet(
                                        PreferenceKeys.FOLLOWED_TAGS,
                                        tag
                                    )
                                    viewModel.setFollowingProvider(null)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonOff,
                                contentDescription = "Unfollow artist",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        }
    }
}
