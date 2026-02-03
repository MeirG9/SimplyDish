package com.example.simplydish.models;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@IgnoreExtraProperties
public class Recipe implements Serializable {

    private static final long serialVersionUID = 1L;

    @DocumentId
    @Nullable
    private String id;

    @Nullable
    private String authorUid;

    @Nullable
    private String title;

    @Nullable
    private String description;

    @Nullable
    private String category;

    @Nullable
    private String imageUrl;

    @Nullable
    private List<String> ingredients;

    @Nullable
    private String directions;

    private long createdAt;

    public Recipe() {
        // Required public no-arg constructor for Firestore
    }

    public Recipe(
            @Nullable String authorUid,
            @Nullable String title,
            @Nullable String description,
            @Nullable String category,
            @Nullable String imageUrl,
            @Nullable List<String> ingredients,
            @Nullable String directions,
            long createdAt
    ) {
        this.authorUid = authorUid;
        this.title = title;
        this.description = description;
        this.category = category;
        this.imageUrl = imageUrl;
        this.ingredients = ingredients;
        this.directions = directions;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(@Nullable String id) {
        this.id = id;
    }

    public String getAuthorUid() {
        return authorUid != null ? authorUid : "";
    }

    public void setAuthorUid(@Nullable String authorUid) {
        this.authorUid = authorUid;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public String getCategory() {
        return category != null ? category : "";
    }

    public void setCategory(@Nullable String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl != null ? imageUrl : "";
    }

    public void setImageUrl(@Nullable String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<String> getIngredients() {
        return ingredients != null ? ingredients : new ArrayList<>();
    }

    public void setIngredients(@Nullable List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public String getDirections() {
        return directions != null ? directions : "";
    }

    public void setDirections(@Nullable String directions) {
        this.directions = directions;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Recipe)) return false;
        Recipe other = (Recipe) o;

        if (id == null || id.isEmpty() || other.id == null || other.id.isEmpty()) {
            return false;
        }
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return (id == null || id.isEmpty()) ? System.identityHashCode(this) : id.hashCode();
    }
}
