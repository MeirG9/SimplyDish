package com.example.simplydish.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.simplydish.R;
import com.example.simplydish.databinding.FragmentRecipeDetailBinding;
import com.example.simplydish.models.Recipe;
import com.example.simplydish.utils.Constants;
import com.example.simplydish.utils.FirebaseFavoritesHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RecipeDetailFragment extends Fragment {

    private FragmentRecipeDetailBinding binding;
    private Recipe recipe;
    private boolean isFavorite;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRecipeDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 1. Get Data from SafeArgs
        if (getArguments() != null) {
            recipe = RecipeDetailFragmentArgs.fromBundle(getArguments()).getRecipe();
        }

        if (recipe == null) {
            Toast.makeText(requireContext(), "Recipe error", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }

        bindData();
        checkFavoriteStatus();

        binding.btnDetailFavorite.setOnClickListener(v -> toggleFavorite());
    }

    private void bindData() {
        binding.tvDetailTitle.setText(recipe.getTitle());
        binding.tvDetailCategory.setText(recipe.getCategory());
        binding.tvDetailDescription.setText(recipe.getDescription());
        binding.tvDetailDirections.setText(recipe.getDirections());

        StringBuilder sb = new StringBuilder();
        for (String ing : recipe.getIngredients()) {
            sb.append("• ").append(ing).append("\n");
        }
        binding.tvDetailIngredients.setText(sb.toString());

        // Glide instead of ImageUtils
        Glide.with(this)
                .load(recipe.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(binding.ivDetailImage);
    }

    private void checkFavoriteStatus() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || recipe.getId() == null) return;

        // Disable until checked
        binding.btnDetailFavorite.setEnabled(false);

        db.collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .collection(Constants.COLLECTION_FAVORITES)
                .document(recipe.getId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (binding == null) return;
                    isFavorite = snapshot.exists();
                    updateFavoriteIcon();
                    binding.btnDetailFavorite.setEnabled(true);
                });
    }

    private void toggleFavorite() {
        if (auth.getCurrentUser() == null) return;

        // Optimistic UI Update: Flip state immediately for UX
        isFavorite = !isFavorite;
        updateFavoriteIcon();

        // Delegate DB operations to Helper
        // We pass '!isFavorite' because we just flipped it (passing the previous DB state)
        FirebaseFavoritesHelper.toggleFavorite(recipe, !isFavorite, () -> {
            // Failure Callback: Revert UI changes if DB update fails
            if (binding != null) {
                isFavorite = !isFavorite;
                updateFavoriteIcon();
                Toast.makeText(requireContext(), "Action failed", Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void updateFavoriteIcon() {
        if (binding == null) return;
        binding.btnDetailFavorite.setImageResource(
                isFavorite ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
        );
        binding.btnDetailFavorite.setColorFilter(
                ContextCompat.getColor(requireContext(), isFavorite ? R.color.primary : android.R.color.darker_gray)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}