package com.example.simplydish.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.simplydish.R;
import com.example.simplydish.utils.Constants;
import com.example.simplydish.utils.ImageUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AddRecipeActivity extends AppCompatActivity {

    private View layoutImagePicker;
    private ImageView ivRecipeImage;

    private TextInputEditText etTitle;
    private TextInputEditText etDescription;
    private TextInputEditText etDirections;

    private AutoCompleteTextView etIngredientInput;
    private Spinner spinnerCategory;

    private Button btnAddIngredient;
    private Button btnSaveRecipe;

    private ChipGroup chipGroupIngredients;
    private ProgressBar progressBar;

    @Nullable
    private Uri imageUri;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private final Set<String> normalizedIngredientsSet = new HashSet<>();

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;

                imageUri = uri;
                ImageUtils.loadImage(ivRecipeImage, uri.toString());
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            navigateToLogin();
            return;
        }

        bindViews();
        setupIngredientsAutoComplete();
        bindListeners();
    }

    private void bindViews() {
        layoutImagePicker = findViewById(R.id.layoutImagePicker);
        ivRecipeImage = findViewById(R.id.ivRecipeImage);

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etDirections = findViewById(R.id.etDirections);

        etIngredientInput = findViewById(R.id.etIngredientInput);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        btnAddIngredient = findViewById(R.id.btnAddIngredient);
        btnSaveRecipe = findViewById(R.id.btnSaveRecipe);

        chipGroupIngredients = findViewById(R.id.chipGroupIngredients);
        progressBar = findViewById(R.id.progressBar);

        ivRecipeImage.setImageResource(R.drawable.ic_food_placeholder);
    }

    private void setupIngredientsAutoComplete() {
        String[] items = getResources().getStringArray(R.array.common_ingredients);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                items
        );
        etIngredientInput.setAdapter(adapter);
    }

    private void bindListeners() {
        layoutImagePicker.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnAddIngredient.setOnClickListener(v -> {
            String raw = getTextTrimmed(etIngredientInput);
            if (TextUtils.isEmpty(raw)) return;

            String normalized = normalizeIngredient(raw);
            if (TextUtils.isEmpty(normalized) || normalizedIngredientsSet.contains(normalized)) {
                etIngredientInput.setText("");
                return;
            }

            addIngredientChip(raw, normalized);
            etIngredientInput.setText("");
        });

        btnSaveRecipe.setOnClickListener(v -> {
            if (!validateInputs()) return;
            saveRecipe();
        });
    }

    private void addIngredientChip(String displayText, String normalizedValue) {
        Chip chip = new Chip(this);
        chip.setText(displayText);
        chip.setTag(normalizedValue);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            normalizedIngredientsSet.remove(normalizedValue);
            chipGroupIngredients.removeView(chip);
        });

        normalizedIngredientsSet.add(normalizedValue);
        chipGroupIngredients.addView(chip);
    }

    private boolean validateInputs() {
        String title = getTextTrimmed(etTitle);
        String description = getTextTrimmed(etDescription);

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return false;
        }

        if (spinnerCategory.getSelectedItemPosition() == 0) {
            TextView selected = (TextView) spinnerCategory.getSelectedView();
            if (selected != null) {
                selected.setError(" ");
                selected.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
                selected.setText("Select a category");
            }
            return false;
        }

        if (chipGroupIngredients.getChildCount() == 0) {
            etIngredientInput.setError("Add at least one ingredient");
            etIngredientInput.requestFocus();
            return false;
        }

        return true;
    }

    private void saveRecipe() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            navigateToLogin();
            return;
        }

        setLoading(true);

        DocumentReference recipeRef = db.collection(Constants.COLLECTION_RECIPES).document();
        String recipeId = recipeRef.getId();

        if (imageUri == null) {
            writeRecipe(recipeRef, user.getUid(), null);
            return;
        }

        StorageReference imageRef = storage.getReference().child("recipe_images/" + recipeId + ".jpg");
        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri ->
                                        writeRecipe(recipeRef, user.getUid(), downloadUri.toString())
                                )
                                .addOnFailureListener(e -> {
                                    setLoading(false);
                                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                })
                )
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void writeRecipe(DocumentReference recipeRef, String authorUid, @Nullable String imageUrl) {
        String title = getTextTrimmed(etTitle);
        String description = getTextTrimmed(etDescription);
        String category = String.valueOf(spinnerCategory.getSelectedItem());
        String directions = getTextTrimmed(etDirections);

        List<String> ingredients = new ArrayList<>();
        for (int i = 0; i < chipGroupIngredients.getChildCount(); i++) {
            View child = chipGroupIngredients.getChildAt(i);
            if (!(child instanceof Chip)) continue;

            Object tag = child.getTag();
            String normalized = tag instanceof String
                    ? (String) tag
                    : normalizeIngredient(((Chip) child).getText().toString());

            if (!TextUtils.isEmpty(normalized)) ingredients.add(normalized);
        }

        Map<String, Object> data = new HashMap<>();
        data.put(Constants.FIELD_AUTHOR_UID, authorUid);
        data.put(Constants.FIELD_TITLE, title);
        data.put(Constants.FIELD_DESCRIPTION, description);
        data.put(Constants.FIELD_CATEGORY, category);
        data.put(Constants.FIELD_IMAGE_URL, imageUrl);
        data.put(Constants.FIELD_INGREDIENTS, ingredients);
        data.put(Constants.FIELD_DIRECTIONS, directions);
        data.put(Constants.FIELD_CREATED_AT, System.currentTimeMillis());

        recipeRef.set(data)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, "Recipe saved", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);

        btnSaveRecipe.setEnabled(!isLoading);
        btnAddIngredient.setEnabled(!isLoading);
        etIngredientInput.setEnabled(!isLoading);
        layoutImagePicker.setEnabled(!isLoading);
    }

    private String getTextTrimmed(@Nullable TextView view) {
        if (view == null) return "";
        CharSequence text = view.getText();
        return text == null ? "" : text.toString().trim();
    }

    private String normalizeIngredient(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized;
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
