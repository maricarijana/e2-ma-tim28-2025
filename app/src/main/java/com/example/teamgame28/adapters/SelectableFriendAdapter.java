package com.example.teamgame28.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teamgame28.R;
import com.example.teamgame28.model.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter za izbor prijatelja sa checkboxima (multi-select).
 */
public class SelectableFriendAdapter extends RecyclerView.Adapter<SelectableFriendAdapter.FriendViewHolder> {

    private List<User> friends;
    private Set<String> selectedUserIds; // Track koji su selektovani

    public SelectableFriendAdapter() {
        this.friends = new ArrayList<>();
        this.selectedUserIds = new HashSet<>();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_selectable, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User user = friends.get(position);
        boolean isSelected = selectedUserIds.contains(user.getUid());
        holder.bind(user, isSelected, this);
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    public void setFriends(List<User> friends) {
        this.friends = friends != null ? friends : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Vrati listu selektovanih userId-jeva.
     */
    public List<String> getSelectedUserIds() {
        return new ArrayList<>(selectedUserIds);
    }

    /**
     * Toggle selekciju korisnika.
     */
    private void toggleSelection(String userId) {
        if (selectedUserIds.contains(userId)) {
            selectedUserIds.remove(userId);
        } else {
            selectedUserIds.add(userId);
        }
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {

        private final CheckBox checkbox;
        private final ImageView avatarImageView;
        private final TextView usernameTextView;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.checkbox_select);
            avatarImageView = itemView.findViewById(R.id.friend_avatar);
            usernameTextView = itemView.findViewById(R.id.friend_username);
        }

        public void bind(User user, boolean isSelected, SelectableFriendAdapter adapter) {
            // Avatar
            String avatarName = user.getAvatar();
            if (avatarName != null && !avatarName.isEmpty()) {
                String resourceName = avatarName.toLowerCase();
                int resId = itemView.getContext().getResources()
                        .getIdentifier(resourceName, "drawable", itemView.getContext().getPackageName());
                if (resId != 0) {
                    avatarImageView.setImageResource(resId);
                } else {
                    avatarImageView.setImageResource(R.drawable.ic_launcher_foreground);
                }
            } else {
                avatarImageView.setImageResource(R.drawable.ic_launcher_foreground);
            }

            // Username
            usernameTextView.setText(user.getUsername());

            // Checkbox
            checkbox.setChecked(isSelected);
            checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                adapter.toggleSelection(user.getUid());
            });

            // Klik na ceo item takođe toggle-uje checkbox
            itemView.setOnClickListener(v -> checkbox.toggle());
        }
    }
}
