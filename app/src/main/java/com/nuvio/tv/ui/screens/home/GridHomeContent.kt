package com.nuvio.tv.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvGridItemSpan
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.ui.components.GridContentCard
import com.nuvio.tv.ui.components.HeroCarousel
import com.nuvio.tv.ui.theme.NuvioColors
import kotlinx.coroutines.flow.distinctUntilChanged

private const val GRID_COLUMNS = 5

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GridHomeContent(
    uiState: HomeUiState,
    gridFocusState: HomeScreenFocusState,
    onNavigateToDetail: (String, String, String) -> Unit,
    onNavigateToCatalogSeeAll: (String, String, String) -> Unit,
    onLoadMore: (catalogId: String, addonId: String, type: String) -> Unit,
    onSaveGridFocusState: (Int, Int) -> Unit
) {
    val gridState = rememberTvLazyGridState(
        initialFirstVisibleItemIndex = gridFocusState.verticalScrollIndex,
        initialFirstVisibleItemScrollOffset = gridFocusState.verticalScrollOffset
    )

    // Save scroll state when leaving
    DisposableEffect(Unit) {
        onDispose {
            onSaveGridFocusState(
                gridState.firstVisibleItemIndex,
                gridState.firstVisibleItemScrollOffset
            )
        }
    }

    // Build index-to-section mapping for sticky header
    val sectionMapping = remember(uiState.gridItems) {
        buildSectionMapping(uiState.gridItems)
    }

    // Track current section for sticky header
    var currentSectionName by remember { mutableStateOf<String?>(null) }
    var currentSectionInfo by remember { mutableStateOf<SectionInfo?>(null) }

    LaunchedEffect(gridState, sectionMapping) {
        snapshotFlow { gridState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstVisibleIndex ->
                val section = sectionMapping.findSectionForIndex(firstVisibleIndex)
                currentSectionName = section?.catalogName
                currentSectionInfo = section
            }
    }

    // Determine if hero is scrolled past
    val isScrolledPastHero by remember {
        derivedStateOf {
            val hasHero = uiState.gridItems.firstOrNull() is GridItem.Hero
            if (hasHero) {
                gridState.firstVisibleItemIndex > 0
            } else {
                true
            }
        }
    }

    // Per-section load-more detection
    LaunchedEffect(gridState, uiState.gridItems) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                // Find which sections are near their end
                val sectionBounds = buildSectionBounds(uiState.gridItems)
                for (bound in sectionBounds) {
                    // If the last visible item is within 5 items of this section's end
                    if (lastVisibleIndex >= bound.lastContentIndex - 5 && lastVisibleIndex <= bound.lastContentIndex) {
                        val row = uiState.catalogRows.find {
                            it.catalogId == bound.catalogId && it.addonId == bound.addonId
                        }
                        if (row != null && row.hasMore && !row.isLoading) {
                            onLoadMore(row.catalogId, row.addonId, row.type.toApiString())
                        }
                    }
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TvLazyVerticalGrid(
            state = gridState,
            columns = TvGridCells.Fixed(GRID_COLUMNS),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 0.dp,
                bottom = 32.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            uiState.gridItems.forEachIndexed { index, gridItem ->
                when (gridItem) {
                    is GridItem.Hero -> {
                        item(
                            key = "hero",
                            span = { TvGridItemSpan(maxLineSpan) }
                        ) {
                            HeroCarousel(
                                items = gridItem.items,
                                onItemClick = { item ->
                                    onNavigateToDetail(
                                        item.id,
                                        item.type.toApiString(),
                                        ""
                                    )
                                }
                            )
                        }
                    }

                    is GridItem.SectionDivider -> {
                        item(
                            key = "divider_${gridItem.catalogId}_${gridItem.addonId}",
                            span = { TvGridItemSpan(maxLineSpan) }
                        ) {
                            SectionDivider(
                                catalogName = gridItem.catalogName,
                                onSeeAllClick = {
                                    onNavigateToCatalogSeeAll(
                                        gridItem.catalogId,
                                        gridItem.addonId,
                                        gridItem.type
                                    )
                                }
                            )
                        }
                    }

                    is GridItem.Content -> {
                        item(
                            key = "content_${gridItem.catalogId}_${gridItem.item.id}",
                            span = { TvGridItemSpan(1) }
                        ) {
                            GridContentCard(
                                item = gridItem.item,
                                onClick = {
                                    onNavigateToDetail(
                                        gridItem.item.id,
                                        gridItem.item.type.toApiString(),
                                        gridItem.addonBaseUrl
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Sticky header overlay
        AnimatedVisibility(
            visible = isScrolledPastHero && currentSectionName != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            StickyCategoryHeader(
                sectionName = currentSectionName ?: "",
                onSeeAllClick = {
                    currentSectionInfo?.let { info ->
                        onNavigateToCatalogSeeAll(info.catalogId, info.addonId, info.type)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionDivider(
    catalogName: String,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 12.dp, start = 24.dp, end = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = catalogName,
            style = MaterialTheme.typography.headlineMedium,
            color = NuvioColors.TextPrimary
        )

        Button(
            onClick = onSeeAllClick,
            modifier = Modifier.height(36.dp),
            shape = ButtonDefaults.shape(
                shape = RoundedCornerShape(18.dp)
            ),
            colors = ButtonDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.1f),
                focusedContainerColor = NuvioColors.FocusBackground
            ),
            border = ButtonDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(1.dp, NuvioColors.FocusRing),
                    shape = RoundedCornerShape(18.dp)
                )
            )
        ) {
            Text(
                text = "See All \u2192",
                style = MaterialTheme.typography.labelMedium,
                color = NuvioColors.TextSecondary
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StickyCategoryHeader(
    sectionName: String,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to NuvioColors.Background,
                        0.7f to NuvioColors.Background.copy(alpha = 0.95f),
                        1.0f to Color.Transparent
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Crossfade(
                targetState = sectionName,
                animationSpec = tween(300),
                label = "sectionNameCrossfade"
            ) { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    color = NuvioColors.TextPrimary
                )
            }

            Button(
                onClick = onSeeAllClick,
                modifier = Modifier.height(32.dp),
                shape = ButtonDefaults.shape(
                    shape = RoundedCornerShape(16.dp)
                ),
                colors = ButtonDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    focusedContainerColor = NuvioColors.FocusBackground
                ),
                border = ButtonDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(1.dp, NuvioColors.FocusRing),
                        shape = RoundedCornerShape(16.dp)
                    )
                )
            ) {
                Text(
                    text = "See All \u2192",
                    style = MaterialTheme.typography.labelSmall,
                    color = NuvioColors.TextSecondary
                )
            }
        }
    }
}

// Section mapping utilities

private data class SectionInfo(
    val catalogName: String,
    val catalogId: String,
    val addonId: String,
    val type: String
)

private class SectionMapping(
    private val indexToSection: Map<Int, SectionInfo>
) {
    fun findSectionForIndex(index: Int): SectionInfo? {
        // Find the section for the given index by searching backwards
        for (i in index downTo 0) {
            indexToSection[i]?.let { return it }
        }
        return null
    }
}

private fun buildSectionMapping(gridItems: List<GridItem>): SectionMapping {
    val mapping = mutableMapOf<Int, SectionInfo>()
    gridItems.forEachIndexed { index, item ->
        if (item is GridItem.SectionDivider) {
            mapping[index] = SectionInfo(
                catalogName = item.catalogName,
                catalogId = item.catalogId,
                addonId = item.addonId,
                type = item.type
            )
        }
    }
    return SectionMapping(mapping)
}

private data class SectionBound(
    val catalogId: String,
    val addonId: String,
    val lastContentIndex: Int
)

private fun buildSectionBounds(gridItems: List<GridItem>): List<SectionBound> {
    val bounds = mutableListOf<SectionBound>()
    var currentCatalogId: String? = null
    var currentAddonId: String? = null
    var lastContentIndex = 0

    fun flushSection() {
        val catId = currentCatalogId ?: return
        val addId = currentAddonId ?: return
        bounds.add(SectionBound(catId, addId, lastContentIndex))
    }

    gridItems.forEachIndexed { index, item ->
        when (item) {
            is GridItem.SectionDivider -> {
                flushSection()
                currentCatalogId = item.catalogId
                currentAddonId = item.addonId
            }
            is GridItem.Content -> {
                lastContentIndex = index
            }
            is GridItem.Hero -> { /* skip */ }
        }
    }
    flushSection()
    return bounds
}
