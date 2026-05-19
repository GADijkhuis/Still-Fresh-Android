package com.stillfresh.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class BottomNavItem(
    val icon: ImageVector,
    val contentDescription: String
)

@Composable
fun HomeBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onAddClick: () -> Unit = {},
    teal: Color = Color(0xFF70B9BE)
) {
    val items = listOf(
        BottomNavItem(Icons.Outlined.Home, "Home"),
        BottomNavItem(Icons.Outlined.Search, "Search"),
        BottomNavItem(Icons.Outlined.Notifications, "Alerts"),
        BottomNavItem(Icons.Outlined.Person, "Profile")
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // The nav bar row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, bottom = 16.dp)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left two icons
            items.take(2).forEachIndexed { index, item ->
                IconButton(
                    onClick = { onTabSelected(index) }
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.contentDescription,
                        tint = if (selectedTab == index) teal else Color(0xFFB0B0B0),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Spacer for the FAB
            Spacer(modifier = Modifier.width(56.dp))

            // Right two icons
            items.drop(2).forEachIndexed { index, item ->
                val actualIndex = index + 2
                IconButton(
                    onClick = { onTabSelected(actualIndex) }
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.contentDescription,
                        tint = if (selectedTab == actualIndex) teal else Color(0xFFB0B0B0),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // FAB centered and overlapping the top
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)
        ) {
            FloatingActionButton(
                onClick = onAddClick,
                shape = CircleShape,
                containerColor = Color(0xFF2D3436),
                contentColor = Color.White,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add item",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
