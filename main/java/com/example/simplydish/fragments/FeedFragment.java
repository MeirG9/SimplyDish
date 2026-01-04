package com.example.simplydish.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplydish.R;
import com.example.simplydish.activities.RecipeDetailActivity;
import com.example.simplydish.adapters.RecipeAdapter;
import com.example.simplydish.models.Recipe;
import com.example.simplydish.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FeedFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private RecipeAdapter adapter;

    private final List<Recipe> recipes = new ArrayList<>();
    private final Set<String> favoriteIds = new HashSet<>();

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    private ListenerRegistration favoritesListener;

    @Nullable
    private ListenerRegistration recipesListener;

    public FeedFragment() { }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews(view);
        setupRecyclerView();

        listenToFavorites();
        listenToRecipes();
    }

    private void bindViews(@NonNull View view) {
        recyclerView = view.findViewById(R.id.recyclerViewFeed);
        progressBar = view.findViewById(R.id.progressBarFeed);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        adapter = new RecipeAdapter(requireContext(), recipes);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(recipe -> {
            if (!isAdded()) return;

            Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
            intent.putExtra(Constants.EXTRA_RECIPE, recipe);
            startActivity(intent);
        });

        adapter.setOnFavoriteClickListener(this::toggleFavorite);
    }

    private void listenToFavorites() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        if (favoritesListener != null) {
            favoritesListener.remove();
            favoritesListener = null;
        }

        favoritesListener = db.collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .collection(Constants.COLLECTION_FAVORITES)
                .addSnapshotListener((snapshots, e) -> {
                    if (!isAdded() || getView() == null) return;
                    if (e != null || snapshots == null) return;

                    favoriteIds.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        favoriteIds.add(doc.getId()); // favorites/{recipeId}
                    }
                    adapter.updateFavorites(new ArrayList<>(favoriteIds));
                });
    }

    private void listenToRecipes() {
        if (recipesListener != null) {
            recipesListener.remove();
            recipesListener = null;
        }

        progressBar.setVisibility(View.VISIBLE);

        recipesListener = db.collection(Constants.COLLECTION_RECIPES)
                .orderBy(Constants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (!isAdded() || getView() == null) return;

                    progressBar.setVisibility(View.GONE);

                    if (e != null || snapshots == null) {
                        showEmptyState(recipes.isEmpty());
                        return;
                    }

                    recipes.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        if (recipe == null) continue;

                        // Ensure the recipe ID is always available for favorites.
                        recipe.setId(doc.getId());
                        recipes.add(recipe);
                    }

                    adapter.updateData(recipes);
                    adapter.updateFavorites(new ArrayList<>(favoriteIds));
                    showEmptyState(recipes.isEmpty());
                });
    }

    private void toggleFavorite(@Nullable Recipe recipe) {
        if (!isAdded() || recipe == null) return;

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String recipeId = recipe.getId();
        if (recipeId.isEmpty()) return;

        if (favoriteIds.contains(recipeId)) {
            db.collection(Constants.COLLECTION_USERS)
                    .document(user.getUid())
                    .collection(Constants.COLLECTION_FAVORITES)
                    .document(recipeId)
                    .delete()
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Failed to remove favorite", Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put(Constants.FIELD_FAVORITED_AT, FieldValue.serverTimestamp());

        db.collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .collection(Constants.COLLECTION_FAVORITES)
                .document(recipeId)
                .set(data)
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to add favorite", Toast.LENGTH_SHORT).show();
                });
    }

    private void showEmptyState(boolean isEmpty) {
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (favoritesListener != null) {
            favoritesListener.remove();
            favoritesListener = null;
        }
        if (recipesListener != null) {
            recipesListener.remove();
            recipesListener = null;
        }
    }
}
