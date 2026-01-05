package com.example.simplydish.models;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {

    @DocumentId
    @Nullable
    private String uid;

    @Nullable
    private String name;

    @Nullable
    private String email;

    public User() {
        // Required public no-arg constructor for Firestore
    }

    public User(@Nullable String name, @Nullable String email) {
        this.name = name;
        this.email = email;
    }

    @Nullable
    public String getUid() {
        return uid;
    }

    public void setUid(@Nullable String uid) {
        this.uid = uid;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    public void setEmail(@Nullable String email) {
        this.email = email;
    }
}
