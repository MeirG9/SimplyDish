package com.example.simplydish.utils;

import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.simplydish.R;

public final class ImageUtils {

    private ImageUtils() {
        // Utility class
    }

    public static void loadImage(@Nullable ImageView imageView, @Nullable String url) {
        if (imageView == null) return;

        final int placeholderRes = R.drawable.ic_food_placeholder;

        // Bind to the View lifecycle to reduce leaks and avoid stale Context usage.
        Glide.with(imageView)
                .load(TextUtils.isEmpty(url) ? null : url)
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .fallback(placeholderRes)
                .centerCrop()
                .into(imageView);
    }
}
