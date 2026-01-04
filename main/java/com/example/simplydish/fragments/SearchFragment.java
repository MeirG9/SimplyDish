package com.example.simplydish.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.example.simplydish.api.MealApi;
import com.example.simplydish.api.MealResponse;
import com.example.simplydish.models.Recipe;
import com.example.simplydish.utils.Constants;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SearchFragment extends Fragment {

    private AutoCompleteTextView etSearchIngredient;
    private Button btnAddSearchTag;
    private Button btnDoSearch;
    private ChipGroup chipGroupSearch;
    private CheckBox cbSearchOnline; // Added for Hybrid Search

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvNoResults;

    private RecipeAdapter adapter;
    private final List<Recipe> recipes = new ArrayList<>();
    private final Set<String> favoriteIds = new HashSet<>();

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    private ListenerRegistration favoritesListener;

    public SearchFragment() { }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews(view);
        setupAutoComplete();
        setupRecyclerView();
        bindListeners();
        listenToFavorites();
    }

    private void bindViews(@NonNull View view) {
        etSearchIngredient = view.findViewById(R.id.etSearchIngredient);
        btnAddSearchTag = view.findViewById(R.id.btnAddSearchTag);
        btnDoSearch = view.findViewById(R.id.btnDoSearch);
        chipGroupSearch = view.findViewById(R.id.chipGroupSearch);
        cbSearchOnline = view.findViewById(R.id.cbSearchOnline); // Bind CheckBox

        recyclerView = view.findViewById(R.id.recyclerViewSearch);
        progressBar = view.findViewById(R.id.progressBarSearch);
        tvNoResults = view.findViewById(R.id.tvNoResults);
    }

    private void setupAutoComplete() {
        String[] ingredientsArray = getResources().getStringArray(R.array.common_ingredients);
        ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ingredientsArray
        );
        etSearchIngredient.setAdapter(autoCompleteAdapter);
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

        // The list item favorite click is simple toggle (timestamp).
        // Complex logic for importing is handled inside DetailActivity or can be mirrored here if needed.
        adapter.setOnFavoriteClickListener(this::toggleFavoriteListItem);
        adapter.updateFavorites(new ArrayList<>(favoriteIds));
    }

    private void bindListeners() {
        btnAddSearchTag.setOnClickListener(v -> {
            String raw = getTextTrimmed(etSearchIngredient);
            if (TextUtils.isEmpty(raw)) return;

            String normalized = normalizeIngredient(raw);
            if (TextUtils.isEmpty(normalized) || hasChipWithTag(normalized)) {
                etSearchIngredient.setText("");
                return;
            }

            addChip(raw, normalized);
            etSearchIngredient.setText("");
        });

        btnDoSearch.setOnClickListener(v -> performSearch());
    }

    private void addChip(@NonNull String displayText, @NonNull String normalizedValue) {
        Chip chip = new Chip(requireContext());
        chip.setText(displayText);
        chip.setTag(normalizedValue);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> chipGroupSearch.removeView(chip));
        chipGroupSearch.addView(chip);
    }

    private boolean hasChipWithTag(@NonNull String tagValue) {
        for (int i = 0; i < chipGroupSearch.getChildCount(); i++) {
            View child = chipGroupSearch.getChildAt(i);
            if (!(child instanceof Chip)) continue;

            Object tag = child.getTag();
            if (tagValue.equals(tag)) return true;
        }
        return false;
    }

    private void performSearch() {
        hideKeyboard();

        List<String> ingredients = getSelectedIngredientsNormalized();
        if (ingredients.isEmpty()) {
            Toast.makeText(requireContext(), "Please add at least one ingredient", Toast.LENGTH_SHORT).show();
            recipes.clear();
            adapter.updateData(recipes);
            tvNoResults.setVisibility(View.VISIBLE);
            return;
        }

        // --- Hybrid Search Logic ---
        if (cbSearchOnline.isChecked()) {
            // Online Search: Takes the first ingredient (TheMealDB restriction)
            searchOnline(ingredients.get(0));
        } else {
            // Existing Firestore Search
            searchFirestore(ingredients);
        }
    }

    private void searchOnline(String ingredient) {
        setLoading(true);
        tvNoResults.setVisibility(View.GONE);

        // Build Retrofit Instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.themealdb.com/api/json/v1/1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MealApi api = retrofit.create(MealApi.class);

        api.getMealsByIngredient(ingredient).enqueue(new Callback<MealResponse>() {
            @Override
            public void onResponse(@NonNull Call<MealResponse> call, @NonNull Response<MealResponse> response) {
                if (!isAdded()) return;
                setLoading(false);

                List<Recipe> apiRecipes = new ArrayList<>();
                if (response.body() != null && response.body().meals != null) {
                    for (MealResponse.MealDto dto : response.body().meals) {
                        apiRecipes.add(dto.toSystemRecipe()); // Convert using DTO logic
                    }
                }

                recipes.clear();
                recipes.addAll(apiRecipes);
                adapter.updateData(recipes);
                adapter.updateFavorites(new ArrayList<>(favoriteIds));

                tvNoResults.setVisibility(recipes.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(@NonNull Call<MealResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                setLoading(false);
                Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchFirestore(List<String> ingredients) {
        setLoading(true);
        tvNoResults.setVisibility(View.GONE);

        List<List<String>> chunks = chunkList(ingredients, 10);
        List<Task<QuerySnapshot>> tasks = new ArrayList<>(chunks.size());

        for (List<String> chunk : chunks) {
            tasks.add(
                    db.collection(Constants.COLLECTION_RECIPES)
                            .whereArrayContainsAny(Constants.FIELD_INGREDIENTS, chunk)
                            .get()
            );
        }

        Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    if (!isAdded() || getView() == null) return;

                    Map<String, Recipe> uniqueById = new HashMap<>();

                    for (Object result : results) {
                        QuerySnapshot snapshot = (QuerySnapshot) result;
                        if (snapshot == null) continue;

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Recipe r = doc.toObject(Recipe.class);
                            if (r == null) continue;

                            r.setId(doc.getId()); // Always enforce ID for favorites/details.
                            uniqueById.put(doc.getId(), r);
                        }
                    }

                    recipes.clear();
                    recipes.addAll(uniqueById.values());

                    Collections.sort(recipes, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

                    adapter.updateData(recipes);
                    adapter.updateFavorites(new ArrayList<>(favoriteIds));

                    tvNoResults.setVisibility(recipes.isEmpty() ? View.VISIBLE : View.GONE);
                    setLoading(false);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getView() == null) return;
                    setLoading(false);
                    Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show();
                });
    }

    private List<String> getSelectedIngredientsNormalized() {
        Set<String> out = new HashSet<>();

        for (int i = 0; i < chipGroupSearch.getChildCount(); i++) {
            View child = chipGroupSearch.getChildAt(i);
            if (!(child instanceof Chip)) continue;

            Object tag = child.getTag();
            if (!(tag instanceof String)) continue;

            String normalized = (String) tag;
            if (!TextUtils.isEmpty(normalized)) out.add(normalized);
        }

        return new ArrayList<>(out);
    }

    private <T> List<List<T>> chunkList(@NonNull List<T> list, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        int i = 0;
        while (i < list.size()) {
            int end = Math.min(i + chunkSize, list.size());
            chunks.add(new ArrayList<>(list.subList(i, end)));
            i = end;
        }
        return chunks;
    }

    /**
     * Simple toggle for list items.
     * Note: For full "Import" logic of online recipes, users should typically enter Detail view.
     * However, if you want to support direct favoriting from list, check RecipeDetailActivity logic.
     */
    private void toggleFavoriteListItem(@Nullable Recipe recipe) {
        if (!isAdded() || recipe == null) return;

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String recipeId = recipe.getId();
        if (TextUtils.isEmpty(recipeId)) return;

        // If online recipe, users must enter details to favorite (simplest approach to ensure safe saving)
        // Or implement the RecipeDetailActivity logic here.
        if (recipeId.startsWith("mealdb_")) {
            Toast.makeText(requireContext(), "Tap recipe to view details & save", Toast.LENGTH_SHORT).show();
            return;
        }

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

    private void hideKeyboard() {
        if (!isAdded()) return;

        View focused = requireActivity().getCurrentFocus();
        if (focused == null) return;

        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
    }

    private String getTextTrimmed(@Nullable TextView view) {
        if (view == null) return "";
        CharSequence cs = view.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    private String normalizeIngredient(@Nullable String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (favoritesListener != null) {
            favoritesListener.remove();
            favoritesListener = null;
        }
    }
}