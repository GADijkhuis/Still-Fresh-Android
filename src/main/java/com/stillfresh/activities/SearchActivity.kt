package com.stillfresh.activities

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stillfresh.components.StillFreshIconButton
import com.stillfresh.components.StillFreshProduct
import com.stillfresh.components.StillFreshTextFieldInvertedColors
import com.stillfresh.dataclasses.openfoodfacts.OpenFoodFactsProduct
import com.stillfresh.handlers.OpenFoodFactsHandler
import kotlinx.coroutines.launch


object SearchActivity : ViewModel() {
    @Composable
    fun SearchView() {
        var searchValue by remember { mutableStateOf("") }
        var searchResults: Array<OpenFoodFactsProduct> by remember { mutableStateOf(arrayOf()) }

        fun updateSearchResults() {
            if (searchValue.replace(" ","") == "") {
                searchResults = arrayOf()
                return
            }

            viewModelScope.launch {
                val data = OpenFoodFactsHandler.getSearchResultsByName(searchValue)

                if (data == null) {
                    searchResults = arrayOf()
                    return@launch
                }

                searchResults = data.products
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(20.dp, 100.dp),
            verticalArrangement = Arrangement.Top,
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
                    icon = Icons.Outlined.Search,
                    contentDescription = "Search",
                    onClick = {
                        updateSearchResults()
                    }
                )


            }

            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                searchResults.iterator().forEach {
                    Log.i("Products", it.toString())

                    StillFreshProduct(product = it)
                }
            }

        }
    }
}
