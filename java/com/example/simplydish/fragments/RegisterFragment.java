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
import com.example.simplydish.databinding.FragmentRegisterBinding;
import com.example.simplydish.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.tvLoginLink.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_register_to_login)
        );

        binding.btnRegister.setOnClickListener(v -> performRegister());
    }

    private void performRegister() {
        String name = getTextTrimmed(binding.etName.getText());
        String email = getTextTrimmed(binding.etEmail.getText());
        String password = getTextTrimmed(binding.etPassword.getText());

        if (!validate(name, email, password)) return;

        setLoading(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> saveUserProfile(result.getUser(), name))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserProfile(@Nullable FirebaseUser user, String name) {
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put(Constants.FIELD_NAME, name);
        data.put(Constants.FIELD_EMAIL, user.getEmail());

        db.collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .set(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(requireContext(), "Account created", Toast.LENGTH_SHORT).show();
                    if (isAdded()) {
                        Navigation.findNavController(requireView()).navigate(R.id.action_register_to_feed);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validate(String name, String email, String password) {
        if (TextUtils.isEmpty(name)) {
            binding.etName.setError("Name is required");
            return false;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Invalid email");
            return false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.etPassword.setError("Password must be at least 6 characters");
            return false;
        }
        return true;
    }

    private void setLoading(boolean isLoading) {
        binding.btnRegister.setEnabled(!isLoading);
        binding.tvLoginLink.setEnabled(!isLoading);
    }

    private String getTextTrimmed(CharSequence text) {
        return text == null ? "" : text.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}