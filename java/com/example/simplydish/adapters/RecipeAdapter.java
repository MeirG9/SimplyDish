package com.example.simplydish.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.simplydish.R;
import com.example.simplydish.databinding.ItemRecipeBinding;
import com.example.simplydish.models.Recipe;

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
    private final AsyncListDiffer<Recipe> differ;
    private Set<String> favoriteIds = new HashSet<>();

    @Nullable
    private OnItemClickListener itemClickListener;
    @Nullable
    private OnFavoriteClickListener favoriteClickListener;

    public RecipeAdapter(@NonNull android.content.Context context, @NonNull List<Recipe> initialRecipes) {
        // Context is not stored directly to avoid leaks, ViewBinding handles layout inflation context
        this.differ = new AsyncListDiffer<>(this, DIFF_CALLBACK);
        updateData(initialRecipes);
    }

    private static final DiffUtil.ItemCallback<Recipe> DIFF_CALLBACK = new DiffUtil.ItemCallback<Recipe>() {
        @Override
        public boolean areItemsTheSame(@NonNull Recipe oldItem, @NonNull Recipe newItem) {
            // Safe check for null IDs
            String oldId = oldItem.getId();
            String newId = newItem.getId();
            return oldId != null && oldId.equals(newId);
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
        differ.submitList(newRecipes == null ? new ArrayList<>() : new ArrayList<>(newRecipes));
    }

    public void updateFavorites(@Nullable List<String> newFavoriteIds) {
        Set<String> newSet = newFavoriteIds == null ? new HashSet<>() : new HashSet<>(newFavoriteIds);
        if (newSet.equals(favoriteIds)) return;

        Set<String> oldSet = favoriteIds;
        favoriteIds = newSet;

        List<Recipe> current = differ.getCurrentList();
        for (int i = 0; i < current.size(); i++) {
            String id = current.get(i).getId();
            if (id == null) continue;

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
        // Using ViewBinding (ItemRecipeBinding generated from item_recipe.xml)
        ItemRecipeBinding binding = ItemRecipeBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new RecipeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = differ.getCurrentList().get(position);
        holder.bind(recipe);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.contains(PAYLOAD_FAVORITE)) {
            holder.bindFavoriteState(differ.getCurrentList().get(position));
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    class RecipeViewHolder extends RecyclerView.ViewHolder {

        private final ItemRecipeBinding binding;

        RecipeViewHolder(@NonNull ItemRecipeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && itemClickListener != null) {
                    itemClickListener.onItemClick(differ.getCurrentList().get(pos));
                }
            });

            binding.btnFavorite.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && favoriteClickListener != null) {
                    favoriteClickListener.onFavoriteClick(differ.getCurrentList().get(pos));
                }
            });
        }

        void bind(Recipe recipe) {
            binding.tvItemTitle.setText(recipe.getTitle());
            binding.tvItemCategory.setText(recipe.getCategory());

            // Direct Glide usage - replacing ImageUtils
            Glide.with(itemView)
                    .load(recipe.getImageUrl())
                    .placeholder(R.drawable.ic_food_placeholder)
                    .error(R.drawable.ic_food_placeholder)
                    .centerCrop()
                    .into(binding.ivItemImage);

            bindFavoriteState(recipe);
        }

        void bindFavoriteState(Recipe recipe) {
            boolean isFavorite = recipe.getId() != null && favoriteIds.contains(recipe.getId());

            binding.btnFavorite.setImageResource(
                    isFavorite ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
            );

            int colorRes = isFavorite ? R.color.primary : android.R.color.darker_gray;
            binding.btnFavorite.setColorFilter(
                    ContextCompat.getColor(itemView.getContext(), colorRes)
            );
        }
    }
}