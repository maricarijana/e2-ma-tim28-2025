package com.example.teamgame28.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teamgame28.R;
import com.example.teamgame28.model.AllianceMissionProgress;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter za prikaz napretka članova saveza u specijalnoj misiji.
 */
public class MemberProgressAdapter extends RecyclerView.Adapter<MemberProgressAdapter.ViewHolder> {

    private List<AllianceMissionProgress> progressList;

    public MemberProgressAdapter() {
        this.progressList = new ArrayList<>();
    }

    public void setProgressList(List<AllianceMissionProgress> progressList) {
        this.progressList = progressList != null ? progressList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_member_progress, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AllianceMissionProgress progress = progressList.get(position);
        holder.bind(progress);
    }

    @Override
    public int getItemCount() {
        return progressList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvUserName;
        private final TextView tvDamageDealt;
        private final TextView tvShopPurchases;
        private final TextView tvBossHits;
        private final TextView tvTaskPoints;
        private final TextView tvTasksCompleted;
        private final TextView tvDaysWithMessages;
        private final TextView tvNoUnfinishedTasks;
        private final ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvDamageDealt = itemView.findViewById(R.id.tv_damage_dealt);
            tvShopPurchases = itemView.findViewById(R.id.tv_shop_purchases);
            tvBossHits = itemView.findViewById(R.id.tv_boss_hits);
            tvTaskPoints = itemView.findViewById(R.id.tv_task_points);
            tvTasksCompleted = itemView.findViewById(R.id.tv_tasks_completed);
            tvDaysWithMessages = itemView.findViewById(R.id.tv_days_with_messages);
            tvNoUnfinishedTasks = itemView.findViewById(R.id.tv_no_unfinished_tasks);
            progressBar = itemView.findViewById(R.id.progress_bar_member);
        }

        public void bind(AllianceMissionProgress progress) {
            // Prikaži userId (idealno bi bilo dohvatiti username iz Firestore)
            tvUserName.setText("Član: " + progress.getUserId().substring(0, Math.min(8, progress.getUserId().length())));

            // Ukupna šteta
            tvDamageDealt.setText("💥 Ukupno HP: " + progress.getDamageDealt());

            // Detalji po kategorijama
            tvShopPurchases.setText("🛒 Kupovina: " + progress.getShopPurchases() + "/5");
            tvBossHits.setText("⚔️ Boss hitovi: " + progress.getBossHits() + "/10");
            tvTaskPoints.setText("📝 Lakši zadaci: " + progress.getTaskPoints() + "/10");
            tvTasksCompleted.setText("🔥 Teži zadaci: " + progress.getTasksCompleted() + "/6");
            tvDaysWithMessages.setText("💬 Dani sa porukama: " + progress.getDaysWithMessages() + "/14");
            tvNoUnfinishedTasks.setText(progress.isNoUnfinishedTasks() ? "✅ Bez nerešenih zadataka" : "❌ Ima nerešenih zadataka");

            // Progress bar (maksimalno 130 HP po članu)
            int maxDamage = 130;
            progressBar.setMax(maxDamage);
            progressBar.setProgress(progress.getDamageDealt());
        }
    }
}
