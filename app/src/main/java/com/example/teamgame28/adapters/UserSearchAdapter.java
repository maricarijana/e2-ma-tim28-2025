package com.example.teamgame28.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teamgame28.R;
import com.example.teamgame28.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter za prikaz korisnika u RecyclerView-u (pretraga korisnika ili lista prijatelja).
 */
public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    public enum Mode {
        FRIEND_LIST,      // Prikazuje prijatelje sa dugmetom "Dodaj u savez"
        SEARCH_RESULTS    // Prikazuje search rezultate sa "Dodaj za prijatelja" ili "Već ste prijatelji"
    }

    private List<User> userList;
    private final OnUserClickListener listener;
    private Mode mode;
    private List<String> friendIds; // Lista userId-jeva prijatelja (za proveru u search režimu)

    public interface OnUserClickListener {
        void onViewProfileClick(User user);
        void onAddFriendClick(User user);
        void onInviteToAllianceClick(User user);
    }

    public UserSearchAdapter(OnUserClickListener listener, Mode mode) {
        this.userList = new ArrayList<>();
        this.listener = listener;
        this.mode = mode;
        this.friendIds = new ArrayList<>();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        boolean isAlreadyFriend = friendIds.contains(user.getUid());
        holder.bind(user, listener, mode, isAlreadyFriend);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    /**
     * Ažurira listu korisnika.
     */
    public void setUserList(List<User> newList) {
        this.userList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Postavi režim adaptera (FRIEND_LIST ili SEARCH_RESULTS).
     */
    public void setMode(Mode mode) {
        this.mode = mode;
        notifyDataSetChanged();
    }

    /**
     * Postavi listu ID-jeva prijatelja (za proveru u search režimu).
     */
    public void setFriendIds(List<String> friendIds) {
        this.friendIds = friendIds != null ? friendIds : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * ViewHolder za jedan item korisnika.
     */
    static class UserViewHolder extends RecyclerView.ViewHolder {

        private final ImageView avatarImageView;
        private final TextView usernameTextView;
        private final TextView uidTextView;
        private final Button viewProfileButton;
        private final Button addFriendButton;
        private final Button inviteAllianceButton;
        private final TextView alreadyFriendsText;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.user_avatar);
            usernameTextView = itemView.findViewById(R.id.user_username);
            uidTextView = itemView.findViewById(R.id.user_uid);
            viewProfileButton = itemView.findViewById(R.id.btn_view_profile);
            addFriendButton = itemView.findViewById(R.id.btn_add_friend);
            inviteAllianceButton = itemView.findViewById(R.id.btn_invite_alliance);
            alreadyFriendsText = itemView.findViewById(R.id.text_already_friends);
        }

        public void bind(User user, OnUserClickListener listener, Mode mode, boolean isAlreadyFriend) {
            // Avatar
            String avatarName = user.getAvatar();
            if (avatarName != null && !avatarName.isEmpty()) {
                String resourceName = avatarName.toLowerCase();
                int resId = itemView.getContext().getResources()
                        .getIdentifier(resourceName, "drawable", itemView.getContext().getPackageName());
                if (resId != 0) {
                    avatarImageView.setImageResource(resId);
                } else {
                    avatarImageView.setImageResource(R.drawable.ic_launcher_foreground); // placeholder
                }
            } else {
                avatarImageView.setImageResource(R.drawable.ic_launcher_foreground); // placeholder
            }

            // Username
            usernameTextView.setText(user.getUsername());

            // UID (sakriven, ali može se koristiti za debug)
            uidTextView.setText(user.getUid());

            // Dugme za pregled profila (uvek vidljivo)
            viewProfileButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewProfileClick(user);
                }
            });

            // Prikaži odgovarajuća dugmad/tekst na osnovu režima
            if (mode == Mode.FRIEND_LIST) {
                // Režim liste prijatelja: samo dugme "Profil", bez dodatnih akcija
                addFriendButton.setVisibility(View.GONE);
                alreadyFriendsText.setVisibility(View.GONE);
                inviteAllianceButton.setVisibility(View.GONE);

            } else if (mode == Mode.SEARCH_RESULTS) {
                // Režim pretrage: prikaži "Dodaj" ili "Već ste prijatelji"
                inviteAllianceButton.setVisibility(View.GONE);

                if (isAlreadyFriend) {
                    // Već prijatelj
                    addFriendButton.setVisibility(View.GONE);
                    alreadyFriendsText.setVisibility(View.VISIBLE);
                } else {
                    // Nije prijatelj
                    addFriendButton.setVisibility(View.VISIBLE);
                    alreadyFriendsText.setVisibility(View.GONE);

                    addFriendButton.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onAddFriendClick(user);
                        }
                    });
                }
            }
        }
    }
}
