package com.example.simplydish.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.simplydish.adapters.RecipeAdapter;
import com.example.simplydish.databinding.FragmentFeedBinding;
import com.example.simplydish.models.Recipe;
import com.example.simplydish.utils.Constants;
import com.example.simplydish.utils.FirebaseFavoritesHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeedFragment extends Fragment {

    private FragmentFeedBinding binding;
    private RecipeAdapter adapter;

    private final List<Recipe> recipes = new ArrayList<>();
    private final Set<String> favoriteIds = new HashSet<>();

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable private ListenerRegistration favoritesListener;
    @Nullable private ListenerRegistration recipesListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFeedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupRecyclerView();
        listenToFavorites();
        listenToRecipes();
    }

    private void setupRecyclerView() {
        binding.recyclerViewFeed.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewFeed.setHasFixedSize(true);

        adapter = new RecipeAdapter(requireContext(), recipes);
        binding.recyclerViewFeed.setAdapter(adapter);

        // Navigation with SafeArgs
        adapter.setOnItemClickListener(recipe -> {
            FeedFragmentDirections.ActionFeedToDetail action =
                    FeedFragmentDirections.actionFeedToDetail(recipe); // Passing recipe object
            Navigation.findNavController(binding.getRoot()).navigate(action);
        });

        adapter.setOnFavoriteClickListener(this::toggleFavorite);
    }

    private void listenToFavorites() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        favoritesListener = db.collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .collection(Constants.COLLECTION_FAVORITES)
                .addSnapshotListener((snapshots, e) -> {
                    if (binding == null || e != null || snapshots == null) return;

                    favoriteIds.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        favoriteIds.add(doc.getId());
                    }
                    adapter.updateFavorites(new ArrayList<>(favoriteIds));
                });
    }

    private void listenToRecipes() {
        binding.progressBarFeed.setVisibility(View.VISIBLE);

        recipesListener = db.collection(Constants.COLLECTION_RECIPES)
                .orderBy(Constants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (binding == null) return;
                    binding.progressBarFeed.setVisibility(View.GONE);

                    if (e != null || snapshots == null) {
                        showEmptyState(recipes.isEmpty());
                        return;
                    }

                    recipes.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        if (recipe != null) {
                            recipe.setId(doc.getId());
                            recipes.add(recipe);
                        }
                    }

                    adapter.updateData(recipes);
                    adapter.updateFavorites(new ArrayList<>(favoriteIds));
                    showEmptyState(recipes.isEmpty());
                });
    }
    private void toggleFavorite(@Nullable Recipe recipe) {
        if (recipe == null || recipe.getId() == null) return;

        boolean isCurrentlyFavorite = favoriteIds.contains(recipe.getId());

        FirebaseFavoritesHelper.toggleFavorite(
                recipe,
                isCurrentlyFavorite,
                () -> Toast.makeText(requireContext(),
                        "Failed to update favorite",
                        Toast.LENGTH_SHORT).show()
        );
    }



    private void showEmptyState(boolean isEmpty) {
        binding.tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (favoritesListener != null) favoritesListener.remove();
        if (recipesListener != null) recipesListener.remove();
        binding = null;
    }
}