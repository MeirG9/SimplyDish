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
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("FieldCanBeLocal")
public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private RecipeAdapter adapter;

    private final List<Recipe> favoriteRecipes = new ArrayList<>();
    private final List<String> favoriteIdsOrdered = new ArrayList<>();

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    private ListenerRegistration favoritesListener;

    public FavoritesFragment() { }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews(view);
        setupRecyclerView();
        listenToFavorites();
    }

    private void bindViews(@NonNull View view) {
        recyclerView = view.findViewById(R.id.recyclerViewFavorites);
        progressBar = view.findViewById(R.id.progressBarFavorites);
        tvEmptyState = view.findViewById(R.id.tvEmptyStateFavorites);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        adapter = new RecipeAdapter(requireContext(), favoriteRecipes);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(recipe -> {
            if (!isAdded()) return;

            Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
            intent.putExtra(Constants.EXTRA_RECIPE, recipe);
            startActivity(intent);
        });

        // In favorites screen, the star action removes from favorites.
        adapter.setOnFavoriteClickListener(this::removeFromFavorites);
    }

    private void listenToFavorites() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        if (favoritesListener != null) {
            favoritesListener.remove();
            favoritesListener = null;
        }

        setLoading(true);

        favoritesListener = db.collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .collection(Constants.COLLECTION_FAVORITES)
                .orderBy(Constants.FIELD_FAVORITED_AT, Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (!isAdded() || getView() == null) return;

                    if (e != null || snapshots == null) {
                        setLoading(false);
                        showEmptyState(favoriteRecipes.isEmpty());
                        return;
                    }

                    favoriteIdsOrdered.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        favoriteIdsOrdered.add(doc.getId()); // favorites/{recipeId}
                    }

                    if (favoriteIdsOrdered.isEmpty()) {
                        favoriteRecipes.clear();
                        adapter.updateData(favoriteRecipes);
                        adapter.updateFavorites(favoriteIdsOrdered);
                        setLoading(false);
                        showEmptyState(true);
                        return;
                    }

                    fetchRecipesByIds(favoriteIdsOrdered);
                });
    }

    private void fetchRecipesByIds(@NonNull List<String> orderedIds) {
        // Firestore whereIn supports up to 10 IDs per query.
        final List<List<String>> chunks = chunk(orderedIds, 10);
        final Map<String, Recipe> byId = new HashMap<>();

        final int totalQueries = chunks.size();
        final int[] completed = {0};

        for (List<String> idsChunk : chunks) {
            db.collection(Constants.COLLECTION_RECIPES)
                    .whereIn(FieldPath.documentId(), idsChunk)
                    .get()
                    .addOnSuccessListener(querySnapshots -> {
                        for (DocumentSnapshot doc : querySnapshots.getDocuments()) {
                            Recipe r = doc.toObject(Recipe.class);
                            if (r == null) continue;
                            r.setId(doc.getId());
                            byId.put(doc.getId(), r);
                        }

                        completed[0]++;
                        if (completed[0] == totalQueries) {
                            if (!isAdded() || getView() == null) return;

                            favoriteRecipes.clear();
                            for (String id : orderedIds) {
                                Recipe r = byId.get(id);
                                if (r != null) favoriteRecipes.add(r);
                            }

                            adapter.updateData(favoriteRecipes);
                            adapter.updateFavorites(orderedIds);
                            setLoading(false);
                            showEmptyState(favoriteRecipes.isEmpty());
                        }
                    })
                    .addOnFailureListener(err -> {
                        completed[0]++;
                        if (completed[0] == totalQueries) {
                            if (!isAdded() || getView() == null) return;

                            adapter.updateData(favoriteRecipes);
                            adapter.updateFavorites(orderedIds);
                            setLoading(false);
                            showEmptyState(favoriteRecipes.isEmpty());
                        }
                    });
        }
    }

    private void removeFromFavorites(@Nullable Recipe recipe) {
        FirebaseUser user = auth.getCurrentUser();
        if (!isAdded() || user == null || recipe == null) return;

        String recipeId = recipe.getId();
        if (recipeId.isEmpty()) return;

        db.collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .collection(Constants.COLLECTION_FAVORITES)
                .document(recipeId)
                .delete()
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(boolean isEmpty) {
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private List<List<String>> chunk(@NonNull List<String> input, int size) {
        List<List<String>> out = new ArrayList<>();
        int i = 0;
        while (i < input.size()) {
            int end = Math.min(i + size, input.size());
            out.add(new ArrayList<>(input.subList(i, end)));
            i = end;
        }
        return out;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (favoritesListener != null) {
            favoritesListener.remove();
            favoritesListener = null;
        }

        recyclerView = null;
        progressBar = null;
        tvEmptyState = null;
    }
}
