package moe.apex.breadboard.artist

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import moe.apex.breadboard.R
import moe.apex.breadboard.detailview.FlexibleImageGrid
import moe.apex.breadboard.detailview.FlexibleImageGridDefaults
import moe.apex.breadboard.image.Image
import moe.apex.breadboard.largeimageview.OffsetBasedLargeImageView
import moe.apex.breadboard.navigation.Results
import moe.apex.breadboard.preferences.Experiment
import moe.apex.breadboard.preferences.ImageSource
import moe.apex.breadboard.preferences.LocalPreferences
import moe.apex.breadboard.preferences.PreferenceKeys
import moe.apex.breadboard.prefs
import moe.apex.breadboard.social.SocialEntry
import moe.apex.breadboard.social.SocialSite
import moe.apex.breadboard.ui.theme.BreadboardTheme
import moe.apex.breadboard.util.LARGE_SPACER
import moe.apex.breadboard.util.LargeVerticalSpacer
import moe.apex.breadboard.util.MEDIUM_SPACER
import moe.apex.breadboard.util.MainScreenScaffold
import moe.apex.breadboard.util.NavigationIcon
import moe.apex.breadboard.util.SMALL_LARGE_SPACER
import moe.apex.breadboard.util.SMALL_SPACER
import moe.apex.breadboard.util.Summary
import moe.apex.breadboard.util.TINY_SPACER
import moe.apex.breadboard.util.copyText
import moe.apex.breadboard.util.generateColours
import moe.apex.breadboard.util.navBarHeight
import moe.apex.breadboard.util.showToast
import moe.apex.breadboard.viewmodel.ArtistProfileViewModel
import moe.apex.breadboard.viewmodel.getGlobalViewModel
import kotlin.random.Random


/* The annoying work with the Navigation Icon here is because the normal TopAppBars still fill the
   status bar when collapsed, and I want the content to scroll beneath it because it looks much
   more polished. */
@Composable
fun ArtistProfileScreen(
    artistTag: String,
    originImageSource: ImageSource,
    viewModel: ArtistProfileViewModel = viewModel(key = artistTag) { ArtistProfileViewModel(artistTag) },
    navController: NavController
) {
    val artist by viewModel.artistProfile.collectAsState()
    val images by viewModel.images.collectAsState()
    val isInitialised by viewModel.isInitialised.collectAsState()

    val context = LocalContext.current
    val prefs = LocalPreferences.current
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current

    val isWideScreen = remember {
        with(density) {
            windowInfo.containerSize.width.toDp() >= 840.dp // M3 spec for large device in landscape
        }
    }

    var shouldShowLargeImage by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(artistTag) {
        try {
            viewModel.loadArtistProfile()
        } catch (_: CancellationException) {
            return@LaunchedEffect
        } catch (e: Exception) {
            Log.e("ArtistProfileScreen", "Error loading artist profile", e)
        }

        if (artist == null) {
            showToast(context, "Couldn't fetch profile. Performing search instead...")
            navController.popBackStack()
            navController.navigate(Results(originImageSource, listOf(artistTag)))
        }
    }

    MainScreenScaffold(
        topAppBar = { },
        blur = shouldShowLargeImage && prefs.isExperimentEnabled(Experiment.IMMERSIVE_UI_EFFECTS),
        addBottomPadding = false,
        floatingActionButton = {
            AnimatedVisibility(
                visible = isInitialised && artist != null,
                enter = fadeIn(),
                exit = ExitTransition.None
            ) {
                ExtendedFloatingActionButton(
                    text = { Text("View all posts") },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        navController.navigate(Results(originImageSource, listOf(artistTag)))
                    }
                )
            }
        }
    ) {
        if (!isInitialised || artist == null) {
            Box(modifier = Modifier.fillMaxSize()) {
                NavigationButtonBox(
                    modifier = Modifier.padding(start = MEDIUM_SPACER.dp),
                    navController = navController
                )

                if (!isInitialised) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        amplitude = 0.6f
                    )
                }
            }
        } else if (isWideScreen) {
            SplitProfileLayout(
                navController = navController,
                artist = artist,
                artistTag = artistTag,
                images = images
            ) { index, _ ->
                selectedImageIndex = index
                shouldShowLargeImage = true
            }
        } else {
            SinglePaneProfileLayout(
                navController = navController,
                artist = artist,
                artistTag = artistTag,
                images = images
            ) { index, _ ->
                selectedImageIndex = index
                shouldShowLargeImage = true
            }
        }
    }

    OffsetBasedLargeImageView(
        navController = navController,
        isActive = shouldShowLargeImage,
        initialSelectedImageIndex = selectedImageIndex,
        allImages = images,
        onActiveStateChanged = { shouldShowLargeImage = it },
        onImageUpdate = { oldImage, newImage ->
            viewModel.updateImage(oldImage, newImage)
        }
    )
}


@Composable
private fun SinglePaneProfileLayout(
    navController: NavController,
    artist: Artist?,
    artistTag: String,
    images: List<Image>,
    onImageClick: (Int, Image) -> Unit
) {
    val prefs = LocalPreferences.current

    FlexibleImageGrid(
        staggered = prefs.useStaggeredGrid,
        contentPadding = PaddingValues(
            start = SMALL_LARGE_SPACER.dp,
            end = SMALL_LARGE_SPACER.dp,
            bottom = 88.dp // FAB height + 16dp vertical padding
        ),
        images = images,
        onImageClick = onImageClick,
        headerItems = {
            item {
                NavigationButtonBox(
                    modifier = Modifier.offset(x = -TINY_SPACER.dp),
                    navController = navController
                )
            }
            item {
                ArtistHeader(artist!!, images)
            }
            item {
                ArtistToolbar(artistTag)
            }
            item {
                PopularPostsHeading()
            }
        },
        noImagesContent = {
            FlexibleImageGridDefaults.NoImages(
                text = "We weren't able to show any posts here.\n" +
                        "Tap the button to search all posts."
            )
        }
    )
}


@Composable
private fun SplitProfileLayout(
    navController: NavController,
    artist: Artist?,
    artistTag: String,
    images: List<Image>,
    onImageClick: (Int, Image) -> Unit
) {
    val prefs = LocalPreferences.current

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(SMALL_SPACER.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = SMALL_LARGE_SPACER.dp,
                    end = SMALL_LARGE_SPACER.dp,
                    bottom = navBarHeight + SMALL_LARGE_SPACER.dp
                )
        ) {
            NavigationButtonBox(
                modifier = Modifier.offset(x = -TINY_SPACER.dp),
                navController = navController
            )
            ArtistHeader(artist!!, images)
            ArtistToolbar(artistTag)
        }
        FlexibleImageGrid(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            staggered = prefs.useStaggeredGrid,
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                        + TopAppBarDefaults.LargeAppBarCollapsedHeight
                        + SMALL_SPACER.dp,
                bottom = 88.dp, // FAB height + 16dp vertical padding
                start = SMALL_LARGE_SPACER.dp,
                end = SMALL_LARGE_SPACER.dp
            ),
            images = images,
            onImageClick = onImageClick,
            headerItems = {
                item {
                    PopularPostsHeading(withDivider = false)
                }
            },
            noImagesContent = {
                FlexibleImageGridDefaults.NoImages(
                    text = "We weren't able to show any posts here.\n" +
                            "Tap the button to search all posts."
                )
            }
        )
    }
}


@Composable
private fun ArtistToolbar(artistTag: String, ) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val prefs = LocalPreferences.current
    val viewModel = getGlobalViewModel()

    val scope = rememberCoroutineScope()
    val preferencesRepository = context.prefs

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(SMALL_SPACER.dp),
            horizontalArrangement = Arrangement.spacedBy(SMALL_SPACER.dp)
        ) {
            val isFollowing = artistTag in prefs.followedTags
            val isBlocked = artistTag in prefs.blockedTags

            ButtonGroup(
                overflowIndicator = { },
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(TINY_SPACER.dp)
            ) {
                customItem(
                    buttonGroupContent = {
                        val interactionSource = remember { MutableInteractionSource() }
                        ToggleButton(
                            interactionSource = interactionSource,
                            checked = isFollowing,
                            enabled = !isBlocked,
                            modifier = Modifier
                                .height(48.dp)
                                .weight(3f)
                                .animateWidth(interactionSource),
                            onCheckedChange = {
                                scope.launch {
                                    if (it) {
                                        preferencesRepository.addToSet(
                                            PreferenceKeys.FOLLOWED_TAGS,
                                            artistTag
                                        )
                                    } else {
                                        preferencesRepository.removeFromSet(
                                            PreferenceKeys.FOLLOWED_TAGS,
                                            artistTag
                                        )
                                    }
                                }.invokeOnCompletion {
                                    viewModel.setFollowingProvider(null)
                                }
                            },
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                checkedContainerColor = MaterialTheme.colorScheme.secondary,
                                checkedContentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            shapes = ToggleButtonShapes(
                                shape = CircleShape.copy(
                                    topEnd = CornerSize(8.dp),
                                    bottomEnd = CornerSize(8.dp)
                                ),
                                checkedShape = CircleShape.copy(
                                    topEnd = CornerSize(8.dp),
                                    bottomEnd = CornerSize(8.dp)
                                ),
                                pressedShape = CircleShape.copy(
                                    topEnd = CornerSize(8.dp),
                                    bottomEnd = CornerSize(8.dp)
                                )
                            )
                        ) {
                            Text(
                                text = if (isBlocked) "Blocked" else if (!isFollowing) "Follow" else "Following"
                            )
                        }
                    },
                    menuContent = { }
                )

                customItem(
                    buttonGroupContent = {
                        val interactionSource = remember { MutableInteractionSource() }
                        Button(
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .height(48.dp)
                                .weight(2f)
                                .animateWidth(interactionSource),
                            shape = CircleShape.copy(
                                topStart = CornerSize(8.dp),
                                bottomStart = CornerSize(8.dp)
                            ),
                            onClick = {
                                shareArtist(
                                    context = context,
                                    artistTag = artistTag,
                                    addFriendlyMessage = true
                                )
                            }
                        ) {
                            Text("Share")
                        }
                    },
                    menuContent = { }
                )
            }

            var showDropdown by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    modifier = Modifier.size(48.dp),
                    onClick = {
                        showDropdown = !showDropdown
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options"
                    )
                }
                DropdownMenu(
                    expanded = showDropdown,
                    shape = MaterialTheme.shapes.medium,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onDismissRequest = { showDropdown = false }
                ) {
                    DropdownMenuItem(
                        onClick = {
                            scope.launch {
                                copyText(
                                    context = context,
                                    clipboard = clipboard,
                                    text = "https://breadboard.moe/artist/$artistTag",
                                )
                            }.invokeOnCompletion {
                                showDropdown = false
                            }
                        },
                        text = { Text("Copy link") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_link),
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        onClick = {
                            scope.launch {
                                copyText(
                                    context = context,
                                    clipboard = clipboard,
                                    text = artistTag,
                                )
                            }.invokeOnCompletion {
                                showDropdown = false
                            }
                        },
                        text = { Text("Copy search tag") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        onClick = {
                            scope.launch {
                                if (artistTag in prefs.manuallyBlockedTags) {
                                    preferencesRepository.removeFromSet(
                                        PreferenceKeys.MANUALLY_BLOCKED_TAGS,
                                        artistTag
                                    )
                                } else {
                                    preferencesRepository.addToSet(
                                        PreferenceKeys.MANUALLY_BLOCKED_TAGS,
                                        artistTag
                                    )
                                    if (artistTag in prefs.followedTags) {
                                        preferencesRepository.removeFromSet(
                                            PreferenceKeys.FOLLOWED_TAGS,
                                            artistTag
                                        )
                                    }
                                }
                            }.invokeOnCompletion {
                                viewModel.resetProviders()
                                showDropdown = false
                            }
                        },
                        text = {
                            Text(
                                text = if (artistTag in prefs.manuallyBlockedTags) {
                                    "Unblock artist"
                                } else "Block artist"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Block,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun PopularPostsHeading(withDivider: Boolean = true) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = LARGE_SPACER.dp, bottom = MEDIUM_SPACER.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LARGE_SPACER.dp)
    ) {
        if (withDivider) {
            HorizontalDivider(Modifier.fillMaxWidth(0.5f))
        }
        Text(
            text = "Popular posts",
            style = MaterialTheme.typography.titleLarge
        )
    }
}


@Composable
private fun NavigationButtonBox(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    Box(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = SMALL_SPACER.dp)
            .height(TopAppBarDefaults.LargeAppBarCollapsedHeight),
        contentAlignment = Alignment.CenterStart
    ) {
        NavigationIcon(
            modifier = modifier,
            navController = navController
        )
    }
}


@Composable
private fun ArtistHeader(
    artist: Artist,
    images: List<Image>,
) {
    val uriHandler = LocalUriHandler.current

    val groupedSocials = remember { artist.groupSocials() }

    BreadboardTheme(darkTheme = true) { // We're adding a dark scrim, so assume always dark theme.
        Surface(shape = MaterialTheme.shapes.extraLargeIncreased) {
            Box {
                if (images.isNotEmpty()) {
                    val randomImageIndex = rememberSaveable { Random.nextInt(images.size) }
                    AsyncImage(
                        model = images[randomImageIndex].let {
                            if (it.isVideo || it.fileFormat == "gif" || it.fileFormat == "webp") {
                                it.previewUrl // Can't use videos, and we don't want animated images
                            } else {
                                it.sampleUrl // Highest quality isn't needed when we're blurring it anyway
                            }
                        },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .then(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12
                                    Modifier.blur(radius = 16.dp)
                                } else Modifier
                            )
                    )
                } else {
                    // If no images to show, just show a solid colour based on their name.
                    val painter = remember { ColorPainter(generateColours(false, artist.name).first) }
                    androidx.compose.foundation.Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize()
                    )
                }

                Column(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f))
                        .fillMaxWidth()
                        .padding(
                            top = LARGE_SPACER.dp * 2,
                            bottom = LARGE_SPACER.dp
                        )
                ) {
                    Text(
                        text = artist.name,
                        modifier = Modifier.padding(horizontal = LARGE_SPACER.dp),
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 16.sp,
                            maxFontSize = 48.sp
                        ),
                        maxLines = 1,
                        fontFamily = FontFamily(Font(R.font.special_gothic_one_expanded)),
                        fontWeight = FontWeight.Bold
                    )
                    artist.otherNames.takeIf { it.isNotEmpty() }?.let {
                        Summary(
                            text = "Also known as:\n" + it.joinToString(" / "),
                            modifier = Modifier.padding(horizontal = LARGE_SPACER.dp)
                        )
                    }
                    LargeVerticalSpacer()
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = LARGE_SPACER.dp),
                        horizontalArrangement = Arrangement.spacedBy(MEDIUM_SPACER.dp)
                    ) {
                        groupedSocials.forEach { (site, socials) ->
                            if (site != SocialSite.IMAGEBOARD) {
                                item {
                                    SocialChip(
                                        site = site,
                                        socialProfiles = socials,
                                        onClick = { uriHandler.openUri(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun SocialChip(
    site: SocialSite,
    socialProfiles: List<SocialEntry>,
    onClick: (String) -> Unit)
{
    var showDropdown by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(if (showDropdown) 180f else 0f)
    val hasDropdownAvailable = remember { socialProfiles.size > 1 || site == SocialSite.OTHER }

    Box {
        InputChip(
            selected = showDropdown,
            modifier = Modifier.heightIn(42.dp),
            elevation = InputChipDefaults.inputChipElevation(elevation = 3.dp),
            shapes = InputChipDefaults.shapes(
                shape = InputChipDefaults.shapes().selectedShape,
                selectedShape = InputChipDefaults.shapes().shape
            ),
            contentPadding = InputChipDefaults.contentPadding(
                hasAvatar = false,
                hasLeadingIcon = true,
                hasTrailingIcon = hasDropdownAvailable
            ) + PaddingValues(start = 4.dp), // We make the icons smaller so we're offsetting the difference
            onClick = {
                if (!hasDropdownAvailable) {
                    onClick(socialProfiles.first().url)
                } else {
                    showDropdown = true
                }
            },
            label = {
                Text(
                    text = site.label,
                    modifier = Modifier.padding(
                        start = TINY_SPACER.dp,
                        end = if (!hasDropdownAvailable) TINY_SPACER.dp else 0.dp
                    )
                )
            },
            colors = InputChipDefaults.inputChipColors(
                containerColor = (site.color?.let { Color(it) }
                    ?: MaterialTheme.colorScheme.surfaceContainerHigh).copy(alpha = 0.75f),
                labelColor = MaterialTheme.colorScheme.onSurface,
                leadingIconColor = MaterialTheme.colorScheme.onSurface,
                trailingIconColor = MaterialTheme.colorScheme.onSurface,
                selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onSurface,
                selectedTrailingIconColor = MaterialTheme.colorScheme.onSurface,
                selectedContainerColor = site.color?.let { Color(it) }
                    ?: MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            border = null,
            leadingIcon = {
                Icon(
                    painter = painterResource(site.iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                )
            },
            trailingIcon = if (hasDropdownAvailable) {
                {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "View all links for ${site.label}",
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = arrowRotation
                            }
                    )
                }
            } else null
        )

        BreadboardTheme {
            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
                shape = MaterialTheme.shapes.medium,
                containerColor = (site.color // Tint the dropdown slightly based on the site's colour
                    ?.let { Color(it).copy(alpha = 0.1f) }
                    ?: Color.Transparent)
                    .compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                for (socialEntry in socialProfiles) {
                    DropdownMenuItem(
                        text = { Text(socialEntry.url) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(site.iconRes),
                                contentDescription = null,
                                tint = site.color?.let { Color(it) } ?: LocalContentColor.current
                            )
                        },
                        onClick = {
                            onClick(socialEntry.url)
                            showDropdown = false
                        }
                    )
                }
            }
        }
    }
}


private fun shareArtist(context: Context, artistTag: String, addFriendlyMessage: Boolean) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            "${if (addFriendlyMessage) "Check out this artist on Breadboard! " else ""}https://breadboard.moe/artist/$artistTag"
        )
    }
    context.startActivity(Intent.createChooser(shareIntent, null))
}