package com.example.simplydish.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.simplydish.R;
import com.example.simplydish.databinding.FragmentAddRecipeBinding;
import com.example.simplydish.utils.Constants;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AddRecipeFragment extends Fragment {

    private FragmentAddRecipeBinding binding;
    private Uri imageUri;
    private final Set<String> ingredientsSet = new HashSet<>();

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // New API for results
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imageUri = uri;
                    Glide.with(this).load(uri).centerCrop().into(binding.ivRecipeImage);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddRecipeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        setupUI();
    }

    private void setupUI() {
        // Dropdown setup
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.common_ingredients));
        binding.etIngredientInput.setAdapter(adapter);

        // Listeners
        binding.layoutImagePicker.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        binding.btnAddIngredient.setOnClickListener(v -> addIngredient());

        binding.btnSaveRecipe.setOnClickListener(v -> {
            if (validateInputs()) startSaveProcess();
        });
    }

    private void addIngredient() {
        String raw = binding.etIngredientInput.getText().toString().trim();
        if (raw.isEmpty()) return;

        String normalized = raw.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (ingredientsSet.contains(normalized)) {
            binding.etIngredientInput.setText("");
            return;
        }

        Chip chip = new Chip(requireContext());
        chip.setText(raw);
        chip.setTag(normalized);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            ingredientsSet.remove(normalized);
            binding.chipGroupIngredients.removeView(chip);
        });

        ingredientsSet.add(normalized);
        binding.chipGroupIngredients.addView(chip);
        binding.etIngredientInput.setText("");
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(binding.etTitle.getText())) {
            binding.etTitle.setError("Title required");
            return false;
        }
        if (binding.spinnerCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(requireContext(), "Select a category", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (ingredientsSet.isEmpty()) {
            binding.etIngredientInput.setError("Add ingredients");
            return false;
        }
        return true;
    }

    // --- Refactored Save Logic (Split methods) ---

    private void startSaveProcess() {
        setLoading(true);
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        DocumentReference recipeRef = db.collection(Constants.COLLECTION_RECIPES).document();

        if (imageUri != null) {
            uploadImageAndSave(user.getUid(), recipeRef);
        } else {
            saveRecipeToFirestore(user.getUid(), recipeRef, null);
        }
    }

    private void uploadImageAndSave(String userId, DocumentReference recipeRef) {
        StorageReference ref = storage.getReference().child("recipe_images/" + recipeRef.getId() + ".jpg");

        ref.putFile(imageUri)
                .addOnSuccessListener(task -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> saveRecipeToFirestore(userId, recipeRef, uri.toString()))
                        .addOnFailureListener(e -> handleError(e.getMessage())))
                .addOnFailureListener(e -> handleError(e.getMessage()));
    }

    private void saveRecipeToFirestore(String userId, DocumentReference recipeRef, @Nullable String imageUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put(Constants.FIELD_AUTHOR_UID, userId);
        data.put(Constants.FIELD_TITLE, getText(binding.etTitle));
        data.put(Constants.FIELD_DESCRIPTION, getText(binding.etDescription));
        data.put(Constants.FIELD_CATEGORY, binding.spinnerCategory.getSelectedItem().toString());
        data.put(Constants.FIELD_DIRECTIONS, getText(binding.etDirections));
        data.put(Constants.FIELD_IMAGE_URL, imageUrl);
        data.put(Constants.FIELD_INGREDIENTS, new ArrayList<>(ingredientsSet));
        data.put(Constants.FIELD_CREATED_AT, System.currentTimeMillis());

        recipeRef.set(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).popBackStack(); // Go back to Feed
                })
                .addOnFailureListener(e -> handleError(e.getMessage()));
    }

    private String getText(TextView tv) {
        return tv.getText() != null ? tv.getText().toString().trim() : "";
    }

    private void handleError(String msg) {
        setLoading(false);
        Toast.makeText(requireContext(), "Error: " + msg, Toast.LENGTH_SHORT).show();
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnSaveRecipe.setEnabled(!isLoading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}