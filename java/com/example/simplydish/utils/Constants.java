package com.example.simplydish.utils;

public final class Constants {

    // Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_RECIPES = "recipes";
    public static final String COLLECTION_FAVORITES = "favorites";

    // User fields
    public static final String FIELD_NAME = "name";
    public static final String FIELD_EMAIL = "email";

    // Recipe fields
    public static final String FIELD_AUTHOR_UID = "authorUid";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_CATEGORY = "category";
    public static final String FIELD_IMAGE_URL = "imageUrl";
    public static final String FIELD_INGREDIENTS = "ingredients";
    public static final String FIELD_DIRECTIONS = "directions";
    public static final String FIELD_CREATED_AT = "createdAt";


    // Favorites fields
    public static final String FIELD_FAVORITED_AT = "favoritedAt";

    // Intents
    public static final String EXTRA_RECIPE = "recipe";

    private Constants() {
        // Utility class
    }
}
