package com.example.simplydish.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.simplydish.R;
import com.example.simplydish.databinding.FragmentLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        auth = FirebaseAuth.getInstance();

        // Already logged in check is handled by MainActivity now,
        // but double check here just in case specific flow lands here.
        if (auth.getCurrentUser() != null) {
            navigateToFeed();
            return;
        }

        binding.tvRegisterLink.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_login_to_register)
        );

        binding.btnLogin.setOnClickListener(v -> performLogin());
    }

    private void performLogin() {
        String email = getTextTrimmed(binding.etEmail.getText());
        String password = getTextTrimmed(binding.etPassword.getText());

        if (!validate(email, password)) return;

        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    Toast.makeText(requireContext(), "Login successful", Toast.LENGTH_SHORT).show();
                    navigateToFeed();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String error = e.getMessage() != null ? e.getMessage() : "Login failed";
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validate(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError(getString(R.string.hint_email) + " is required"); // Using resources
            binding.etEmail.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Invalid email format");
            binding.etEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError(getString(R.string.hint_password) + " is required");
            binding.etPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void setLoading(boolean isLoading) {
        binding.btnLogin.setEnabled(!isLoading);
        binding.tvRegisterLink.setEnabled(!isLoading);
    }

    private String getTextTrimmed(CharSequence text) {
        return text == null ? "" : text.toString().trim();
    }

    private void navigateToFeed() {
        // Check if attached to avoid crashes
        if (isAdded()) {
            Navigation.findNavController(requireView()).navigate(R.id.action_login_to_feed);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }
}