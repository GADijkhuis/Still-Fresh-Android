package com.stillfresh.activities

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stillfresh.components.StillFreshIconButton
import com.stillfresh.components.StillFreshProduct
import com.stillfresh.components.StillFreshTextFieldInvertedColors
import com.stillfresh.dataclasses.openfoodfacts.OpenFoodFactsProduct
import com.stillfresh.enums.SearchLoadingState
import com.stillfresh.handlers.OpenFoodFactsHandler
import kotlinx.coroutines.launch


object SearchActivity : ViewModel() {
    @Composable
    fun SearchView() {
        var searchLoadingState by remember { mutableStateOf(SearchLoadingState.NOT_SEARCHED) }
        var searchValue by remember { mutableStateOf("") }
        var searchResults: Array<OpenFoodFactsProduct> by remember { mutableStateOf(arrayOf()) }

        fun updateSearchResults() {
            if (searchValue.replace(" ","") == "") {
                searchLoadingState = SearchLoadingState.NOT_SEARCHED
                searchResults = arrayOf()
                return
            }

            searchLoadingState = SearchLoadingState.LOADING

            viewModelScope.launch {
                val data = OpenFoodFactsHandler.getSearchResultsByName(searchValue)

                if (data == null) {
                    searchLoadingState = SearchLoadingState.ERROR
                    searchResults = arrayOf()
                    return@launch
                }


                if (data.count == 0) {
                    searchLoadingState = SearchLoadingState.NO_RESULTS
                    searchResults = arrayOf()
                    return@launch
                }

                searchResults = data.products
                searchLoadingState = SearchLoadingState.SEARCHING_COMPLETE
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(20.dp, 100.dp, 20.dp, 70.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StillFreshTextFieldInvertedColors(
                    value = searchValue,
                    onValueChange = {
                        searchValue = it
                    },
                    label = "Search products",
                    modifier = Modifier.weight(1f)
                )
                StillFreshIconButton(
                    enabled = (searchLoadingState != SearchLoadingState.LOADING),
                    icon = Icons.Outlined.Search,
                    contentDescription = "Search",
                    onClick = {
                        updateSearchResults()
                    }
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                when (searchLoadingState) {
                    SearchLoadingState.NOT_SEARCHED -> {
                        Text(
                            text = "Start searching products",
                            color = Color.Gray,
                            fontSize = 12.sp,
                        )
                    }
                    SearchLoadingState.LOADING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.width(40.dp),
                            color = Color(0xFF70B9BE),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                    SearchLoadingState.NO_RESULTS -> {
                        Text(
                            text = "No products found. Try another name for the product",
                            color = Color.Gray,
                            fontSize = 12.sp,
                        )
                    }
                    SearchLoadingState.SEARCHING_COMPLETE -> {
                        searchResults.iterator().forEach {
                            Log.i("Products", it.toString())

                            StillFreshProduct(product = it)
                        }
                    }
                    else -> {
                        Text(
                            text = "An error occured while searching for products. Wait a few seconds and try again",
                            color = Color.Gray,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

        }
    }
}
