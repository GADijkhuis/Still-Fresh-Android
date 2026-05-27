package com.stillfresh.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class BottomNavItem(
    val icon: ImageVector,
    val contentDescription: String
)

private val FAB_SIZE = 56.dp
private val BAR_HEIGHT = 56.dp

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
            .drawBehind {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val hr = 44.dp.toPx()   // horizontal radius (width of notch)
                val vr = hr * 0.7f      // vertical radius (depth of notch) — symmetric so endpoints stay at y=0

                val arcRect = Rect(
                    left = cx - hr,
                    top = -vr,
                    right = cx + hr,
                    bottom = vr
                )

                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(cx - hr, 0f)
                    arcTo(rect = arcRect, startAngleDegrees = 180f, sweepAngleDegrees = -180f, forceMoveTo = false)
                    lineTo(w, 0f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path = path, color = Color.White, style = Fill)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.take(2).forEachIndexed { index, item ->
                NavIconButton(item = item, selected = selectedTab == index, teal = teal) {
                    onTabSelected(index)
                }
            }

            // Space for FAB
            Spacer(modifier = Modifier.width(FAB_SIZE + 8.dp))

            items.drop(2).forEachIndexed { index, item ->
                val actualIndex = index + 2
                NavIconButton(item = item, selected = selectedTab == actualIndex, teal = teal) {
                    onTabSelected(actualIndex)
                }
            }
        }

        // FAB centered, overlapping the top of the bar
        FloatingActionButton(
            onClick = onAddClick,
            shape = CircleShape,
            containerColor = teal,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 3.dp
            ),
            modifier = Modifier
                .size(FAB_SIZE)
                .align(Alignment.TopCenter)
                .offset(y = (-FAB_SIZE / 2))
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add item",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun NavIconButton(
    item: BottomNavItem,
    selected: Boolean,
    teal: Color,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.contentDescription,
                tint = if (selected) teal else Color(0xFFB0B0B0),
                modifier = Modifier.size(24.dp)
            )
            if (selected) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(teal, CircleShape)
                )
            }
        }
    }
}
