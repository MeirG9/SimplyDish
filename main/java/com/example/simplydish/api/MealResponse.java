package com.example.simplydish.api;

import com.example.simplydish.models.Recipe;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class MealResponse {

    @SerializedName("meals")
    public List<MealDto> meals;

    public static class MealDto {
        public String idMeal;
        public String strMeal;       // Title
        public String strMealThumb;  // Image URL

        /**
         * Converts the API DTO to your internal Recipe model.
         * STRICT ADHERENCE: Uses the existing constructor (without ID)
         * and sets the ID manually afterwards.
         */
        public Recipe toSystemRecipe() {
            // 1. Create instance using your specific constructor:
            // (authorUid, title, description, category, imageUrl, ingredients, directions, createdAt)
            Recipe recipe = new Recipe(
                    "System",                     // authorUid
                    strMeal,                      // title
                    "Imported from TheMealDB",    // description
                    "International",              // category
                    strMealThumb,                 // imageUrl
                    new ArrayList<>(),            // ingredients (API filter doesn't provide list)
                    "Full instructions available on the web.", // directions placeholder
                    System.currentTimeMillis()    // createdAt
            );

            // 2. Set the ID explicitly using the setter, as required by your model
            recipe.setId("mealdb_" + idMeal);

            return recipe;
        }
    }
}