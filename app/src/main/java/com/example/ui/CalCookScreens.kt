package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalCookApp(viewModel: CalCookViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val mealPlans by viewModel.mealPlans.collectAsStateWithLifecycle()
    val groceryItems by viewModel.groceryItems.collectAsStateWithLifecycle()
    val substitutions by viewModel.substitutions.collectAsStateWithLifecycle()
    val budgetCap by viewModel.budgetCap.collectAsStateWithLifecycle()

    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isScanningCalendar by viewModel.isScanningCalendar.collectAsStateWithLifecycle()
    val isGeneratingMealPlan by viewModel.isGeneratingMealPlan.collectAsStateWithLifecycle()
    val isGeneratingRecipe by viewModel.isGeneratingRecipe.collectAsStateWithLifecycle()
    val selectedRecipe by viewModel.selectedRecipe.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // State for navigation (representing our tabs)
    var currentTab by remember { mutableStateOf("Planner") } // Planner, Groceries, Budget, Creator, Settings
    var showOnboarding by remember { mutableStateOf(false) }

    // Onboarding auto-check: if profile has default values, let's show onboarding first!
    LaunchedEffect(userProfile) {
        if (userProfile.name == "Chef") {
            showOnboarding = true
        }
    }

    if (showOnboarding) {
        OnboardingScreen(
            onCompleted = { name, size, skill, diet, apps ->
                viewModel.completeOnboardingAndSetup(name, size, skill, diet, apps)
                showOnboarding = false
            }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = com.example.ui.theme.HighDensityNavBg,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = com.example.ui.theme.HighDensityBorder,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                ) {
                    listOf(
                        Triple("Planner", Icons.Default.CalendarToday, "Planner"),
                        Triple("Groceries", Icons.Default.ShoppingBag, "Groceries"),
                        Triple("Budget", Icons.Default.AttachMoney, "Budget"),
                        Triple("Creator", Icons.Default.RestaurantMenu, "AI Kitchen")
                    ).forEach { (tabId, icon, label) ->
                        NavigationBarItem(
                            selected = currentTab == tabId,
                            onClick = {
                                currentTab = tabId
                                // Close selected recipe view if we move tabs
                                if (selectedRecipe != null) {
                                    viewModel.createCustomRecipeWithAI("") // Reset selected
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = com.example.ui.theme.HighDensityCoffee,
                                unselectedIconColor = com.example.ui.theme.HighDensityMuted,
                                selectedTextColor = com.example.ui.theme.HighDensityCoffee,
                                unselectedTextColor = com.example.ui.theme.HighDensityMuted,
                                indicatorColor = com.example.ui.theme.HighDensityCoral
                            ),
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_tab_$tabId")
                        )
                    }
                }
            }
        ) { paddingVal ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingVal),
                color = MaterialTheme.colorScheme.background
            ) {
                // If a recipe is selected, show its detail screen
                if (selectedRecipe != null) {
                    val scaleServings by viewModel.recipeServings.collectAsStateWithLifecycle()
                    RecipeDetailScreen(
                        recipe = selectedRecipe!!,
                        userProfile = userProfile,
                        servings = scaleServings,
                        onClose = { viewModel.scanAndSyncCalendar() /* Close by rescan or reset selected */ },
                        onUpdateServings = { viewModel.updateRecipeServings(it) },
                        onSubstitute = { ingredient, meal -> 
                            viewModel.requestIngredientSubstitution(ingredient, meal)
                        },
                        mealPlans = mealPlans
                    )
                } else {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "TabTransition"
                    ) { tab ->
                        when (tab) {
                            "Planner" -> PlannerScreen(
                                viewModel = viewModel,
                                calendarEvents = calendarEvents,
                                mealPlans = mealPlans,
                                isScanning = isScanningCalendar,
                                isPlanning = isGeneratingMealPlan,
                                budgetCap = budgetCap,
                                profile = userProfile,
                                substitutions = substitutions,
                                onNavigateToTab = { currentTab = it }
                            )
                            "Groceries" -> GroceriesScreen(
                                viewModel = viewModel,
                                groceryItems = groceryItems,
                                profile = userProfile
                            )
                            "Budget" -> BudgetScreen(
                                viewModel = viewModel,
                                budgetCap = budgetCap,
                                profile = userProfile,
                                mealPlans = mealPlans
                            )
                            "Creator" -> CreatorScreen(
                                viewModel = viewModel,
                                profile = userProfile,
                                isGenerating = isGeneratingRecipe
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------ ONBOARDING SCREEN ------------------

@Composable
fun OnboardingScreen(onCompleted: (String, Int, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var sizeText by remember { mutableStateOf("2") }
    var skillLevel by remember { mutableStateOf("Intermediate") }
    var dietaryRestriction by remember { mutableStateOf("none") }
    
    // Kitchen equipment checkboxes
    var hasStove by remember { mutableStateOf(true) }
    var hasMicrowave by remember { mutableStateOf(true) }
    var hasOven by remember { mutableStateOf(true) }
    var hasSlowCooker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        // Brand Header Logo Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.RestaurantMenu,
                contentDescription = "Cal-Cook Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = "Welcome to Cal-Cook",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "The calendar-synced culinary planner. We analyze your day, scan schedules, find cooking gaps, and scale delicious meals.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Card containing fields
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Configure Your Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g. Alex Mercer") },
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_name_field"),
                    singleLine = true
                )

                // Household sizes (Servings scaling factor)
                OutlinedTextField(
                    value = sizeText,
                    onValueChange = { sizeText = it },
                    label = { Text("Household Size (For recipe scaling)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_size_field"),
                    singleLine = true
                )

                // Skill level
                Text("Your Cooking Skills", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Beginner", "Intermediate", "Advanced").forEach { level ->
                        val selected = skillLevel == level
                        Button(
                            onClick = { skillLevel = level },
                            colors = if (selected) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("skill_$level")
                        ) {
                            Text(level, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Dietary preference
                Text("Dietary Restrictions", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("none", "vegan", "keto", "gluten-free").forEach { diet ->
                        val selected = dietaryRestriction == diet
                        Button(
                            onClick = { dietaryRestriction = diet },
                            colors = if (selected) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("diet_$diet")
                        ) {
                            Text(diet.capitalize(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Owned Kitchen Equipment Checklist
                Text("Your Kitchen Equipment", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "Stove" to hasStove,
                        "Microwave" to hasMicrowave,
                        "Oven" to hasOven,
                        "Slow Cooker" to hasSlowCooker
                    ).forEach { (app, checked) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (app) {
                                        "Stove" -> hasStove = !hasStove
                                        "Microwave" -> hasMicrowave = !hasMicrowave
                                        "Oven" -> hasOven = !hasOven
                                        "Slow Cooker" -> hasSlowCooker = !hasSlowCooker
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    when (app) {
                                        "Stove" -> hasStove = !hasStove
                                        "Microwave" -> hasMicrowave = !hasMicrowave
                                        "Oven" -> hasOven = !hasOven
                                        "Slow Cooker" -> hasSlowCooker = !hasSlowCooker
                                    }
                                },
                                modifier = Modifier.testTag("eq_$app")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(app)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Submit buttons
        Button(
            onClick = {
                val finalName = name.ifBlank { "Alex Mercer" }
                val count = sizeText.toIntOrNull() ?: 2
                val appsList = mutableListOf<String>()
                if (hasStove) appsList.add("Stove")
                if (hasMicrowave) appsList.add("Microwave")
                if (hasOven) appsList.add("Oven")
                if (hasSlowCooker) appsList.add("Slow Cooker")
                val apps = appsList.joinToString(",")

                onCompleted(finalName, count, skillLevel, dietaryRestriction, apps)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboarding_complete_btn"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Sync, contentDescription = "Sync")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Link Calendar & Generate Recipes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ------------------ TAB 1: PLANNER SCREEN ------------------

@Composable
fun PlannerScreen(
    viewModel: CalCookViewModel,
    calendarEvents: List<CalendarEvent>,
    mealPlans: List<MealPlan>,
    isScanning: Boolean,
    isPlanning: Boolean,
    budgetCap: BudgetCap,
    profile: UserProfile,
    substitutions: List<Substitution>,
    onNavigateToTab: (String) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()) }
    val dateStr = dateFormatter.format(Date())
    val groceryItems by viewModel.groceryItems.collectAsStateWithLifecycle()

    val initials = remember(profile.name) {
        val parts = profile.name.trim().split("\\s+".toRegex())
        val res = if (parts.size >= 2) {
            "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}"
        } else {
            profile.name.take(2)
        }
        res.ifBlank { "JD" }
    }.uppercase()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(com.example.ui.theme.HighDensityBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High Density Header Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = com.example.ui.theme.HighDensityCoffee,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "${calendarEvents.size.coerceAtLeast(3)} blocks available today",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = com.example.ui.theme.HighDensityMuted
                )
            }

            // Sync Interactive Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(com.example.ui.theme.HighDensityPeach)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { viewModel.scanAndSyncCalendar() }
                    .testTag("refresh_calendar_btn"),
                contentAlignment = Alignment.Center
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = com.example.ui.theme.HighDensityCoffee
                    )
                } else {
                    Text(
                        text = initials,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.HighDensityCoffee,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Active State Banner
        if (isPlanning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.HighDensityCoral),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = com.example.ui.theme.HighDensityOnCoral
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "AI smart engine is scanning gaps and planning...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.HighDensityOnCoral
                    )
                }
            }
        }

        // Calendar Custom Visualizer Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAILY SCHEDULE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.HighDensityMuted,
                        letterSpacing = 1.sp
                    )
                    
                    // SYNCED Badge Pill
                    Box(
                        modifier = Modifier
                            .background(com.example.ui.theme.HighDensityCoral, RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SYNCED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = com.example.ui.theme.HighDensityOnCoral
                        )
                    }
                }

                // Custom timeline display bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(com.example.ui.theme.HighDensityProgressTrack)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // 25% Meeting Segment (Sky Blue)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.25f)
                                .background(com.example.ui.theme.HighDensityBlue)
                                .border(width = (0.5).dp, color = Color.White.copy(alpha = 0.5f))
                        )
                        // 15% Free weight gap
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.15f)
                        )
                        // 30% Deep Work block (Sky Blue)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.30f)
                                .background(com.example.ui.theme.HighDensityBlue)
                                .border(width = (0.5).dp, color = Color.White.copy(alpha = 0.5f))
                        )
                        // 20% Cook Slot (Lime highlighted slot)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.20f)
                                .background(com.example.ui.theme.HighDensityLime),
                            contentAlignment = Alignment.Center
                        ) {
                            // Centered small dynamic indicator bullet
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(com.example.ui.theme.HighDensityOnLime, CircleShape)
                            )
                        }
                        // 10% Final gap
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.10f)
                                .background(com.example.ui.theme.HighDensityBlue)
                        )
                    }
                }

                // Timeline Tick marks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("08:00", "12:00", "16:00", "20:00").forEach { tick ->
                        Text(
                            text = tick,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = com.example.ui.theme.HighDensityMuted
                        )
                    }
                }
            }
        }

        // Priority Meal Suggestion (Hero Card)
        val recommendedMeal = remember(mealPlans) {
            mealPlans.firstOrNull { !it.isCompleted } ?: mealPlans.firstOrNull()
        }

        if (recommendedMeal != null) {
            var recipeMatch: Recipe? = null
            val rList = viewModel.recipes.value
            recipeMatch = rList.firstOrNull { it.id == recommendedMeal.recipeId }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.HighDensityCoffee),
                shape = RoundedCornerShape(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Top-right Corner Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(com.example.ui.theme.HighDensityCoral, RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${recommendedMeal.prepCookTime}m Gap @ ${if (recommendedMeal.mealType == "Breakfast") "08:30" else if (recommendedMeal.mealType == "Lunch") "12:15" else "18:45"}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = com.example.ui.theme.HighDensityOnCoral
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "SMART RECOMMENDATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.HighDensityPeach,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = recommendedMeal.recipeTitle,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                lineHeight = 34.sp
                            )
                        }

                        // Mini indicator pills
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Quick Prep Pill with green bullet
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(100.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(com.example.ui.theme.HighDensityLime, CircleShape)
                                )
                                Text(
                                    text = "Quick Prep",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }

                            // Dynamic Cost Estimate pill
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(100.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$${String.format("%.2f", recipeMatch?.baseCost ?: 4.25)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }

                        // Big main Matcha cooking CTA
                        Button(
                            onClick = { recipeMatch?.let { viewModel.selectRecipe(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("start_cooking_btn_${recommendedMeal.mealType.lowercase()}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.example.ui.theme.HighDensityLime,
                                contentColor = com.example.ui.theme.HighDensityOnLime
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Start Cooking Now",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Quick Stats Row
        val totalUnboughtGrocery = groceryItems.count { !it.isBought && !it.inPantry }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats Card 1: Daily Budget
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTab("Budget") },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "DAILY BUDGET",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.HighDensityMuted,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$${String.format("%.2f", budgetCap.currentDailySpend)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = com.example.ui.theme.HighDensityCoffee
                        )
                        Text(
                            text = "/ $${budgetCap.dailyAllocation.toInt()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.HighDensityMuted,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    // Progress Track Bar
                    val usageFrac = (budgetCap.currentDailySpend / budgetCap.dailyAllocation).toFloat().coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(com.example.ui.theme.HighDensityProgressTrack)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(usageFrac)
                                .background(com.example.ui.theme.HighDensityGold)
                        )
                    }
                }
            }

            // Stats Card 2: Grocery List
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToTab("Groceries") },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "GROCERY LIST",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.HighDensityMuted,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$totalUnboughtGrocery Items",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = com.example.ui.theme.HighDensityCoffee
                        )

                        // Arrow action button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(com.example.ui.theme.HighDensityPeach, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "→",
                                color = com.example.ui.theme.HighDensityCoffee,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Text(
                        text = "2 items expiring soon",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.HighDensityMuted
                    )
                }
            }
        }

        // Section header with Trigger to Re-Generate meal plan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Today's Meal Schedule",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = com.example.ui.theme.HighDensityCoffee
            )
            TextButton(
                onClick = { viewModel.generateSmartMealPlan() },
                modifier = Modifier.testTag("replan_btn")
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = com.example.ui.theme.HighDensityCoffee
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Re-Plan Meals",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.example.ui.theme.HighDensityCoffee
                )
            }
        }

        // Dynamic schedule list (replaces original cards)
        if (mealPlans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, com.example.ui.theme.HighDensityBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No meals planned. Tap Re-Plan to align calendar gaps.",
                    color = com.example.ui.theme.HighDensityMuted
                )
            }
        } else {
            mealPlans.onEach { meal ->
                var recipeMatch: Recipe? = null
                val rList = viewModel.recipes.value
                recipeMatch = rList.firstOrNull { it.id == meal.recipeId }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { recipeMatch?.let { viewModel.selectRecipe(it) } }
                        .testTag("meal_card_${meal.mealType.lowercase()}"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, com.example.ui.theme.HighDensityBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = meal.isCompleted,
                                    onCheckedChange = { viewModel.toggleMealCompletion(meal) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = com.example.ui.theme.HighDensityCoffee,
                                        uncheckedColor = com.example.ui.theme.HighDensityMuted
                                    ),
                                    modifier = Modifier.testTag("checkbox_${meal.mealType.lowercase()}")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = meal.mealType.uppercase(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = com.example.ui.theme.HighDensityMuted,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = meal.recipeTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = com.example.ui.theme.HighDensityCoffee,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            // Prep time limit badge
                            Card(
                                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.HighDensityPeach),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${meal.prepCookTime}m prep",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = com.example.ui.theme.HighDensityCoffee,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Warnings
                        meal.statusWarning?.let { warn ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFF2EC), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    warn,
                                    fontSize = 11.sp,
                                    color = Color(0xFFD32F2F),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Ingredients quick preview
                        recipeMatch?.let { rec ->
                            Spacer(modifier = Modifier.height(8.dp))
                            val items = rec.ingredientsList.split(",").map { it.split(":").firstOrNull() ?: "" }.take(4).joinToString(", ")
                            Text(
                                text = "Ingredients: $items...",
                                fontSize = 11.sp,
                                color = com.example.ui.theme.HighDensityMuted
                            )
                        }

                        // Action help footer
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                "View directions & cooking timer →",
                                style = MaterialTheme.typography.bodySmall,
                                color = com.example.ui.theme.HighDensityCoffee,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------ TAB 2: GROCERIES SCREEN ------------------

@Composable
fun GroceriesScreen(
    viewModel: CalCookViewModel,
    groceryItems: List<GroceryItem>,
    profile: UserProfile
) {
    var customName by remember { mutableStateOf("") }
    var customQty by remember { mutableStateOf("1") }
    var customUnit by remember { mutableStateOf("pieces") }
    var customCategory by remember { mutableStateOf("Pantry") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shopping lists Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Dynamic Grocery List",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Quantities scaled automatically for household size (${profile.householdSize})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // Add custom purchase item trigger
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add Custom Item to List", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        placeholder = { Text("Apples") },
                        modifier = Modifier.weight(2f).testTag("custom_grocery_name"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customQty,
                        onValueChange = { customQty = it },
                        placeholder = { Text("4") },
                        modifier = Modifier.weight(1f).testTag("custom_grocery_qty"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = customUnit,
                        onValueChange = { customUnit = it },
                        placeholder = { Text("pcs") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Category:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    listOf("Produce", "Dairy", "Meat & Seafood", "Pantry").forEach { cat ->
                        val sel = customCategory == cat
                        Button(
                            onClick = { customCategory = cat },
                            colors = if (sel) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(cat, fontSize = 9.sp)
                        }
                    }
                }
                Button(
                    onClick = {
                        if (customName.isNotBlank()) {
                            viewModel.addGroceryItem(
                                name = customName,
                                quantity = customQty.toDoubleOrNull() ?: 1.0,
                                unit = customUnit.ifBlank { "pieces" },
                                category = customCategory,
                                cost = 2.0
                            )
                            customName = ""
                            customQty = "1"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("add_custom_grocery_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Ingredient", fontSize = 12.sp)
                }
            }
        }

        // Active Shopping List vs Pantry items
        val (pantryItems, activeShopping) = groceryItems.partition { it.inPantry }

        // SHOPPING LIST AISLES
        Text("Active Shopping List", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (activeShopping.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("All ingredients are in your kitchen pantry! You are ready to cook.")
            }
        } else {
            // Group by Aisle category
            val groupedByAisle = activeShopping.groupBy { it.category }
            groupedByAisle.forEach { (aisle, items) ->
                Text(aisle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                items.forEach { grocery ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = grocery.isBought,
                                onCheckedChange = { viewModel.toggleGroceryBought(grocery) },
                                modifier = Modifier.testTag("grocery_buy_${grocery.id}")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = grocery.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${grocery.quantity} ${grocery.unit} • Est $${String.format("%.2f", grocery.costEstimate)}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Pantry mark toggle
                            TextButton(
                                onClick = { viewModel.toggleGroceryInPantry(grocery) },
                                modifier = Modifier.testTag("grocery_pantry_${grocery.id}")
                            ) {
                                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("In Pantry", fontSize = 10.sp)
                            }
                            IconButton(onClick = { viewModel.deleteGrocery(grocery.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }

        // PANTRY SECTION
        Text("Your Pantry Inventory", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (pantryItems.isEmpty()) {
            Text("Pantry is currently empty. Tap 'In Pantry' on ingredients you already have to remove them from shopping.", fontSize = 11.sp, color = Color.Gray)
        } else {
            pantryItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(item.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                    TextButton(onClick = { viewModel.toggleGroceryInPantry(item) }) {
                        Text("Add to shop", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // One-tap checkout button
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.DeliveryDining, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Order via Amazon Fresh / Instacart", fontWeight = FontWeight.Bold)
        }
    }
}

// ------------------ TAB 3: BUDGET SCREEN ------------------

@Composable
fun BudgetScreen(
    viewModel: CalCookViewModel,
    budgetCap: BudgetCap,
    profile: UserProfile,
    mealPlans: List<MealPlan>
) {
    var monthlyInput by remember { mutableStateOf(profile.monthlyBudget.toString()) }
    var dailyInput by remember { mutableStateOf(budgetCap.dailyAllocation.toString()) }
    var splurgeMode by remember { mutableStateOf(profile.splurgeMode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Budget Management", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)

        // Real-Time track meters
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Monthly Allocation Target", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Spent: $${String.format("%.2f", budgetCap.actualMonthlySpend)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("Limit: $${String.format("%.2f", profile.monthlyBudget)}", fontSize = 13.sp, color = Color.Gray)
                }
                
                // Progress Bar
                val monthlyProg = (budgetCap.actualMonthlySpend / profile.monthlyBudget).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { monthlyProg },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color = if (monthlyProg > 0.9f) Color.Red else MaterialTheme.colorScheme.primary
                )

                Divider()

                // Daily Allocation Target
                Text("Today's Food Costs", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Today's Spend: $${String.format("%.2f", budgetCap.currentDailySpend)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("Day Cap: $${String.format("%.2f", budgetCap.dailyAllocation)}", fontSize = 13.sp, color = Color.Gray)
                }

                // Today Progress Bar
                val dailyProg = (budgetCap.currentDailySpend / budgetCap.dailyAllocation).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { dailyProg },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color = if (dailyProg > 1.0f) Color.Red else MaterialTheme.colorScheme.primary
                )

                if (dailyProg > 1.0f && !splurgeMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Day spent threshold reached! Swap premium ingredients or enable Splurge Mode.", fontSize = 10.sp, color = Color.Red)
                    }
                }
            }
        }

        // Budget Adjustments Settings
        Text("Configure Budget Parameters", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Monthly input field
                OutlinedTextField(
                    value = monthlyInput,
                    onValueChange = { monthlyInput = it },
                    label = { Text("Monthly Target Budget ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("monthly_budget_input"),
                    singleLine = true
                )

                // Daily Allocated limit field
                OutlinedTextField(
                    value = dailyInput,
                    onValueChange = { dailyInput = it },
                    label = { Text("Daily Allocation Limit ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("daily_budget_input"),
                    singleLine = true
                )

                // Splurge Mode toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Splurge Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Allows overspending on one premium meal and automatically compensates budget on other days.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = splurgeMode,
                        onCheckedChange = { splurgeMode = it },
                        modifier = Modifier.testTag("splurge_toggle")
                    )
                }

                Button(
                    onClick = {
                        val mVal = monthlyInput.toDoubleOrNull() ?: 450.0
                        val dVal = dailyInput.toDoubleOrNull() ?: 18.0
                        viewModel.updateBudgetSettings(mVal, dVal, splurgeMode)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_budget_btn")
                ) {
                    Text("Save Parameters")
                }
            }
        }

        // Cost-per-serving breakdown
        Text("Cost-Per-Serving Estimates of Plan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        mealPlans.forEach { meal ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(meal.mealType, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(meal.recipeTitle, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(220.dp))
                }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = "$${String.format("%.2f", (viewModel.recipes.value.firstOrNull { it.id == meal.recipeId }?.baseCost ?: 3.50))}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ------------------ TAB 4: CREATOR SCREEN ------------------

@Composable
fun CreatorScreen(
    viewModel: CalCookViewModel,
    profile: UserProfile,
    isGenerating: Boolean
) {
    var recipePrompt by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("AI Generation Kitchen", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Let Gemini AI custom craft recipes tailored perfectly to your kitchen equipment (${profile.appliancesOwned}) and diet preferences (${profile.dietaryRestrictions}).",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = recipePrompt,
                    onValueChange = { recipePrompt = it },
                    label = { Text("What are you craving today?") },
                    placeholder = { Text("e.g. Vegetarian high protein bowl under 10 minutes") },
                    modifier = Modifier.fillMaxWidth().height(110.dp).testTag("ai_recipe_input"),
                    maxLines = 4
                )

                Button(
                    onClick = {
                        if (recipePrompt.isNotBlank()) {
                            viewModel.createCustomRecipeWithAI(recipePrompt)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("ai_generate_recipe_btn"),
                    enabled = !isGenerating
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stirring ingredients...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Customized Recipe")
                    }
                }
            }
        }

        // Custom recipe history and favorites
        Text("Recipe Catalog", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        val allRecipes by viewModel.recipes.collectAsStateWithLifecycle()
        allRecipes.forEach { recipe ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectRecipe(recipe) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(recipe.source.take(2).uppercase(), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(recipe.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${recipe.prepTime + recipe.cookTime}m • ${recipe.calories} kcal • Source: ${recipe.source}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                }
            }
        }
    }
}

// ------------------ RECIPE DETAIL SCREEN ------------------

@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    userProfile: UserProfile,
    servings: Int,
    onClose: () -> Unit,
    onUpdateServings: (Int) -> Unit,
    onSubstitute: (String, MealPlan) -> Unit,
    mealPlans: List<MealPlan>
) {
    // Interactive cooking active instructions & timer
    var activeTimerSeconds by remember { mutableStateOf(recipe.cookTime * 60) }
    var timerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(timerRunning, activeTimerSeconds) {
        if (timerRunning && activeTimerSeconds > 0) {
            delay(1000)
            activeTimerSeconds -= 1
        } else if (activeTimerSeconds == 0) {
            timerRunning = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).testTag("close_recipe_btn")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Close")
            }
            Text(
                "Recipe Details",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                Text(recipe.source, modifier = Modifier.padding(4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Card Recipe Title block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(recipe.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(recipe.description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Prep: ${recipe.prepTime} min", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Cook: ${recipe.cookTime} min", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${recipe.calories} kcal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Est. cost: $${String.format("%.2f", recipe.baseCost)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Diet labels
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    recipe.dietaryFlags.split(",").filter { it.isNotBlank() }.forEach { flag ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Text(
                                flag,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Active Cooking timer view
        if (recipe.cookTime > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Recipe Active Timer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        val min = activeTimerSeconds / 60
                        val sec = activeTimerSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", min, sec),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { timerRunning = !timerRunning },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.testTag("recipe_timer_trigger")
                        ) {
                            Text(if (timerRunning) "Pause" else "Start Timer", fontSize = 11.sp)
                        }
                        TextButton(onClick = { activeTimerSeconds = recipe.cookTime * 60; timerRunning = false }) {
                            Text("Reset", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Servings Adjuster
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Scale Servings Size", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Scaling base quantities", fontSize = 10.sp, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onUpdateServings(servings - 1) }) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null)
                }
                Text("$servings", fontWeight = FontWeight.Black, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = { onUpdateServings(servings + 1) }) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                }
            }
        }

        // INGREDIENTS WITH SUBSTITUTION TRIGGER BUTTON
        Text("Ingredients Required", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val scaleRatio = servings.toDouble() / userProfile.householdSize.toDouble()
                
                recipe.ingredientsList.split(",").filter { it.isNotBlank() }.forEach { ing ->
                    val parts = ing.trim().split(":")
                    if (parts.size >= 3) {
                        val name = parts[0]
                        val rawQty = parts[1].toDoubleOrNull() ?: 1.0
                        val unit = parts[2]
                        val scaledQty = rawQty * scaleRatio

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${String.format("%.2f", scaledQty)} $unit", fontSize = 11.sp, color = Color.Gray)
                            }
                            
                            // Context substitution triggers
                            TextButton(
                                onClick = {
                                    // Map this action back to matching meal unit if any
                                    val matchMeal = mealPlans.firstOrNull { it.recipeId == recipe.id }
                                        ?: MealPlan(date="", mealType="Dinner", recipeId=recipe.id, recipeTitle=recipe.title, prepCookTime=recipe.prepTime+recipe.cookTime)
                                    onSubstitute(name, matchMeal)
                                },
                                modifier = Modifier.testTag("substitute_btn_${name.replace(" ", "_")}")
                            ) {
                                Icon(Icons.Default.SwapCalls, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Context Swap", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // INSTRUCTIONS STEP BY STEP
        Text("Cooking Directions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                recipe.instructionsList.split("|").filter { it.isNotBlank() }.mapIndexed { index, step ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(step.trim(), fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
