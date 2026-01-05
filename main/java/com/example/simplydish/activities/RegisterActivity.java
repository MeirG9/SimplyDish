package com.example.simplydish.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.simplydish.R;
import com.example.simplydish.utils.Constants;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private Button btnRegister;
    private TextView tvLoginLink;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() != null) {
            navigateToMain();
            return;
        }

        bindViews();
        bindListeners();
    }

    private void bindViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
    }

    private void bindListeners() {
        tvLoginLink.setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> performRegister());
    }

    private void performRegister() {
        final String name = getTextTrimmed(etName);
        final String email = getTextTrimmed(etEmail);
        final String password = getTextTrimmed(etPassword);

        if (!validate(name, email, password)) return;

        setLoading(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> saveUserProfile(result.getUser(), name))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validate(String name, String email, String password) {
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            etName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email");
            etEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void saveUserProfile(@Nullable FirebaseUser user, String name) {
        if (user == null) {
            setLoading(false);
            Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
            return;
        }

        final String uid = user.getUid();

        Map<String, Object> data = new HashMap<>();
        data.put(Constants.FIELD_NAME, name);
        data.put(Constants.FIELD_EMAIL, user.getEmail());

        db.collection(Constants.COLLECTION_USERS)
                .document(uid) // users/{uid}
                .set(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean isLoading) {
        btnRegister.setEnabled(!isLoading);
        tvLoginLink.setEnabled(!isLoading);
    }

    private String getTextTrimmed(@Nullable TextInputEditText editText) {
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString().trim();
    }

    private void navigateToMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
