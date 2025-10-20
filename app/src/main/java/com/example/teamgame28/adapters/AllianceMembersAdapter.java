package com.example.teamgame28.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teamgame28.R;
import com.example.teamgame28.model.User;

import java.util.ArrayList;
import java.util.List;

public class AllianceMembersAdapter extends RecyclerView.Adapter<AllianceMembersAdapter.MemberViewHolder> {

    private List<User> members;
    private String leaderId;

    public AllianceMembersAdapter() {
        this.members = new ArrayList<>();
    }

    public void setMembers(List<User> members, String leaderId) {
        this.members = members != null ? members : new ArrayList<>();
        this.leaderId = leaderId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alliance_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User member = members.get(position);
        holder.bind(member, member.getUid().equals(leaderId));
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivAvatar;
        private final TextView tvUsername;
        private final TextView tvRole;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_member_avatar);
            tvUsername = itemView.findViewById(R.id.tv_member_username);
            tvRole = itemView.findViewById(R.id.tv_member_role);
        }

        public void bind(User member, boolean isLeader) {
            // Avatar
            String avatarName = member.getAvatar();
            if (avatarName != null && !avatarName.isEmpty()) {
                String resourceName = avatarName.toLowerCase();
                int resId = itemView.getContext().getResources()
                        .getIdentifier(resourceName, "drawable", itemView.getContext().getPackageName());
                if (resId != 0) {
                    ivAvatar.setImageResource(resId);
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_launcher_foreground);
                }
            } else {
                ivAvatar.setImageResource(R.drawable.ic_launcher_foreground);
            }

            // Username
            tvUsername.setText(member.getUsername());

            // Oznaka vođe
            if (isLeader) {
                tvRole.setText("VOĐA");
                tvRole.setVisibility(View.VISIBLE);
            } else {
                tvRole.setVisibility(View.GONE);
            }
        }
    }
}
