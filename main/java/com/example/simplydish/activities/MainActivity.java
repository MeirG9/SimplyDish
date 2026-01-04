package com.example.simplydish.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.simplydish.R;
import com.example.simplydish.fragments.FavoritesFragment;
import com.example.simplydish.fragments.FeedFragment;
import com.example.simplydish.fragments.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            navigateToLogin();
            return;
        }

        bindViews();
        bindListeners();

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_feed);
        }
    }

    private void bindViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void bindListeners() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_feed) {
                fragment = new FeedFragment();
            } else if (itemId == R.id.nav_search) {
                fragment = new SearchFragment();
            } else if (itemId == R.id.nav_favorites) {
                fragment = new FavoritesFragment();
            } else {
                return false;
            }

            showFragment(fragment);
            return true;
        });

        findViewById(R.id.fab_add_recipe).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddRecipeActivity.class))
        );
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
