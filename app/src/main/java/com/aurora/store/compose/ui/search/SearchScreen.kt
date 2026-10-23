/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.search

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.aurora.extensions.emptyPagingItems
import com.aurora.gplayapi.SearchSuggestEntry
import com.aurora.gplayapi.data.models.App
import com.aurora.store.R
import com.aurora.store.compose.composable.AuroraListItem
import com.aurora.store.compose.composable.ContainedLoadingIndicator
import com.aurora.store.compose.composable.Placeholder
import com.aurora.store.compose.composable.ScrollHint
import com.aurora.store.compose.composable.SearchSuggestionListItem
import com.aurora.store.compose.composable.SectionHeader
import com.aurora.store.compose.composable.app.LargeAppListItem
import com.aurora.store.compose.navigation.Destination
import com.aurora.store.compose.preview.AppPreviewProvider
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.compose.ui.details.AppDetailsScreen
import com.aurora.store.data.model.normalizeSearchQuery
import com.aurora.store.viewmodel.details.AppDetailsViewModel
import com.aurora.store.viewmodel.search.SearchViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(viewModel: SearchViewModel = hiltViewModel()) {
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val results = viewModel.apps.collectAsLazyPagingItems()

    ScreenContent(
        suggestions = suggestions,
        history = history,
        results = results,
        onSearch = viewModel::submitSearch,
        onFetchSuggestions = viewModel::fetchSuggestions,
        onRemoveHistory = viewModel::removeHistory,
        onClearHistory = viewModel::clearHistory
    )
}

@Composable
private fun ScreenContent(
    suggestions: List<SearchSuggestEntry> = emptyList(),
    history: List<String> = emptyList(),
    results: LazyPagingItems<App> = emptyPagingItems(),
    onFetchSuggestions: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onRemoveHistory: (String) -> Unit = {},
    onClearHistory: () -> Unit = {}
) {
    val activity = LocalActivity.current as? ComponentActivity
    val textFieldState = rememberTextFieldState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var hasFocus by remember { mutableStateOf(false) }
    var isSearching by rememberSaveable { mutableStateOf(false) }

    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<String>()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(textFieldState, hasFocus) {
        snapshotFlow { textFieldState.text.toString() }
            .collectLatest { query -> onFetchSuggestions(if (hasFocus) query else "") }
    }

    fun dismissSuggestions() {
        focusManager.clearFocus()
        keyboard?.hide()
    }

    BackHandler(enabled = hasFocus) { dismissSuggestions() }

    fun showDetailPane(packageName: String) {
        dismissSuggestions()
        coroutineScope.launch {
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, packageName)
        }
    }

    fun onRequestSearch(query: String) {
        val trimmed = normalizeSearchQuery(query) ?: return
        if (textFieldState.text.toString() != trimmed) {
            textFieldState.setTextAndPlaceCursorAtEnd(trimmed)
        }
        onSearch(trimmed)
        isSearching = true
        dismissSuggestions()
    }

    @Composable
    fun SearchBar() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(dimensionResource(R.dimen.spacing_small)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { activity?.onBackPressedDispatcher?.onBackPressed() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.action_back)
                )
            }
            OutlinedTextField(
                state = textFieldState,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { hasFocus = it.isFocused },
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                onKeyboardAction = { onRequestSearch(textFieldState.text.toString()) },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_hint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_round_search),
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (textFieldState.text.isNotEmpty()) {
                        IconButton(onClick = { textFieldState.clearText() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_cancel),
                                contentDescription = stringResource(R.string.action_clear)
                            )
                        }
                    }
                }
            )
        }
    }

    @Composable
    fun InlineSuggestions() {
        val query = textFieldState.text.toString().trim()
        val matchingHistory = history.filter { it.contains(query, ignoreCase = true) }
        val matchingSuggestions = suggestions.filter { suggestion ->
            matchingHistory.none { it.equals(suggestion.title, ignoreCase = true) }
        }
        val hasEntries = matchingHistory.isNotEmpty() || matchingSuggestions.isNotEmpty()
        if ((hasFocus || !isSearching) && hasEntries) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
            ) {
                if (matchingHistory.isNotEmpty()) {
                    item(key = "history_header") {
                        SectionHeader(
                            title = stringResource(R.string.search_recent),
                            trailing = {
                                TextButton(onClick = onClearHistory) {
                                    Text(text = stringResource(R.string.search_clear_history))
                                }
                            }
                        )
                    }
                    items(matchingHistory, key = { "history:$it" }) { entry ->
                        AuroraListItem(
                            modifier = Modifier.heightIn(min = 48.dp),
                            headline = entry,
                            onClick = { onRequestSearch(entry) },
                            trailing = {
                                IconButton(onClick = { onRemoveHistory(entry) }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_cancel),
                                        contentDescription = stringResource(
                                            R.string.search_remove_history,
                                            entry
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
                items(matchingSuggestions, key = { "suggestion:${it.title}" }) { suggestion ->
                    SearchSuggestionListItem(
                        searchSuggestEntry = suggestion,
                        onClick = { onRequestSearch(it) },
                        onAction = {
                            textFieldState.setTextAndPlaceCursorAtEnd(it.trim())
                            focusRequester.requestFocus()
                            keyboard?.show()
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun ListPane() {
        Scaffold(topBar = { SearchBar() }) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .imePadding()
                    .fillMaxSize()
            ) {
                InlineSuggestions()
                Box(modifier = Modifier.weight(1f)) {
                    when (results.loadState.refresh) {
                        is LoadState.Loading -> ContainedLoadingIndicator()

                        is LoadState.Error -> {
                            Placeholder(
                                painter = painterResource(R.drawable.ic_refresh),
                                message = stringResource(R.string.error),
                                actionLabel = stringResource(R.string.action_retry),
                                onAction = { results.retry() }
                            )
                        }

                        else -> {
                            if (isSearching && results.itemCount == 0) {
                                Placeholder(
                                    painter = painterResource(R.drawable.ic_disclaimer),
                                    message = stringResource(R.string.no_apps_available)
                                )
                            } else {
                                val listState = rememberLazyListState()
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(
                                        dimensionResource(R.dimen.spacing_medium)
                                    )
                                ) {
                                    items(
                                        count = results.itemCount,
                                        key = { index ->
                                            "${results.peek(index)?.packageName}:$index"
                                        }
                                    ) { index ->
                                        results[index]?.let { app ->
                                            LargeAppListItem(
                                                app = app,
                                                onClick = { showDetailPane(app.packageName) }
                                            )
                                        }
                                    }
                                }
                                ScrollHint(
                                    listState = listState,
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun DetailPane() {
        val packageName = scaffoldNavigator.currentDestination?.contentKey
        if (packageName != null) {
            AppDetailsScreen(
                packageName = packageName,
                viewModel = hiltViewModel<AppDetailsViewModel>(key = packageName),
                onNavigateTo = { destination ->
                    if (destination is Destination.AppDetails) {
                        showDetailPane(destination.packageName)
                    }
                },
                forceSinglePane = true
            )
        } else if (isSearching && results.itemCount > 0) {
            Placeholder(
                painter = painterResource(R.drawable.ic_round_search),
                message = stringResource(R.string.select_app_for_details)
            )
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = scaffoldNavigator,
        listPane = { AnimatedPane { ListPane() } },
        detailPane = { AnimatedPane { DetailPane() } }
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@PreviewScreenSizes
@Composable
private fun SearchScreenPreview(@PreviewParameter(AppPreviewProvider::class) app: App) {
    val apps = List(10) { index -> app.copy(packageName = "${app.packageName}.$index") }
    val results = MutableStateFlow(PagingData.from(apps)).collectAsLazyPagingItems()
    ScreenContent(results = results, history = listOf("Aurora Store", "Firefox"))
}
