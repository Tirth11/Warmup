package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

class CalCookRepository(private val dao: CalCookDao) {

    val calendarEvents: Flow<List<CalendarEvent>> = dao.getAllCalendarEvents()
    val recipes: Flow<List<Recipe>> = dao.getAllRecipes()
    val allMealPlans: Flow<List<MealPlan>> = dao.getAllMealPlans()
    val groceryItems: Flow<List<GroceryItem>> = dao.getAllGroceryItems()
    val substitutions: Flow<List<Substitution>> = dao.getAllSubstitutions()
    val userProfile: Flow<UserProfile?> = dao.getUserProfile()
    val budgetCap: Flow<BudgetCap?> = dao.getBudgetCap()

    fun getMealPlansForDate(date: String): Flow<List<MealPlan>> = dao.getMealPlansForDate(date)

    suspend fun insertCalendarEvent(event: CalendarEvent) = dao.insertCalendarEvent(event)
    suspend fun insertCalendarEvents(events: List<CalendarEvent>) = dao.insertCalendarEvents(events)
    suspend fun deleteCalendarEvent(event: CalendarEvent) = dao.deleteCalendarEvent(event)
    suspend fun clearCalendarEvents() = dao.clearCalendarEvents()

    suspend fun insertRecipe(recipe: Recipe) = dao.insertRecipe(recipe)
    suspend fun insertRecipes(recipes: List<Recipe>) = dao.insertRecipes(recipes)
    suspend fun clearRecipes() = dao.clearRecipes()

    suspend fun insertMealPlan(mealPlan: MealPlan) = dao.insertMealPlan(mealPlan)
    suspend fun insertMealPlans(mealPlans: List<MealPlan>) = dao.insertMealPlans(mealPlans)
    suspend fun updateMealPlan(mealPlan: MealPlan) = dao.updateMealPlan(mealPlan)
    suspend fun clearMealPlansForDate(date: String) = dao.clearMealPlansForDate(date)
    suspend fun clearAllMealPlans() = dao.clearAllMealPlans()

    suspend fun insertGroceryItem(item: GroceryItem) = dao.insertGroceryItem(item)
    suspend fun insertGroceryItems(items: List<GroceryItem>) = dao.insertGroceryItems(items)
    suspend fun updateGroceryItem(item: GroceryItem) = dao.updateGroceryItem(item)
    suspend fun deleteGroceryItem(id: Int) = dao.deleteGroceryItem(id)
    suspend fun clearGroceryItems() = dao.clearGroceryItems()

    suspend fun insertSubstitution(sub: Substitution) = dao.insertSubstitution(sub)
    suspend fun updateSubstitution(sub: Substitution) = dao.updateSubstitution(sub)

    suspend fun updateUserProfile(profile: UserProfile) = dao.insertUserProfile(profile)
    suspend fun updateBudgetCap(cap: BudgetCap) = dao.insertBudgetCap(cap)

    suspend fun prepopulateDataIfNeeded() {
        // 1. Prepopulate User Profile
        val currentProfile = dao.getUserProfileSync()
        if (currentProfile == null) {
            dao.insertUserProfile(
                UserProfile(
                    id = 1,
                    name = "Alex Mercer",
                    householdSize = 2,
                    skillLevel = "Intermediate",
                    appliancesOwned = "Stove,Microwave,Oven,Slow Cooker",
                    dietaryRestrictions = "none",
                    dislikedIngredients = "Cilantro,Blue Cheese",
                    monthlyBudget = 450.0,
                    splurgeMode = false
                )
            )
        }

        // 2. Prepopulate Budget Cap
        val currentCap = dao.getBudgetCapSync()
        if (currentCap == null) {
            dao.insertBudgetCap(
                BudgetCap(
                    id = 1,
                    dailyAllocation = 18.0,
                    weeklyAllocation = 110.0,
                    actualMonthlySpend = 42.50,
                    currentDailySpend = 12.00
                )
            )
        }

        // 3. Prepopulate Recipes
        val existingRecipes = dao.getAllRecipes().firstOrNull() ?: emptyList()
        if (existingRecipes.isEmpty()) {
            val curatedRecipes = listOf(
                Recipe(
                    id = 1,
                    title = "Mediterranean Quinoa Bowl",
                    description = "A fresh, protein-packed bowl loaded with tomatoes, olives, quinoa, cucumbers, and feta.",
                    ingredientsList = "Quinoa:0.5:cup, Cucumber:0.5:cup, Cherry Tomatoes:0.5:cup, Kalamata Olives:4:pieces, Feta Cheese:2:tbsp, Olive Oil:1:tbsp, Lemon Juice:1:tsp",
                    instructionsList = "Rinse and cook quinoa as per package instructions.|Dice the cucumbers and cherry tomatoes.|Toss quinoa, cucumber, cherry tomatoes, and olives together.|Drizzle with olive oil and lemon juice, then crumble feta on top.",
                    prepTime = 5,
                    cookTime = 5,
                    calories = 380,
                    dietaryFlags = "gluten-free,vegetarian",
                    appliancesRequired = "Stove",
                    baseCost = 3.50,
                    source = "Curated"
                ),
                Recipe(
                    id = 2,
                    title = "Avocado & Hummus Salad Wrap",
                    description = "Super fast and portable wrap filled with spinach, avocado, tomatoes, cucumber, and creamy hummus.",
                    ingredientsList = "Tortilla Wrap:1:wrap, Avocado:0.5:fruit, Hummus:2:tbsp, Baby Spinach:0.5:cup, Tomato:0.5:fruit, Cucumber:4:slices",
                    instructionsList = "Lay tortilla wrap flat on a clean surface.|Spread hummus evenly across the wrap.|Layer spinach, tomato slices, cucumber, and avocado slices.|Fold sides in and roll tightly to enclose ingredients.",
                    prepTime = 7,
                    cookTime = 0,
                    calories = 310,
                    dietaryFlags = "vegan,gluten-free",
                    appliancesRequired = "none",
                    baseCost = 2.80,
                    source = "Curated"
                ),
                Recipe(
                    id = 3,
                    title = "Garlic Lemon Butter Pasta",
                    description = "An incredibly simple and aromatic pasta dish that delivers chef-quality flavor under 15 minutes.",
                    ingredientsList = "Spaghetti:80:g, Garlic Cloves:3:pieces, Butter:2:tbsp, Fresh Lemon juice:1:tbsp, Parmesan Cheese:2:tbsp, Fresh Parsley:1:tbsp",
                    instructionsList = "Boil spaghetti in salted water for 8-9 minutes until al dente.|While pasta boils, mince the garlic cloves.|In a saucepan, melt butter on low-medium. Sauté minced garlic until golden.|Drain pasta, toss directly with garlic-butter sauce, lemon juice, grated parmesan, and chopped parsley.",
                    prepTime = 5,
                    cookTime = 10,
                    calories = 440,
                    dietaryFlags = "vegetarian",
                    appliancesRequired = "Stove",
                    baseCost = 1.90,
                    source = "Curated"
                ),
                Recipe(
                    id = 4,
                    title = "Crispy Teriyaki Tofu Stir-fry",
                    description = "Delicious cubes of tofu tossed with broccoli, bell pepper, carrots, and sweet teriyaki sauce.",
                    ingredientsList = "Firm Tofu:150:g, Broccoli Florets:1:cup, Bell Pepper:0.5:fruit, Carrots:0.5:fruit, Teriyaki Sauce:3:tbsp, Sesame Oil:1:tbsp, White Rice:0.5:cup",
                    instructionsList = "Cook your white rice in a pot or rice cooker.|Press tofu to remove excess moisture; dice into cubes.|Sauté tofu in sesame oil until golden brown on all sides.|Add broccoli, diced bell pepper, and carrot slices. Cook for 5 minutes.|Pour in teriyaki sauce and simmer until veggies are tender but crisp.",
                    prepTime = 10,
                    cookTime = 15,
                    calories = 410,
                    dietaryFlags = "vegan,gluten-free",
                    appliancesRequired = "Stove",
                    baseCost = 3.20,
                    source = "Curated"
                ),
                Recipe(
                    id = 5,
                    title = "Hearty Beef & Vegetable Stew",
                    description = "A comforting, slow-cooked beef stew made with rich beef stock, carrots, celery, and golden potatoes.",
                    ingredientsList = "Beef Stew Meat:200:g, Potatoes:2:small, Carrots:1:large, Celery Ribs:1:piece, Beef Broth:2:cup, Garlic:2:cloves, Rosemary:0.5:tsp",
                    instructionsList = "Cut beef, potatoes, carrots, and celery into bite-sized pieces.|Season beef with salt and pepper, then brown in a pan (optional).|Transfer beef, vegetables, broth, garlic, and rosemary to the slow cooker.|Cover and cook on LOW for 6-8 hours or HIGH for 4 hours.",
                    prepTime = 15,
                    cookTime = 240,
                    calories = 580,
                    dietaryFlags = "gluten-free",
                    appliancesRequired = "Slow Cooker",
                    baseCost = 8.50,
                    source = "Curated"
                ),
                Recipe(
                    id = 6,
                    title = "Herb-Crusted Lemon Salmon",
                    description = "Elegant oven-baked salmon fillet crusted with fresh green herbs, baked alongside tender asparagus.",
                    ingredientsList = "Salmon Fillet:150:g, Asparagus Spears:6:pieces, Olive Oil:1.5:tbsp, Mixed Herbs:1:tsp, Lemon:0.5:fruit, Salt & Pepper:1:pinch",
                    instructionsList = "Preheat oven to 400°F (200°C).|Place salmon fillet and asparagus on a baking sheet.|Drizzle both with olive oil, rub salmon with mixed herbs, salt, and pepper.|Place lemon slices on top of salmon.|Bake for 12-15 minutes until salmon flakes easily with a fork.",
                    prepTime = 8,
                    cookTime = 12,
                    calories = 460,
                    dietaryFlags = "gluten-free,keto",
                    appliancesRequired = "Oven",
                    baseCost = 11.20,
                    source = "Curated"
                ),
                Recipe(
                    id = 7,
                    title = "Nutritionally-Dense Power Bowl",
                    description = "High-protein bowl featuring edamame, spinach, avocado, boiled eggs, sweet potato, and sesame drizzle.",
                    ingredientsList = "Sweet Potato:1:medium, Eggs:2:large, Edamame beans:0.5:cup, Baby Spinach:1:cup, Avocado:0.5:fruit, Sesame dressing:2:tbsp",
                    instructionsList = "Prick sweet potato and microwave for 5 minutes until fully soft.|Boil egg in saucepan for 7 minutes to produce medium-hard yolks.|Assemble bowl starting with spinach.|Add microwaved sweet potato cubes, halved boiled eggs, edamame, and avocado slices.|Drizzle with sesame dressing.",
                    prepTime = 6,
                    cookTime = 8,
                    calories = 520,
                    dietaryFlags = "gluten-free,vegetarian",
                    appliancesRequired = "Microwave,Stove",
                    baseCost = 4.10,
                    source = "Curated"
                ),
                Recipe(
                    id = 8,
                    title = "Protein Bars & Fresh Berries",
                    description = "The ultimate grab-and-go meal. High-quality protein bar paired with fresh seasonal berries.",
                    ingredientsList = "Protein Bar:1:piece, Strawberries:5:pieces, Blueberries:0.25:cup",
                    instructionsList = "Rinse strawberries and blueberries.|Unwrap the protein bar and arrange nicely on a plate with berries.|Perfect for when you have less than 10 minutes of free time!",
                    prepTime = 2,
                    cookTime = 0,
                    calories = 280,
                    dietaryFlags = "gluten-free,vegetarian",
                    appliancesRequired = "none",
                    baseCost = 3.90,
                    source = "Curated"
                )
            )
            dao.insertRecipes(curatedRecipes)
        }

        // 4. Prepopulate Calendar Events (today's schedule)
        val existingEvents = dao.getAllCalendarEvents().firstOrNull() ?: emptyList()
        if (existingEvents.isEmpty()) {
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)
            val baseTime = today.timeInMillis

            val dayEvents = listOf(
                CalendarEvent(
                    title = "Scanned: Run 5K Gym Workout",
                    startTime = baseTime + (7 * 60 + 30) * 60 * 1000, // 7:30 AM
                    endTime = baseTime + (8 * 15) * 60 * 1000, // 8:15 AM
                    status = "Free"
                ),
                CalendarEvent(
                    title = "Scanned: Standup Meeting",
                    startTime = baseTime + (9 * 60) * 60 * 1000, // 9:00 AM
                    endTime = baseTime + (9 * 60 + 30) * 60 * 1000, // 9:30 AM
                    status = "Busy"
                ),
                CalendarEvent(
                    title = "Scanned: Product Design Sync",
                    startTime = baseTime + (10 * 60) * 60 * 1000, // 10:00 AM
                    endTime = baseTime + (11 * 60 + 30) * 60 * 1000, // 11:30 AM
                    status = "Busy"
                ),
                CalendarEvent(
                    title = "Scanned: Quick 1:1 Manager Block",
                    startTime = baseTime + (11 * 60 + 35) * 60 * 1000, // 11:35 AM
                    endTime = baseTime + (11 * 60 + 55) * 60 * 1000, // 11:55 AM
                    status = "Busy"
                ),
                CalendarEvent(
                    title = "Scanned: Free Lunch Gap",
                    startTime = baseTime + (12 * 60) * 60 * 1000, // 12:00 PM
                    endTime = baseTime + (12 * 60 + 30) * 60 * 1000, // 12:30 PM (30 min window!)
                    status = "Free"
                ),
                CalendarEvent(
                    title = "Scanned: Dev Pair-Programming Sync",
                    startTime = baseTime + (14 * 60) * 60 * 1000, // 2:00 PM
                    endTime = baseTime + (15 * 60 + 30) * 60 * 1000, // 3:30 PM
                    status = "Busy"
                ),
                CalendarEvent(
                    title = "Scanned: Commute Trip Home",
                    startTime = baseTime + (16 * 60 + 30) * 60 * 1000, // 4:30 PM
                    endTime = baseTime + (17 * 60) * 60 * 1000, // 5:00 PM
                    status = "Traveling"
                ),
                CalendarEvent(
                    title = "Scanned: Evening Cook Window",
                    startTime = baseTime + (18 * 60) * 60 * 1000, // 6:00 PM
                    endTime = baseTime + (19 * 60 + 30) * 60 * 1000, // 7:30 PM (90 min free window!)
                    status = "Free"
                )
            )
            dao.insertCalendarEvents(dayEvents)
        }

        // 5. Prepopulate Substitutions
        val existingSubs = dao.getAllSubstitutions().firstOrNull() ?: emptyList()
        if (existingSubs.isEmpty()) {
            val standardSubs = listOf(
                Substitution(
                    originalIngredient = "Salmon Fillet",
                    substituteIngredient = "Canned Tuna",
                    contextTrigger = "Traveling",
                    reason = "Shelf-stable, ready-to-eat swap that needs no cooling or oven.",
                    isApproved = true
                ),
                Substitution(
                    originalIngredient = "Quinoa",
                    substituteIngredient = "Instant Microwave Brown Rice",
                    contextTrigger = "In a rush",
                    reason = "Cuts prep time from 20 minutes to 90 seconds.",
                    isApproved = true
                ),
                Substitution(
                    originalIngredient = "Beef Stew Meat",
                    substituteIngredient = "Canned Kidney Beans",
                    contextTrigger = "In a rush",
                    reason = "Avoids 4-hour slow cooking; just rinse, heat, and serve.",
                    isApproved = true
                ),
                Substitution(
                    originalIngredient = "Peanut Butter",
                    substituteIngredient = "Sunflower Seed Butter",
                    contextTrigger = "Allergy",
                    reason = "Peanut-allergy-safe swap with a identical creaminess and protein profile.",
                    isApproved = true
                ),
                Substitution(
                    originalIngredient = "Fresh Broccoli",
                    substituteIngredient = "Frozen Broccoli Florets",
                    contextTrigger = "In a rush",
                    reason = "Pre-washed and pre-cut, saves 5 minutes chopped prep time.",
                    isApproved = true
                )
            )
            dao.insertSubstitutions(standardSubs)
        }

        // 6. Prepopulate some default Meal Plans for today if empty
        val existingPlans = dao.getAllMealPlans().firstOrNull() ?: emptyList()
        if (existingPlans.isEmpty()) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = dateFormat.format(Date())

            val defaultPlans = listOf(
                MealPlan(
                    date = todayStr,
                    mealType = "Breakfast",
                    recipeId = 8,
                    recipeTitle = "Protein Bars & Fresh Berries",
                    prepCookTime = 2,
                    isCompleted = true
                ),
                MealPlan(
                    date = todayStr,
                    mealType = "Lunch",
                    recipeId = 2,
                    recipeTitle = "Avocado & Hummus Salad Wrap",
                    prepCookTime = 7,
                    isCompleted = false
                ),
                MealPlan(
                    date = todayStr,
                    mealType = "Dinner",
                    recipeId = 3,
                    recipeTitle = "Garlic Lemon Butter Pasta",
                    prepCookTime = 15,
                    isCompleted = false
                )
            )
            dao.insertMealPlans(defaultPlans)

            // 7. Also build a list of matching Groceries for these meal plans
            val defaultGroceries = listOf(
                GroceryItem(name = "Tortilla Wraps", quantity = 1.0, unit = "pack", category = "Pantry", inPantry = true, costEstimate = 2.50),
                GroceryItem(name = "Ripe Avocado", quantity = 2.0, unit = "fruit", category = "Produce", inPantry = false, costEstimate = 3.00),
                GroceryItem(name = "Hummus Tub", quantity = 1.0, unit = "container", category = "Deli", inPantry = false, costEstimate = 3.50),
                GroceryItem(name = "Spaghetti Pasta", quantity = 1.0, unit = "box", category = "Pantry", inPantry = true, costEstimate = 1.20),
                GroceryItem(name = "Garlic Bulbs", quantity = 1.0, unit = "bulb", category = "Produce", inPantry = false, costEstimate = 0.80),
                GroceryItem(name = "Fresh Lemons", quantity = 3.0, unit = "pieces", category = "Produce", inPantry = false, costEstimate = 1.50),
                GroceryItem(name = "Salted Butter", quantity = 1.0, unit = "block", category = "Dairy", inPantry = true, costEstimate = 3.00),
                GroceryItem(name = "Parmesan Cheese", quantity = 1.0, unit = "tub", category = "Dairy", inPantry = false, costEstimate = 4.00)
            )
            dao.insertGroceryItems(defaultGroceries)
        }
    }
}
