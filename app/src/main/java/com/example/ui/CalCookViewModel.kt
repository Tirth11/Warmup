package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class CalCookViewModel(application: Application) : AndroidViewModel(application) {

    private val database = CalCookDatabase.getInstance(application)
    private val repository = CalCookRepository(database.dao)

    // Expose flows from Repository
    val calendarEvents: StateFlow<List<CalendarEvent>> = repository.calendarEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recipes: StateFlow<List<Recipe>> = repository.recipes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mealPlans: StateFlow<List<MealPlan>> = repository.allMealPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groceryItems: StateFlow<List<GroceryItem>> = repository.groceryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val substitutions: StateFlow<List<Substitution>> = repository.substitutions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .map { it ?: UserProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    val budgetCap: StateFlow<BudgetCap> = repository.budgetCap
        .map { it ?: BudgetCap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetCap())

    // UI Local State Transitions
    private val _isGeneratingMealPlan = MutableStateFlow(false)
    val isGeneratingMealPlan: StateFlow<Boolean> = _isGeneratingMealPlan.asStateFlow()

    private val _isScanningCalendar = MutableStateFlow(false)
    val isScanningCalendar: StateFlow<Boolean> = _isScanningCalendar.asStateFlow()

    private val _isGeneratingRecipe = MutableStateFlow(false)
    val isGeneratingRecipe: StateFlow<Boolean> = _isGeneratingRecipe.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe.asStateFlow()

    // Servings scaling selection
    private val _recipeServings = MutableStateFlow(2)
    val recipeServings: StateFlow<Int> = _recipeServings.asStateFlow()

    init {
        // Initialize database default entries
        viewModelScope.launch {
            try {
                repository.prepopulateDataIfNeeded()
            } catch (e: Exception) {
                Log.e("CalCookViewModel", "Prepopulation failed", e)
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun selectRecipe(recipe: Recipe) {
        _selectedRecipe.value = recipe
        // Grab original profile serving size as default
        _recipeServings.value = userProfile.value.householdSize
    }

    fun updateRecipeServings(servings: Int) {
        _recipeServings.value = servings.coerceIn(1, 12)
    }

    // 1. Calendar Scanner & Replanner
    fun scanAndSyncCalendar() {
        viewModelScope.launch {
            _isScanningCalendar.value = true
            _statusMessage.value = "Establishing handshake with calendars..."
            delay(1500)
            
            // Randomly insert or reset events to demonstrate real-time changes
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            val baseTime = today.timeInMillis

            repository.clearCalendarEvents()

            val randomcommuteText = if (Math.random() > 0.5) "Busy Commute" else "Traveling"
            val updatedEvents = listOf(
                CalendarEvent(
                    title = "Scanned: Morning Gym Runner Session",
                    startTime = baseTime + (7 * 60) * 60 * 1000,
                    endTime = baseTime + (8 * 0) * 60 * 1000,
                    status = "Free"
                ),
                CalendarEvent(
                    title = "Scanned: Emergency Standup Meeting",
                    startTime = baseTime + (9 * 60) * 60 * 1000,
                    endTime = baseTime + (10 * 60) * 60 * 1000,
                    status = "Busy"
                ),
                CalendarEvent(
                    title = "Scanned: Planning & Retrospective",
                    startTime = baseTime + (11 * 60) * 60 * 1000,
                    endTime = baseTime + (11 * 60 + 50) * 60 * 1000, // 50m busy
                    status = "Busy"
                ),
                CalendarEvent(
                    title = "Scanned: 10-Min Free Lunch",
                    startTime = baseTime + (12 * 60) * 60 * 1000,
                    endTime = baseTime + (12 * 60 + 10) * 60 * 1000, // strictly 10 mins!
                    status = "Free"
                ),
                CalendarEvent(
                    title = "Scanned: Code Review Sync",
                    startTime = baseTime + (13 * 60 + 30) * 60 * 1000,
                    endTime = baseTime + (15 * 60) * 60 * 1000,
                    status = "Busy"
                ),
                CalendarEvent(
                    title = "Scanned: Travelling to Head Office",
                    startTime = baseTime + (15 * 60 + 45) * 60 * 1000,
                    endTime = baseTime + (16 * 60 + 15) * 60 * 1000,
                    status = "Traveling"
                ),
                CalendarEvent(
                    title = "Scanned: Evening Cooking Block",
                    startTime = baseTime + (18 * 60 + 15) * 60 * 1000,
                    endTime = baseTime + (19 * 60 + 45) * 60 * 1000, // 90 mins free!
                    status = "Free"
                )
            )

            repository.insertCalendarEvents(updatedEvents)
            _isScanningCalendar.value = false
            _statusMessage.value = "Calendar synced! Re-evaluating time windows for meals..."
            
            // Automatically re-plan when calendar changes mid-day
            generateSmartMealPlan()
        }
    }

    // 2. Meal Planning Engine
    fun generateSmartMealPlan() {
        viewModelScope.launch {
            _isGeneratingMealPlan.value = true
            _statusMessage.value = "Analyzing available cooking slots..."
            
            // Find free time slots in the calendar to schedule meals
            val events = calendarEvents.value
            val freeSlots = events.filter { it.status == "Free" }
            
            // Identify cooking windows
            val maxFreeDuration = freeSlots.maxOfOrNull { it.durationMinutes } ?: 0L
            val availableRecipes = recipes.value

            val profileStr = """
                Name: ${userProfile.value.name}
                Skill: ${userProfile.value.skillLevel}
                DietaryPref: ${userProfile.value.dietaryRestrictions}
                Disliked: ${userProfile.value.dislikedIngredients}
                AppliancesOwned: ${userProfile.value.appliancesOwned}
                DailyCap: ${budgetCap.value.dailyAllocation}
                MonthlyBudget: ${userProfile.value.monthlyBudget}
                SplurgeMode: ${userProfile.value.splurgeMode}
            """.trimIndent()

            val scheduleStr = events.joinToString("\n") { 
                "${it.title}: ${it.startTime} to ${it.endTime} [${it.status}] (${it.durationMinutes} mins)" 
            }

            // Attempt AI Meal Plan generation first
            val response = GeminiService.generateMealPlan(scheduleStr, profileStr)

            if (response != "API_KEY_MISSING" && !response.startsWith("ERROR:")) {
                try {
                    val mealsArray = JSONArray(response)
                    val generatedPlans = mutableListOf<MealPlan>()
                    val generatedRecipes = mutableListOf<Recipe>()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val todayStr = dateFormat.format(Date())

                    for (i in 0 until mealsArray.length()) {
                        val mealObj = mealsArray.getJSONObject(i)
                        val mealType = mealObj.getString("mealType")
                        val recipeTitle = mealObj.getString("recipeTitle")
                        val description = mealObj.getString("description")
                        val prepCookTime = mealObj.getInt("prepCookTime")
                        val baseCost = mealObj.getDouble("baseCost")
                        val calories = mealObj.getInt("calories")
                        val dietaryFlags = mealObj.optString("dietaryFlags", "")
                        val appliancesRequired = mealObj.optString("appliancesRequired", "")
                        val ingredients = mealObj.optString("ingredients", "")
                        val instructions = mealObj.optString("instructions", "")
                        val statusWarning = mealObj.optString("statusWarning", null)

                        // Insert this recipe dynamically
                        val rId = 100 + i // Unique workspace id
                        val customRecipe = Recipe(
                            id = rId,
                            title = recipeTitle,
                            description = description,
                            ingredientsList = ingredients,
                            instructionsList = instructions,
                            prepTime = prepCookTime / 2,
                            cookTime = prepCookTime / 2,
                            calories = calories,
                            dietaryFlags = dietaryFlags,
                            appliancesRequired = appliancesRequired,
                            baseCost = baseCost,
                            source = "AI Generated"
                        )
                        generatedRecipes.add(customRecipe)

                        generatedPlans.add(
                            MealPlan(
                                date = todayStr,
                                mealType = mealType,
                                recipeId = rId,
                                recipeTitle = recipeTitle,
                                prepCookTime = prepCookTime,
                                isCompleted = false,
                                statusWarning = statusWarning
                            )
                        )
                    }

                    if (generatedPlans.isNotEmpty()) {
                        repository.insertRecipes(generatedRecipes)
                        repository.clearMealPlansForDate(todayStr)
                        repository.insertMealPlans(generatedPlans)
                        
                        // Compile grocery list based on new meals
                        rebuildGroceryList(generatedRecipes, generatedPlans)
                        
                        updateActualSpending(generatedPlans)
                        _statusMessage.value = "AI Chef created a dynamic calendar-synced meal plan for you!"
                        _isGeneratingMealPlan.value = false
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.e("CalCookViewModel", "Failed to parse AI meal plan JSON: ${e.message}")
                }
            }

            // Fallback: Local rule-based recipe matches matching cooking windows
            delay(1000)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = dateFormat.format(Date())

            val localPlans = mutableListOf<MealPlan>()
            
            // Map free time windows
            // Lunch selection: if we have a very short < 10m slot, assign grab & go bar
            val shortFreeTimeExists = freeSlots.any { it.durationMinutes <= 20 }
            val lunchRecipe = (if (shortFreeTimeExists) {
                availableRecipes.firstOrNull { it.id == 8 } // Protein bar
            } else {
                availableRecipes.firstOrNull { it.id == 2 } // Avocado Wrap
            }) ?: availableRecipes.firstOrNull() ?: Recipe(title = "Default", description = "", ingredientsList = "", instructionsList = "", prepTime = 5, cookTime = 5, calories = 100, dietaryFlags = "", appliancesRequired = "", baseCost = 2.0)

            val dinnerRecipe = (if (maxFreeDuration >= 60) {
                availableRecipes.firstOrNull { it.id == 5 } // Slow Stew
            } else if (maxFreeDuration >= 20) {
                availableRecipes.firstOrNull { it.id == 3 || it.id == 6 } // Garlic Pasta or Salmon
            } else {
                availableRecipes.firstOrNull { it.id == 1 } // Med Quinoa
            }) ?: availableRecipes.firstOrNull() ?: Recipe(title = "Default", description = "", ingredientsList = "", instructionsList = "", prepTime = 5, cookTime = 5, calories = 100, dietaryFlags = "", appliancesRequired = "", baseCost = 2.0)

            val bfastRecipe = availableRecipes.firstOrNull { it.id == 7 } ?: availableRecipes.firstOrNull() ?: Recipe(title = "Default", description = "", ingredientsList = "", instructionsList = "", prepTime = 5, cookTime = 5, calories = 100, dietaryFlags = "", appliancesRequired = "", baseCost = 2.0)

            // Construct 3 local meals
            localPlans.add(
                MealPlan(
                    date = todayStr,
                    mealType = "Breakfast",
                    recipeId = bfastRecipe.id,
                    recipeTitle = bfastRecipe.title,
                    prepCookTime = bfastRecipe.prepTime + bfastRecipe.cookTime
                )
            )
            localPlans.add(
                MealPlan(
                    date = todayStr,
                    mealType = "Lunch",
                    recipeId = lunchRecipe.id,
                    recipeTitle = lunchRecipe.title,
                    prepCookTime = lunchRecipe.prepTime + lunchRecipe.cookTime,
                    statusWarning = if (lunchRecipe.id == 8) "Scanning alert: <10m slot detected. Assigned Grab-and-Go lunch!" else null
                )
            )
            localPlans.add(
                MealPlan(
                    date = todayStr,
                    mealType = "Dinner",
                    recipeId = dinnerRecipe.id,
                    recipeTitle = dinnerRecipe.title,
                    prepCookTime = dinnerRecipe.prepTime + dinnerRecipe.cookTime,
                    statusWarning = if (maxFreeDuration >= 60) "Free time is 60+ min! Pre-planned complex Slow Cooker Beef Stew." else null
                )
            )

            // Save meal plans locally
            repository.clearMealPlansForDate(todayStr)
            repository.insertMealPlans(localPlans)
            
            // Re-build grocery list
            val selectedPlanRecipes = availableRecipes.filter { r -> localPlans.any { it.recipeId == r.id } }
            rebuildGroceryList(selectedPlanRecipes, localPlans)
            
            updateActualSpending(localPlans)

            _statusMessage.value = "Smart Meal Plan assigned dynamically based on calendar gaps!"
            _isGeneratingMealPlan.value = false
        }
    }

    private suspend fun rebuildGroceryList(actRecipes: List<Recipe>, planList: List<MealPlan>) {
        repository.clearGroceryItems()
        val list = mutableListOf<GroceryItem>()

        val hSize = userProfile.value.householdSize

        actRecipes.forEach { recipe ->
            recipe.ingredientsList.split(",").forEach { ing ->
                val parts = ing.trim().split(":")
                if (parts.size >= 3) {
                    val name = parts[0]
                    val qty = parts[1].toDoubleOrNull() ?: 1.0
                    val unit = parts[2]
                    
                    // Deduplicate ingredients across multiple recipes by combining quantities
                    val existingIndex = list.indexOfFirst { it.name.lowercase() == name.lowercase() }
                    if (existingIndex != -1) {
                        val prev = list[existingIndex]
                        list[existingIndex] = prev.copy(quantity = prev.quantity + (qty * hSize))
                    } else {
                        // Categorize beautifully
                        val cat = when {
                            name.contains("Tomato", true) || name.contains("Spinach", true) || name.contains("Cucumber", true) || name.contains("Avocado", true) || name.contains("Garlic", true) || name.contains("Lemon", true) || name.contains("Asparagus", true) || name.contains("Carrot", true) || name.contains("Broccoli", true) -> "Produce"
                            name.contains("Butter", true) || name.contains("Cheese", true) || name.contains("Milk", true) -> "Dairy"
                            name.contains("Beef", true) || name.contains("Salmon", true) || name.contains("Chicken", true) -> "Meat & Seafood"
                            else -> "Pantry"
                        }
                        
                        // Scale automatic quantity Based on servings
                        val estCost = when (cat) {
                            "Meat & Seafood" -> 6.50
                            "Dairy" -> 2.50
                            "Produce" -> 1.20
                            else -> 1.80
                        }

                        // Pantry Inventory: Mark some basic items (like water, salt, butter, oil) as already in pantry!
                        val isBasicPantryItem = name.contains("Salt", true) || name.contains("Pepper", true) || name.contains("Butter", true) || name.contains("Water", true) || name.contains("Quinoa", true)

                        list.add(
                            GroceryItem(
                                name = name,
                                quantity = qty * hSize,
                                unit = unit,
                                category = cat,
                                inPantry = isBasicPantryItem,
                                isBought = false,
                                costEstimate = estCost,
                                recipeId = recipe.id
                            )
                        )
                    }
                }
            }
        }
        repository.insertGroceryItems(list)
    }

    private suspend fun updateActualSpending(plans: List<MealPlan>) {
        val totalCost = plans.sumOf { plan ->
            val recipe = database.dao.getRecipeById(plan.recipeId)
            recipe?.baseCost ?: 3.50
        }
        val current = budgetCap.value
        repository.updateBudgetCap(
            current.copy(
                currentDailySpend = totalCost,
                actualMonthlySpend = (current.actualMonthlySpend + totalCost).coerceAtMost(userProfile.value.monthlyBudget)
            )
        )
    }

    // 3. Dynamic Grocery Item Triggers
    fun toggleGroceryInPantry(item: GroceryItem) {
        viewModelScope.launch {
            repository.updateGroceryItem(item.copy(inPantry = !item.inPantry))
        }
    }

    fun toggleGroceryBought(item: GroceryItem) {
        viewModelScope.launch {
            repository.updateGroceryItem(item.copy(isBought = !item.isBought))
        }
    }

    fun addGroceryItem(name: String, quantity: Double, unit: String, category: String, cost: Double) {
        viewModelScope.launch {
            repository.insertGroceryItem(
                GroceryItem(
                    name = name,
                    quantity = quantity,
                    unit = unit,
                    category = category,
                    inPantry = false,
                    isBought = false,
                    costEstimate = cost
                )
            )
        }
    }

    fun deleteGrocery(id: Int) {
        viewModelScope.launch {
            repository.deleteGroceryItem(id)
        }
    }

    // 4. Recipe Timers & Completion Toggle
    fun toggleMealCompletion(meal: MealPlan) {
        viewModelScope.launch {
            repository.updateMealPlan(meal.copy(isCompleted = !meal.isCompleted))
            _statusMessage.value = if (!meal.isCompleted) "Meal marked as completed! Good job cooking!" else "Meal set back to active."
        }
    }

    // 5. Intelligent Substitution Controller
    fun requestIngredientSubstitution(ingredient: String, meal: MealPlan, contextOverride: String? = null) {
        viewModelScope.launch {
            _isGeneratingRecipe.value = true
            
            // Determine context trigger (based on scanned schedule/status traveling, or in-a-rush time constraint)
            val schedule = calendarEvents.value
            val isRush = meal.prepCookTime <= 10
            val isTravelling = schedule.any { it.status == "Traveling" }
            val isWfh = schedule.any { it.title.contains("Home", true) }
            
            val contextTrigger = contextOverride ?: when {
                isTravelling -> "Traveling"
                isRush -> "In a rush"
                isWfh -> "Working from home"
                else -> "Allergy"
            }

            _statusMessage.value = "Consulting AI Chef for $contextTrigger swap for $ingredient..."

            val aiResponse = GeminiService.getSmartSubstitution(ingredient, contextTrigger)

            if (aiResponse != "API_KEY_MISSING" && !aiResponse.startsWith("ERROR:")) {
                try {
                    val jsonObj = JSONObject(aiResponse)
                    val originalIng = jsonObj.getString("originalIngredient")
                    val subIng = jsonObj.getString("substituteIngredient")
                    val reason = jsonObj.getString("reason")

                    val newSub = Substitution(
                        originalIngredient = originalIng,
                        substituteIngredient = subIng,
                        contextTrigger = contextTrigger,
                        reason = reason,
                        isApproved = true
                    )
                    repository.insertSubstitution(newSub)

                    // Automatically update matching grocery item!
                    val activeGroceries = groceryItems.value
                    val matchingGrocery = activeGroceries.firstOrNull { it.name.contains(originalIng, true) }
                    if (matchingGrocery != null) {
                        repository.updateGroceryItem(
                            matchingGrocery.copy(name = subIng, category = matchingGrocery.category + " (AI Swap)")
                        )
                    }

                    _statusMessage.value = "Swapped! Proposed '$subIng' because: $reason"
                    _isGeneratingRecipe.value = false
                    return@launch
                } catch (e: Exception) {
                    Log.e("CalCookViewModel", "AI swap parse failed: ${e.message}")
                }
            }

            // Fallback smart check:
            delay(1000)
            val fallbackSubMap = mapOf(
                "salmon" to Pair("Canned Tuna", "Easy, shelf-stable, and doesn't require any preheating or frozen cooling."),
                "beef" to Pair("Cooked Tofu Cubes", "Speeds up cook time to 3 mins instead of hours of slow cooking."),
                "garlic" to Pair("Garlic Powder", "Zero prep chops, saves you precious minutes when in a rush!"),
                "spinach" to Pair("Dehydrated Kale", "Shelf stable and highly transportable for travel commutes."),
                "pasta" to Pair("Chickpea Pasta", "Gluten-free, high-protein alternative matching organic macros.")
            )

            var subFound = false
            for ((key, pair) in fallbackSubMap) {
                if (ingredient.lowercase().contains(key)) {
                    val newSub = Substitution(
                        originalIngredient = ingredient,
                        substituteIngredient = pair.first,
                        contextTrigger = contextTrigger,
                        reason = pair.second,
                        isApproved = true
                    )
                    repository.insertSubstitution(newSub)

                    // Match grocery
                    val activeG = groceryItems.value
                    val match = activeG.firstOrNull { it.name.lowercase().contains(key) }
                    if (match != null) {
                        repository.updateGroceryItem(
                            match.copy(name = pair.first, category = match.category + " (Swap)")
                        )
                    }

                    _statusMessage.value = "Substitute found locally: Proposed '${pair.first}' - ${pair.second}"
                    subFound = true
                    break
                }
            }

            if (!subFound) {
                // Generically substitute with a healthy default swap depending on allergy or time
                val friendlySwap = if (contextTrigger == "Allergy") "Allergy-Safe Blend" else "Quick Pantry Substitute"
                val genSub = Substitution(
                    originalIngredient = ingredient,
                    substituteIngredient = friendlySwap,
                    contextTrigger = contextTrigger,
                    reason = "Generic time-saving and allergy-safe kitchen swap.",
                    isApproved = true
                )
                repository.insertSubstitution(genSub)
                _statusMessage.value = "Assigned a context-supported swap: '$friendlySwap'"
            }

            _isGeneratingRecipe.value = false
        }
    }

    // 6. Custom Recipe Creator with AI
    fun createCustomRecipeWithAI(recipePrompt: String) {
        viewModelScope.launch {
            _isGeneratingRecipe.value = true
            _statusMessage.value = "Crafting custom recipe via Gemini AI..."

            val profile = userProfile.value
            val profileStr = """
                Skill: ${profile.skillLevel}
                Dietary: ${profile.dietaryRestrictions}
                Appliances: ${profile.appliancesOwned}
                No: ${profile.dislikedIngredients}
            """.trimIndent()

            val response = GeminiService.generateRecipeAI(recipePrompt, profileStr)

            if (response != "API_KEY_MISSING" && !response.startsWith("ERROR:")) {
                try {
                    val jsonObj = JSONObject(response)
                    val title = jsonObj.getString("title")
                    val desc = jsonObj.getString("description")
                    val prep = jsonObj.getInt("prepTime")
                    val cook = jsonObj.getInt("cookTime")
                    val cal = jsonObj.getInt("calories")
                    val flags = jsonObj.optString("dietaryFlags", "")
                    val apps = jsonObj.optString("appliancesRequired", "")
                    val cost = jsonObj.optDouble("baseCost", 4.0)
                    val ings = jsonObj.getString("ingredients")
                    val inst = jsonObj.getString("instructions")

                    val customRecipe = Recipe(
                        title = title,
                        description = desc,
                        ingredientsList = ings,
                        instructionsList = inst,
                        prepTime = prep,
                        cookTime = cook,
                        calories = cal,
                        dietaryFlags = flags,
                        appliancesRequired = apps,
                        baseCost = cost,
                        source = "AI Generated"
                    )

                    repository.insertRecipe(customRecipe)
                    selectRecipe(customRecipe)
                    _statusMessage.value = "Created '$title' customized to your pantry constraints and skill!"
                    _isGeneratingRecipe.value = false
                    return@launch
                } catch (e: Exception) {
                    Log.e("CalCookViewModel", "Recipe parsing failed: ${e.message}")
                }
            }

            // Fallback recipe creation if no internet or key
            delay(1200)
            val fallbackTitle = "Custom $recipePrompt Delight"
            val fallbackRecipe = Recipe(
                title = fallbackTitle,
                description = "A customized culinary recipe created locally for your kitchen setup.",
                ingredientsList = "Main Protein:150:g, Secret Spices:2:tbsp, Olive Oil:1:tbsp, Fresh Herbs:1:sprig",
                instructionsList = "Lightly coat the main protein with olive oil.|Rub with spices and herbs.|Cook on medium heat using your owned appliances for 10 minutes.",
                prepTime = 5,
                cookTime = 10,
                calories = 340,
                dietaryFlags = profile.dietaryRestrictions,
                appliancesRequired = profile.appliancesOwned.split(",").firstOrNull() ?: "stove",
                baseCost = 4.25,
                source = "AI Generated"
            )

            repository.insertRecipe(fallbackRecipe)
            selectRecipe(fallbackRecipe)
            _statusMessage.value = "$fallbackTitle created! (Offline AI Mode activated)"
            _isGeneratingRecipe.value = false
        }
    }

    // 7. Budget Allocation Controls
    fun updateBudgetSettings(monthly: Double, dailyAlloc: Double, isSplurge: Boolean) {
        viewModelScope.launch {
            val profile = userProfile.value.copy(monthlyBudget = monthly, splurgeMode = isSplurge)
            repository.updateUserProfile(profile)

            val cap = budgetCap.value.copy(dailyAllocation = dailyAlloc, weeklyAllocation = dailyAlloc * 7)
            repository.updateBudgetCap(cap)

            _statusMessage.value = "Updated budget thresholds! Cal-Cook is optimizing recipes."
        }
    }

    // 8. Onboarding Complete Flow
    fun completeOnboardingAndSetup(name: String, size: Int, skill: String, dietary: String, appliances: String) {
        viewModelScope.launch {
            val freshProfile = UserProfile(
                id = 1,
                name = name,
                householdSize = size,
                skillLevel = skill,
                dietaryRestrictions = dietary,
                appliancesOwned = appliances
            )
            repository.updateUserProfile(freshProfile)

            // Auto-trigger initial scan
            scanAndSyncCalendar()
        }
    }
}
