package com.example.simplydish.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.simplydish.R;
import com.example.simplydish.adapters.RecipeAdapter;
import com.example.simplydish.api.MealApi;
import com.example.simplydish.api.MealResponse;
import com.example.simplydish.databinding.FragmentSearchBinding;
import com.example.simplydish.models.Recipe;
import com.example.simplydish.utils.Constants;
import com.example.simplydish.utils.FirebaseFavoritesHelper;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private RecipeAdapter adapter;
    private final List<Recipe> recipes = new ArrayList<>();
    private final Set<String> favoriteIds = new HashSet<>();

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    @Nullable private ListenerRegistration favoritesListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupAutoComplete();
        setupRecyclerView();
        setupListeners();
        listenToFavorites();
    }

    private void setupAutoComplete() {
        String[] ingredientsArray = getResources().getStringArray(R.array.common_ingredients);
        ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ingredientsArray
        );
        binding.etSearchIngredient.setAdapter(autoCompleteAdapter);
    }

    private void setupRecyclerView() {
        binding.recyclerViewSearch.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RecipeAdapter(requireContext(), recipes);
        binding.recyclerViewSearch.setAdapter(adapter);

        adapter.setOnItemClickListener(recipe -> {
            SearchFragmentDirections.ActionSearchToDetail action =
                    SearchFragmentDirections.actionSearchToDetail(recipe);
            Navigation.findNavController(binding.getRoot()).navigate(action);
        });

        // Simple toggle for list items, detailed logic in DetailFragment
        adapter.setOnFavoriteClickListener(this::toggleFavoriteSimple);
    }

    private void setupListeners() {
        binding.btnAddSearchTag.setOnClickListener(v -> {
            String raw = binding.etSearchIngredient.getText().toString().trim();
            if (raw.isEmpty()) return;

            String normalized = raw.toLowerCase(Locale.ROOT).trim();
            if (!isTagAlreadyAdded(normalized)) {
                addChip(raw, normalized);
            }
            binding.etSearchIngredient.setText("");
        });

        binding.btnDoSearch.setOnClickListener(v -> performSearch());
    }

    private void addChip(String text, String normalized) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setTag(normalized);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> binding.chipGroupSearch.removeView(chip));
        binding.chipGroupSearch.addView(chip);
    }

    private boolean isTagAlreadyAdded(String normalized) {
        for (int i = 0; i < binding.chipGroupSearch.getChildCount(); i++) {
            View child = binding.chipGroupSearch.getChildAt(i);
            if (child instanceof Chip && normalized.equals(child.getTag())) return true;
        }
        return false;
    }

    private void performSearch() {
        hideKeyboard();
        List<String> ingredients = getSelectedIngredients();

        if (ingredients.isEmpty()) {
            Toast.makeText(requireContext(), "Please add ingredients", Toast.LENGTH_SHORT).show();
            return;
        }

        if (binding.cbSearchOnline.isChecked()) {
            searchOnline(ingredients.get(0)); // MealDB supports 1 ingredient filter
        } else {
            searchFirestore(ingredients);
        }
    }

    private void searchOnline(String ingredient) {
        setLoading(true);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.themealdb.com/api/json/v1/1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        retrofit.create(MealApi.class).getMealsByIngredient(ingredient).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                if (binding == null) return;
                setLoading(false);

                recipes.clear();
                if (response.body() != null && response.body().meals != null) {
                    for (MealResponse.MealDto dto : response.body().meals) {
                        recipes.add(dto.toSystemRecipe());
                    }
                }
                updateUI();
            }

            @Override
            public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                if (binding == null) return;
                setLoading(false);
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchFirestore(List<String> ingredients) {
        setLoading(true);
        // Splitting into chunks of 10 for 'array-contains-any' limitation if needed,
        // simplified here for clarity assuming < 10 tags usually.
        db.collection(Constants.COLLECTION_RECIPES)
                .whereArrayContainsAny(Constants.FIELD_INGREDIENTS, ingredients)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (binding == null) return;
                    recipes.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        Recipe r = doc.toObject(Recipe.class);
                        if (r != null) {
                            r.setId(doc.getId());
                            recipes.add(r);
                        }
                    }
                    // Simple client-side sort by relevance (count matches) could be added here
                    Collections.sort(recipes, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    setLoading(false);
                    updateUI();
                });
    }

    private void updateUI() {
        adapter.updateData(recipes);
        adapter.updateFavorites(new ArrayList<>(favoriteIds));
        binding.tvNoResults.setVisibility(recipes.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void toggleFavoriteSimple(Recipe recipe) {
        if (recipe == null || recipe.getId() == null) return;

        if (recipe.getId().startsWith("mealdb_")) {
            Toast.makeText(requireContext(),
                    "Open details to save online recipes",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isCurrentlyFavorite = favoriteIds.contains(recipe.getId());

        FirebaseFavoritesHelper.toggleFavorite(
                recipe,
                isCurrentlyFavorite,
                () -> Toast.makeText(requireContext(),
                        "Failed to update favorite",
                        Toast.LENGTH_SHORT).show()
        );
    }


    private void listenToFavorites() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        favoritesListener = db.collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .collection(Constants.COLLECTION_FAVORITES)
                .addSnapshotListener((snapshots, e) -> {
                    if (binding == null || snapshots == null) return;
                    favoriteIds.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        favoriteIds.add(doc.getId());
                    }
                    adapter.updateFavorites(new ArrayList<>(favoriteIds));
                });
    }

    private List<String> getSelectedIngredients() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < binding.chipGroupSearch.getChildCount(); i++) {
            View child = binding.chipGroupSearch.getChildAt(i);
            if (child instanceof Chip) {
                list.add((String) child.getTag());
            }
        }
        return list;
    }

    private void hideKeyboard() {
        View view = requireActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void setLoading(boolean isLoading) {
        binding.progressBarSearch.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.recyclerViewSearch.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        binding.tvNoResults.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (favoritesListener != null) favoritesListener.remove();
        binding = null;
    }
}