/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.blacklist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.aurora.store.R
import com.aurora.store.compose.composable.SectionHeader
import com.aurora.store.data.model.SearchFilter

@Composable
fun SearchFilterSection(
    filter: SearchFilter,
    isAnonymous: Boolean,
    onFilter: (SearchFilter) -> Unit
) {
    val ratingLabels = stringArrayResource(R.array.filterRatingLabels)
    val ratingValues = stringArrayResource(R.array.filterRatingValues)
    val downloadLabels = stringArrayResource(R.array.filterDownloadsLabels)
    val downloadValues = stringArrayResource(R.array.filterDownloadsValues)
    Column {
        SectionHeader(title = stringResource(R.string.search_filters_title))
        Text(
            text = stringResource(R.string.search_filters_description),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_medium))
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.spacing_medium)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            SearchFilterMenu(
                label = stringResource(R.string.action_filter_rating),
                labels = ratingLabels,
                values = ratingValues,
                selectedIndex = ratingValues.indexOfFirst { it.toFloat() == filter.minRating },
                onSelect = { onFilter(filter.copy(minRating = it.toFloat())) }
            )
            SearchFilterMenu(
                label = stringResource(R.string.app_info_downloads),
                labels = downloadLabels,
                values = downloadValues,
                selectedIndex = downloadValues.indexOfFirst { it.toLong() == filter.minInstalls },
                onSelect = { onFilter(filter.copy(minInstalls = it.toLong())) }
            )
            FilterChip(
                selected = filter.isFree,
                onClick = { onFilter(filter.copy(isFree = !filter.isFree)) },
                label = { Text(text = stringResource(R.string.details_free)) }
            )
            FilterChip(
                selected = filter.noAds,
                onClick = { onFilter(filter.copy(noAds = !filter.noAds)) },
                label = { Text(text = stringResource(R.string.action_filter_no_ads)) }
            )
            if (!isAnonymous) {
                FilterChip(
                    selected = filter.noGMS,
                    onClick = { onFilter(filter.copy(noGMS = !filter.noGMS)) },
                    label = { Text(text = stringResource(R.string.action_filter_no_gms)) }
                )
            }
        }
        if (isAnonymous) {
            Text(
                text = stringResource(R.string.search_filters_anonymous),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.spacing_medium),
                    vertical = dimensionResource(R.dimen.spacing_small)
                )
            )
        }
    }
}

@Composable
private fun SearchFilterMenu(
    label: String,
    labels: Array<String>,
    values: Array<String>,
    selectedIndex: Int,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selectedIndex > 0,
            onClick = { expanded = true },
            label = {
                Text(
                    text = if (selectedIndex > 0) {
                        stringResource(
                            R.string.search_filter_value,
                            label,
                            labels[selectedIndex].trim()
                        )
                    } else {
                        label
                    }
                )
            },
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_drop_down),
                    contentDescription = null
                )
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            labels.zip(values).forEachIndexed { index, (label, value) ->
                DropdownMenuItem(
                    text = { Text(text = label) },
                    leadingIcon = {
                        if (index == selectedIndex) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null
                            )
                        }
                    },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
