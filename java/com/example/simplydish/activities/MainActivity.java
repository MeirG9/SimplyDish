package com.example.simplydish.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.simplydish.R;
import com.example.simplydish.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Setup ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        // 2. Setup Navigation
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
            setupVisibilityRules();
        }

        // 3. Auth Logic Check
        if (auth.getCurrentUser() == null) {
            // Navigate to login if user is not signed in
            // Use a post action to ensure graph is ready
            binding.getRoot().post(() -> {
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() != R.id.nav_login) {
                    navController.navigate(R.id.nav_login);
                }
            });
        }

        // 4. Global FAB Navigation
        binding.fabAddRecipe.setOnClickListener(v ->
                navController.navigate(R.id.action_global_add_recipe)
        );
    }

    private void setupVisibilityRules() {
        // Define top-level destinations where BottomNav & FAB should be visible
        Set<Integer> topLevelDestinations = Set.of(
                R.id.nav_feed,
                R.id.nav_search,
                R.id.nav_favorites
        );

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean isTopLevel = topLevelDestinations.contains(destination.getId());
            int visibility = isTopLevel ? View.VISIBLE : View.GONE;

            binding.bottomNavigation.setVisibility(visibility);
            binding.fabAddRecipe.setVisibility(visibility);
        });
    }
}