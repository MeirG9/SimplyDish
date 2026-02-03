package com.example.simplydish.utils;

import androidx.annotation.Nullable;

import com.example.simplydish.models.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirebaseFavoritesHelper {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();

    private FirebaseFavoritesHelper() {}

    public static void toggleFavorite(@Nullable Recipe recipe, boolean isCurrentlyFavorite, @Nullable Runnable onFailure) {
        if (recipe == null || recipe.getId() == null) return;
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        String recipeId = recipe.getId();

        if (isCurrentlyFavorite) {
            db.collection(Constants.COLLECTION_USERS)
                    .document(uid)
                    .collection(Constants.COLLECTION_FAVORITES)
                    .document(recipeId)
                    .delete()
                    .addOnFailureListener(e -> {
                        if (onFailure != null) onFailure.run();
                    });
        } else {
            if (recipeId.startsWith("mealdb_")) {
                db.collection(Constants.COLLECTION_RECIPES)
                        .document(recipeId)
                        .set(recipe);
            }

            Map<String, Object> data = new HashMap<>();
            data.put(Constants.FIELD_FAVORITED_AT, FieldValue.serverTimestamp());

            db.collection(Constants.COLLECTION_USERS)
                    .document(uid)
                    .collection(Constants.COLLECTION_FAVORITES)
                    .document(recipeId)
                    .set(data)
                    .addOnFailureListener(e -> {
                        if (onFailure != null) onFailure.run();
                    });
        }
    }

    public static void removeFavoriteById(@Nullable String recipeId, @Nullable Runnable onFailure) {
        if (recipeId == null || auth.getCurrentUser() == null) return;
        db.collection(Constants.COLLECTION_USERS).document(auth.getCurrentUser().getUid())
                .collection(Constants.COLLECTION_FAVORITES).document(recipeId).delete()
                .addOnFailureListener(e -> { if (onFailure != null) onFailure.run(); });
    }
}