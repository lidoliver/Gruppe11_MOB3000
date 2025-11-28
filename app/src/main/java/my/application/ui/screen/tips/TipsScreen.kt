package my.application.ui.screen.tips

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

data class TipCategory(
    val title: String,
    val icon: ImageVector,
    val tips: List<Tip>,
    val color: Color
)

data class Tip(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsScreen(navController: NavController) {
    val categories = remember {
        listOf(
            TipCategory(
                title = "Frisbee Golf Basics",
                icon = Icons.Filled.Sports,
                color = Color(0xFF4CAF50),
                tips = listOf(
                    Tip(
                        title = "Grip Techniques",
                        description = "Start with the power grip for drives: place your thumb on top of the disc, fingers underneath gripping the rim. For shorter shots, use a fan grip with fingers spread across the underside of the disc.",
                        icon = Icons.Filled.PanTool
                    ),
                    Tip(
                        title = "Stance & Balance",
                        description = "Keep your feet shoulder-width apart and your weight balanced. For right-handed backhand throws, step with your left foot forward. Maintain a stable base throughout your throw.",
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk
                    ),
                    Tip(
                        title = "Follow Through",
                        description = "Always complete your throwing motion. Your arm should follow through across your body, and your weight should shift from back foot to front foot. This maximizes distance and accuracy.",
                        icon = Icons.AutoMirrored.Filled.TrendingUp
                    ),
                    Tip(
                        title = "Disc Selection",
                        description = "Start with mid-range discs for practice. Putters are great for short, controlled shots. Drivers are for experienced players seeking maximum distance.",
                        icon = Icons.Filled.Album
                    )
                )
            ),
            TipCategory(
                title = "Advanced Techniques",
                icon = Icons.Filled.EmojiEvents,
                color = Color(0xFFFF9800),
                tips = listOf(
                    Tip(
                        title = "Wind Management",
                        description = "Throw into headwinds with overstable discs and a lower release angle. Use understable discs with tailwinds. Adjust your aim based on crosswinds.",
                        icon = Icons.Filled.Air
                    ),
                    Tip(
                        title = "Hyzer & Anhyzer",
                        description = "Hyzer: angle the disc's outer edge down for a curving finish left (right-handed). Anhyzer: angle outer edge up for curves right. Master both for navigating obstacles.",
                        icon = Icons.Filled.Autorenew
                    ),
                    Tip(
                        title = "Putting Consistency",
                        description = "Develop a pre-putt routine. Focus on the chains, not the basket. Use a push putt for distances under 20 feet, spin putt for longer attempts. Practice daily!",
                        icon = Icons.Filled.GpsFixed
                    ),
                    Tip(
                        title = "Course Strategy",
                        description = "Play for position, not always distance. Sometimes a safe lay-up is better than a risky shot. Study the course layout and plan your route before each hole.",
                        icon = Icons.Filled.Map
                    )
                )
            ),
            TipCategory(
                title = "App Features",
                icon = Icons.Filled.PhoneAndroid,
                color = Color(0xFF2196F3),
                tips = listOf(
                    Tip(
                        title = "Track Your Rounds",
                        description = "Use the scorecard feature to keep track of your game. You can log scores for multiple players, track your progress over time, and see detailed statistics.",
                        icon = Icons.Filled.Scoreboard
                    ),
                    Tip(
                        title = "Find Courses",
                        description = "Navigate to the Map screen to discover disc golf courses near you. Tap on any course marker to see details, ratings, and directions.",
                        icon = Icons.Filled.LocationOn
                    ),
                    Tip(
                        title = "Join a Club",
                        description = "Connect with other players by joining or creating a club. Organize tournaments, share course recommendations, and compete on leaderboards.",
                        icon = Icons.Filled.Group
                    ),
                    Tip(
                        title = "Track Statistics",
                        description = "View your player stats to see your average score, best rounds, and improvement over time. Use this data to identify areas for improvement.",
                        icon = Icons.Filled.Analytics
                    ),
                    Tip(
                        title = "Stay Connected",
                        description = "Add friends to compare scores and challenge each other. Use the messaging feature to coordinate games and share tips with your disc golf community.",
                        icon = Icons.Filled.People
                    )
                )
            ),
            TipCategory(
                title = "Etiquette & Rules",
                icon = Icons.AutoMirrored.Filled.Rule,
                color = Color(0xFF9C27B0),
                tips = listOf(
                    Tip(
                        title = "Course Courtesy",
                        description = "Wait for the group ahead to clear the landing zone before throwing. Let faster groups play through. Always be respectful and friendly to other players.",
                        icon = Icons.Filled.EmojiPeople
                    ),
                    Tip(
                        title = "Playing Order",
                        description = "The player farthest from the basket throws first. On the tee, the player with the best score on the previous hole throws first (honors).",
                        icon = Icons.AutoMirrored.Filled.Sort
                    ),
                    Tip(
                        title = "Out of Bounds",
                        description = "If your disc lands out of bounds (OB), take a one-stroke penalty and play from the point where the disc crossed into OB, or from the designated drop zone.",
                        icon = Icons.Filled.Warning
                    ),
                    Tip(
                        title = "Care for Courses",
                        description = "Always clean up after yourself. Don't damage trees or vegetation. Stay on marked paths when possible. Help maintain the course by picking up litter.",
                        icon = Icons.Filled.Eco
                    )
                )
            )
        )
    }

    var selectedTabIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tips & Tricks",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row for categories
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp
            ) {
                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(category.title) },
                        icon = { Icon(category.icon, contentDescription = null) }
                    )
                }
            }

            // Content for selected category
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(categories[selectedTabIndex].tips) { tip ->
                    TipCard(
                        tip = tip,
                        categoryColor = categories[selectedTabIndex].color
                    )
                }
            }
        }
    }
}

@Composable
fun TipCard(
    tip: Tip,
    categoryColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        categoryColor.copy(alpha = 0.7f),
                                        categoryColor
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tip.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = tip.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Show less" else "Show more",
                    tint = categoryColor
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        color = categoryColor.copy(alpha = 0.3f),
                        thickness = 2.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = tip.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
            }
        }
    }
}

