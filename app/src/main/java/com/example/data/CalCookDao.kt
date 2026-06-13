package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CalCookDao {

    // --- Calendar Events ---
    @Query("SELECT * FROM calendar_events ORDER BY startTime ASC")
    fun getAllCalendarEvents(): Flow<List<CalendarEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvent(event: CalendarEvent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvents(events: List<CalendarEvent>)

    @Query("DELETE FROM calendar_events")
    suspend fun clearCalendarEvents()

    @Delete
    suspend fun deleteCalendarEvent(event: CalendarEvent)

    // --- Recipes ---
    @Query("SELECT * FROM recipes")
    fun getAllRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipeById(id: Int): Recipe?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<Recipe>)

    @Query("DELETE FROM recipes")
    suspend fun clearRecipes()

    // --- Meal Plans ---
    @Query("SELECT * FROM meal_plans WHERE date = :date")
    fun getMealPlansForDate(date: String): Flow<List<MealPlan>>

    @Query("SELECT * FROM meal_plans")
    fun getAllMealPlans(): Flow<List<MealPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(mealPlan: MealPlan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlans(mealPlans: List<MealPlan>)

    @Update
    suspend fun updateMealPlan(mealPlan: MealPlan)

    @Query("DELETE FROM meal_plans WHERE date = :date")
    suspend fun clearMealPlansForDate(date: String)

    @Query("DELETE FROM meal_plans")
    suspend fun clearAllMealPlans()

    // --- Grocery Items ---
    @Query("SELECT * FROM grocery_items")
    fun getAllGroceryItems(): Flow<List<GroceryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroceryItem(item: GroceryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroceryItems(items: List<GroceryItem>)

    @Update
    suspend fun updateGroceryItem(item: GroceryItem)

    @Query("DELETE FROM grocery_items WHERE id = :id")
    suspend fun deleteGroceryItem(id: Int)

    @Query("DELETE FROM grocery_items")
    suspend fun clearGroceryItems()

    // --- Substitutions ---
    @Query("SELECT * FROM substitutions")
    fun getAllSubstitutions(): Flow<List<Substitution>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubstitution(sub: Substitution)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubstitutions(subs: List<Substitution>)

    @Update
    suspend fun updateSubstitution(sub: Substitution)

    @Query("DELETE FROM substitutions")
    suspend fun clearSubstitutions()

    // --- User Profile ---
    @Query("SELECT * FROM user_profiles WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE id = 1")
    suspend fun getUserProfileSync(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    // --- Budget Cap ---
    @Query("SELECT * FROM budget_caps WHERE id = 1")
    fun getBudgetCap(): Flow<BudgetCap?>

    @Query("SELECT * FROM budget_caps WHERE id = 1")
    suspend fun getBudgetCapSync(): BudgetCap?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetCap(budgetCap: BudgetCap)
}
