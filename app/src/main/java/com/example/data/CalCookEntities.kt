package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

// ------------------ ENTITIES ------------------

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val startTime: Long, // Epoch ms
    val endTime: Long,   // Epoch ms
    val status: String,   // "Busy", "Free", "Traveling", "Out of Office"
    val isMock: Boolean = true
) {
    val durationMinutes: Long
        get() = (endTime - startTime) / (1000 * 60)
}

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val ingredientsList: String, // Comma-separated or JSON list of ingredients
    val instructionsList: String, // Pipe-separated or JSON list of steps
    val prepTime: Int, // minutes
    val cookTime: Int, // minutes
    val calories: Int,
    val dietaryFlags: String, // Comma-separated: "vegan,keto,gluten-free"
    val appliancesRequired: String, // Comma-separated: "oven,slow cooker"
    val baseCost: Double,
    val imageUrl: String? = null,
    val source: String = "Curated" // "Curated", "AI Generated", "History Favorite"
)

@Entity(tableName = "meal_plans")
data class MealPlan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // "YYYY-MM-DD"
    val mealType: String, // "Breakfast", "Lunch", "Dinner", "Snack"
    val recipeId: Int,
    val recipeTitle: String,
    val prepCookTime: Int,
    val isCompleted: Boolean = false,
    val statusWarning: String? = null // Warnings like "Exceeds daily budget!", etc.
)

@Entity(tableName = "grocery_items")
data class GroceryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val quantity: Double,
    val unit: String,
    val category: String, // "Produce", "Dairy", "Meat", "Pantry", etc.
    val inPantry: Boolean = false, // If true, user already has it
    val isBought: Boolean = false, // Checked off during shopping
    val costEstimate: Double,
    val recipeId: Int? = null // For tracking source
)

@Entity(tableName = "substitutions")
data class Substitution(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalIngredient: String,
    val substituteIngredient: String,
    val contextTrigger: String, // "Traveling", "In a rush", "Working from home", "Allergy"
    val reason: String,
    val isApproved: Boolean = false
)

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Chef",
    val householdSize: Int = 1,
    val skillLevel: String = "Beginner", // "Beginner", "Intermediate", "Advanced"
    val appliancesOwned: String = "Stove,Microwave,Oven", // Comma-separated
    val dietaryRestrictions: String = "", // Comma-separated: "vegan,keto,gluten-free,none"
    val dislikedIngredients: String = "", // Comma-separated
    val monthlyBudget: Double = 400.0,
    val splurgeMode: Boolean = false
)

@Entity(tableName = "budget_caps")
data class BudgetCap(
    @PrimaryKey val id: Int = 1,
    val dailyAllocation: Double = 15.0,
    val weeklyAllocation: Double = 100.0,
    val actualMonthlySpend: Double = 0.0,
    val currentDailySpend: Double = 0.0
)
