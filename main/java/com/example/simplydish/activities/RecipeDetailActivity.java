package com.example.simplydish.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.simplydish.R;
import com.example.simplydish.models.Recipe;
import com.example.simplydish.utils.Constants;
import com.example.simplydish.utils.ImageUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RecipeDetailActivity extends AppCompatActivity {

    private ImageView ivImage;
    private TextView tvTitle;
    private TextView tvCategory;
    private TextView tvDescription;
    private TextView tvIngredients;
    private TextView tvDirections;
    private FloatingActionButton btnFavorite;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    private Recipe recipe;

    private boolean isFavorite;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews();
        updateFavoriteIcon();
        setupActionBar();

        recipe = (Recipe) getIntent().getSerializableExtra(Constants.EXTRA_RECIPE);
        if (recipe == null) {
            Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindData(recipe);

        btnFavorite.setOnClickListener(v -> toggleFavorite());

        if (isRecipeIdValid()) {
            checkIfFavorite();
        } else {
            btnFavorite.setEnabled(false);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    private void bindViews() {
        ivImage = findViewById(R.id.ivDetailImage);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvCategory = findViewById(R.id.tvDetailCategory);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvIngredients = findViewById(R.id.tvDetailIngredients);
        tvDirections = findViewById(R.id.tvDetailDirections);
        btnFavorite = findViewById(R.id.btnDetailFavorite);
    }

    private void setupActionBar() {
        if (getSupportActionBar() == null) return;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Recipe");
    }

    private void bindData(Recipe recipe) {
        tvTitle.setText(recipe.getTitle());
        tvCategory.setText(recipe.getCategory());
        tvDescription.setText(recipe.getDescription());
        tvDirections.setText(recipe.getDirections());

        StringBuilder sb = new StringBuilder();
        for (String ingredient : recipe.getIngredients()) {
            if (TextUtils.isEmpty(ingredient)) continue;
            sb.append("• ").append(ingredient).append('\n');
        }
        tvIngredients.setText(sb.toString().trim());

        ImageUtils.loadImage(ivImage, recipe.getImageUrl());
    }

    private boolean isRecipeIdValid() {
        return recipe != null && !TextUtils.isEmpty(recipe.getId());
    }

    private void checkIfFavorite() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || recipe == null) return;

        btnFavorite.setEnabled(false);

        db.collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .collection(Constants.COLLECTION_FAVORITES)
                .document(recipe.getId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) return;
                    isFavorite = snapshot.exists();
                    updateFavoriteIcon();
                    btnFavorite.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnFavorite.setEnabled(true);
                });
    }

    // --- UPDATED HYBRID LOGIC START ---
    private void toggleFavorite() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login", Toast.LENGTH_SHORT).show();
            return;
        }
        if (recipe == null || TextUtils.isEmpty(recipe.getId())) {
            Toast.makeText(this, "Invalid recipe", Toast.LENGTH_SHORT).show();
            return;
        }

        btnFavorite.setEnabled(false);

        if (isFavorite) {
            // Remove from favorites (Standard Logic)
            db.collection(Constants.COLLECTION_USERS)
                    .document(user.getUid())
                    .collection(Constants.COLLECTION_FAVORITES)
                    .document(recipe.getId())
                    .delete()
                    .addOnSuccessListener(unused -> {
                        if (isFinishing() || isDestroyed()) return;
                        isFavorite = false;
                        updateFavoriteIcon();
                        Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                        btnFavorite.setEnabled(true);
                    })
                    .addOnFailureListener(e -> {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnFavorite.setEnabled(true);
                    });
            return;
        }

        // Add to favorites (Hybrid Logic)
        if (recipe.getId().startsWith("mealdb_")) {
            // This is an API recipe. Ensure it exists in the global 'recipes' collection first.
            // Using set() acts as an Upsert (Update/Insert).
            db.collection(Constants.COLLECTION_RECIPES)
                    .document(recipe.getId())
                    .set(recipe) // Save full recipe to global DB
                    .addOnSuccessListener(aVoid -> {
                        // Once safe, link to user favorites
                        addToUserFavorites(user.getUid());
                    })
                    .addOnFailureListener(e -> {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(this, "Failed to import recipe: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnFavorite.setEnabled(true);
                    });
        } else {
            // Standard Firestore recipe
            addToUserFavorites(user.getUid());
        }
    }

    private void addToUserFavorites(String uid) {
        // Save ONLY timestamp to user favorites (Database Smell Fix)
        Map<String, Object> data = new HashMap<>();
        data.put(Constants.FIELD_FAVORITED_AT, FieldValue.serverTimestamp());

        db.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .collection(Constants.COLLECTION_FAVORITES)
                .document(recipe.getId())
                .set(data)
                .addOnSuccessListener(unused -> {
                    if (isFinishing() || isDestroyed()) return;
                    isFavorite = true;
                    updateFavoriteIcon();
                    Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
                    btnFavorite.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnFavorite.setEnabled(true);
                });
    }
    // --- UPDATED HYBRID LOGIC END ---

    private void updateFavoriteIcon() {
        if (isFavorite) {
            btnFavorite.setImageResource(android.R.drawable.btn_star_big_on);
            btnFavorite.setColorFilter(ContextCompat.getColor(this, R.color.primary));
        } else {
            btnFavorite.setImageResource(android.R.drawable.btn_star_big_off);
            btnFavorite.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray));
        }
    }
}