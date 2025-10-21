package com.example.teamgame28.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teamgame28.R;
import com.example.teamgame28.model.Alliance;
import com.example.teamgame28.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter za prikaz alliance invites.
 */
public class AllianceInviteAdapter extends RecyclerView.Adapter<AllianceInviteAdapter.InviteViewHolder> {

    private List<Alliance> alliances;
    private Map<String, User> leadersById; // leaderId -> User
    private final OnInviteActionListener listener;

    public interface OnInviteActionListener {
        void onAcceptClick(Alliance alliance);
        void onDeclineClick(Alliance alliance);
    }

    public AllianceInviteAdapter(OnInviteActionListener listener) {
        this.alliances = new ArrayList<>();
        this.leadersById = new HashMap<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public InviteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alliance_invite, parent, false);
        return new InviteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InviteViewHolder holder, int position) {
        Alliance alliance = alliances.get(position);
        User leader = leadersById.get(alliance.getLeaderId());
        holder.bind(alliance, leader, listener);
    }

    @Override
    public int getItemCount() {
        return alliances.size();
    }

    public void setData(List<Alliance> alliances, Map<String, User> leadersById) {
        this.alliances = alliances != null ? alliances : new ArrayList<>();
        this.leadersById = leadersById != null ? leadersById : new HashMap<>();
        notifyDataSetChanged();
    }

    static class InviteViewHolder extends RecyclerView.ViewHolder {

        private final TextView allianceNameTextView;
        private final TextView leaderNameTextView;
        private final Button acceptButton;
        private final Button declineButton;

        public InviteViewHolder(@NonNull View itemView) {
            super(itemView);
            allianceNameTextView = itemView.findViewById(R.id.alliance_name);
            leaderNameTextView = itemView.findViewById(R.id.leader_name);
            acceptButton = itemView.findViewById(R.id.btn_accept);
            declineButton = itemView.findViewById(R.id.btn_decline);
        }

        public void bind(Alliance alliance, User leader, OnInviteActionListener listener) {
            // Naziv saveza
            allianceNameTextView.setText(alliance.getName());

            // Ime vođe
            String leaderUsername = leader != null ? leader.getUsername() : "Nepoznat vođa";
            leaderNameTextView.setText("Vođa: " + leaderUsername);

            // Prihvati
            acceptButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAcceptClick(alliance);
                }
            });

            // Odbij
            declineButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeclineClick(alliance);
                }
            });
        }
    }
}
