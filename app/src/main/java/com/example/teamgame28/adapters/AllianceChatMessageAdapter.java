package com.example.teamgame28.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teamgame28.R;
import com.example.teamgame28.model.AllianceMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllianceChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_MINE = 1;
    private static final int VIEW_TYPE_THEIRS = 2;

    private List<AllianceMessage> messages = new ArrayList<>();
    private String currentUserId;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public AllianceChatMessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<AllianceMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void addMessage(AllianceMessage message) {
        this.messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        AllianceMessage message = messages.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_MINE;
        } else {
            return VIEW_TYPE_THEIRS;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_MINE) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message_mine, parent, false);
            return new MyMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message_theirs, parent, false);
            return new TheirMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AllianceMessage message = messages.get(position);

        if (holder instanceof MyMessageViewHolder) {
            ((MyMessageViewHolder) holder).bind(message);
        } else if (holder instanceof TheirMessageViewHolder) {
            ((TheirMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ViewHolder za moje poruke
    class MyMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvSenderName;
        TextView tvMessageText;
        TextView tvTimestamp;

        MyMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tv_sender_name);
            tvMessageText = itemView.findViewById(R.id.tv_message_text);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
        }

        void bind(AllianceMessage message) {
            tvSenderName.setText("Ti");
            tvMessageText.setText(message.getText());
            tvTimestamp.setText(timeFormat.format(new Date(message.getTimestamp())));
        }
    }

    // ViewHolder za tuđe poruke
    class TheirMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvSenderName;
        TextView tvMessageText;
        TextView tvTimestamp;

        TheirMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tv_sender_name);
            tvMessageText = itemView.findViewById(R.id.tv_message_text);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
        }

        void bind(AllianceMessage message) {
            tvSenderName.setText(message.getSenderUsername() != null ? message.getSenderUsername() : "Član");
            tvMessageText.setText(message.getText());
            tvTimestamp.setText(timeFormat.format(new Date(message.getTimestamp())));
        }
    }
}
