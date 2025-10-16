package com.example.teamgame28.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teamgame28.R;
import com.example.teamgame28.model.TaskCategory;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<TaskCategory> categories = new ArrayList<>();
    private OnCategoryActionListener listener;

    public interface OnCategoryActionListener {
        void onEditCategory(TaskCategory category);
        void onDeleteCategory(TaskCategory category);
    }

    public CategoryAdapter(OnCategoryActionListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<TaskCategory> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        TaskCategory category = categories.get(position);
        holder.bind(category, listener);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final View colorView;
        private final TextView nameText;
        private final ImageButton editButton;
        private final ImageButton deleteButton;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.viewCategoryColor);
            nameText = itemView.findViewById(R.id.textCategoryName);
            editButton = itemView.findViewById(R.id.btnEditCategory);
            deleteButton = itemView.findViewById(R.id.btnDeleteCategory);
        }

        public void bind(TaskCategory category, OnCategoryActionListener listener) {
            nameText.setText(category.getName());

            // 🎨 Postavi boju
            try {
                colorView.setBackgroundColor(Color.parseColor(category.getColor()));
            } catch (Exception e) {
                colorView.setBackgroundColor(Color.GRAY);
            }

            // 🔹 Edit akcija
            editButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditCategory(category);
                }
            });

            // 🔹 Delete akcija
            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteCategory(category);
                }
            });
        }
    }
}
