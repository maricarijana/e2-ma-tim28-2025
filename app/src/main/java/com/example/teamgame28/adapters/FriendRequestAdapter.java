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
import com.example.teamgame28.model.FriendRequest;
import com.example.teamgame28.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter za prikaz pending friend requests.
 */
public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.RequestViewHolder> {

    private List<FriendRequest> requests;
    private Map<String, User> usersById; // Mapa userId -> User za fromUserId
    private final OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onAcceptClick(FriendRequest request);
    }

    public FriendRequestAdapter(OnRequestActionListener listener) {
        this.requests = new ArrayList<>();
        this.usersById = new HashMap<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        FriendRequest request = requests.get(position);
        User fromUser = usersById.get(request.getFromUserId());
        holder.bind(request, fromUser, listener);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    /**
     * Postavi listu requests i mapu korisnika.
     */
    public void setData(List<FriendRequest> requests, Map<String, User> usersById) {
        this.requests = requests != null ? requests : new ArrayList<>();
        this.usersById = usersById != null ? usersById : new HashMap<>();
        notifyDataSetChanged();
    }

    /**
     * ViewHolder za jedan friend request.
     */
    static class RequestViewHolder extends RecyclerView.ViewHolder {

        private final ImageView avatarImageView;
        private final TextView usernameTextView;
        private final TextView timestampTextView;
        private final Button acceptButton;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.request_avatar);
            usernameTextView = itemView.findViewById(R.id.request_username);
            timestampTextView = itemView.findViewById(R.id.request_timestamp);
            acceptButton = itemView.findViewById(R.id.btn_accept_request);
        }

        public void bind(FriendRequest request, User fromUser, OnRequestActionListener listener) {
            if (fromUser != null) {
                // Avatar
                String avatarName = fromUser.getAvatar();
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
                usernameTextView.setText(fromUser.getUsername());
            } else {
                // Fallback ako User nije učitan
                avatarImageView.setImageResource(R.drawable.ic_launcher_foreground);
                usernameTextView.setText("Nepoznat korisnik");
            }

            // Timestamp (relativno vreme)
            long timeDiff = System.currentTimeMillis() - request.getTimestamp();
            String timeAgo = getTimeAgo(timeDiff);
            timestampTextView.setText(timeAgo);

            // Dugme za prihvatanje
            acceptButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAcceptClick(request);
                }
            });
        }

        private String getTimeAgo(long milliseconds) {
            long seconds = milliseconds / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (days > 0) {
                return "Pre " + days + " d";
            } else if (hours > 0) {
                return "Pre " + hours + " h";
            } else if (minutes > 0) {
                return "Pre " + minutes + " min";
            } else {
                return "Upravo sada";
            }
        }
    }
}
