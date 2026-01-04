package com.example.simplydish.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.simplydish.R;
import com.example.simplydish.models.Recipe;
import com.example.simplydish.utils.ImageUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(@NonNull Recipe recipe);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(@NonNull Recipe recipe);
    }

    private static final Object PAYLOAD_FAVORITE = new Object();

    @NonNull
    private final LayoutInflater inflater;

    @NonNull
    private final AsyncListDiffer<Recipe> differ;

    @NonNull
    private Set<String> favoriteIds = new HashSet<>();

    @Nullable
    private OnItemClickListener itemClickListener;

    @Nullable
    private OnFavoriteClickListener favoriteClickListener;

    public RecipeAdapter(@NonNull android.content.Context context, @NonNull List<Recipe> initialRecipes) {
        this.inflater = LayoutInflater.from(context);
        this.differ = new AsyncListDiffer<>(this, DIFF_CALLBACK);
        updateData(initialRecipes);
    }

    private static final DiffUtil.ItemCallback<Recipe> DIFF_CALLBACK = new DiffUtil.ItemCallback<Recipe>() {
        @Override
        public boolean areItemsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
            String oldId = oldItem.getId();
            String newId = newItem.getId();
            if (TextUtils.isEmpty(oldId) || TextUtils.isEmpty(newId)) return false;
            return oldId.equals(newId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
            return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                    && Objects.equals(oldItem.getCategory(), newItem.getCategory())
                    && Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl())
                    && oldItem.getCreatedAt() == newItem.getCreatedAt();
        }
    };

    public void setOnItemClickListener(@Nullable OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnFavoriteClickListener(@Nullable OnFavoriteClickListener listener) {
        this.favoriteClickListener = listener;
    }

    public void updateData(@Nullable List<Recipe> newRecipes) {
        List<Recipe> safeCopy = newRecipes == null ? new ArrayList<>() : new ArrayList<>(newRecipes);
        differ.submitList(safeCopy);
    }

    public void updateFavorites(@Nullable List<String> newFavoriteIds) {
        Set<String> newSet = newFavoriteIds == null ? new HashSet<>() : new HashSet<>(newFavoriteIds);
        if (newSet.equals(favoriteIds)) return;

        Set<String> oldSet = favoriteIds;
        favoriteIds = newSet;

        List<Recipe> current = differ.getCurrentList();
        for (int i = 0; i < current.size(); i++) {
            String id = current.get(i).getId();
            if (TextUtils.isEmpty(id)) continue;

            boolean wasFav = oldSet.contains(id);
            boolean isFav = favoriteIds.contains(id);
            if (wasFav != isFav) {
                notifyItemChanged(i, PAYLOAD_FAVORITE);
            }
        }
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = differ.getCurrentList().get(position);

        holder.tvTitle.setText(recipe.getTitle());
        holder.tvCategory.setText(recipe.getCategory());
        ImageUtils.loadImage(holder.ivImage, recipe.getImageUrl());

        bindFavoriteState(holder, recipe);

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) itemClickListener.onItemClick(recipe);
        });

        holder.btnFavorite.setOnClickListener(v -> {
            if (favoriteClickListener != null) favoriteClickListener.onFavoriteClick(recipe);
        });
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.contains(PAYLOAD_FAVORITE)) {
            Recipe recipe = differ.getCurrentList().get(position);
            bindFavoriteState(holder, recipe);
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    private void bindFavoriteState(@NonNull RecipeViewHolder holder, @NonNull Recipe recipe) {
        boolean isFavorite = isFavorite(recipe);

        holder.btnFavorite.setImageResource(
                isFavorite ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
        );
        holder.btnFavorite.setColorFilter(
                ContextCompat.getColor(
                        holder.itemView.getContext(),
                        isFavorite ? R.color.primary : android.R.color.darker_gray
                )
        );
    }

    private boolean isFavorite(@NonNull Recipe recipe) {
        String id = recipe.getId();
        return !TextUtils.isEmpty(id) && favoriteIds.contains(id);
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {

        final TextView tvTitle;
        final TextView tvCategory;
        final ImageView ivImage;
        final ImageView btnFavorite;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvCategory = itemView.findViewById(R.id.tvItemCategory);
            ivImage = itemView.findViewById(R.id.ivItemImage);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }
}
