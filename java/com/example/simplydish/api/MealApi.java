package com.example.simplydish.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MealApi {
    // Search meals by main ingredient (free endpoint)
    @GET("filter.php")
    Call<MealResponse> getMealsByIngredient(@Query("i") String ingredient);
}