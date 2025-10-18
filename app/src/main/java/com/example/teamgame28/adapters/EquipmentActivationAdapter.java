package com.example.teamgame28.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.teamgame28.R;
import com.example.teamgame28.model.Clothing;
import com.example.teamgame28.model.Equipment;
import com.example.teamgame28.model.Potion;
import com.example.teamgame28.model.PotionType;
import com.example.teamgame28.model.Weapon;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter za aktivaciju/deaktivaciju opreme.
 * Prikazuje dugmiće "Activate" ili "Deactivate" u zavisnosti od stanja.
 */
public class EquipmentActivationAdapter extends RecyclerView.Adapter<EquipmentActivationAdapter.ViewHolder> {

    private List<Equipment> equipmentList;
    private final OnEquipmentActionListener listener;
    private final boolean isActiveList; // Da li prikazuje aktivnu opremu

    public interface OnEquipmentActionListener {
        void onActivateClick(Equipment equipment);
        void onDeactivateClick(Equipment equipment);
    }

    public EquipmentActivationAdapter(OnEquipmentActionListener listener, boolean isActiveList) {
        this.equipmentList = new ArrayList<>();
        this.listener = listener;
        this.isActiveList = isActiveList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipment_activation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Equipment equipment = equipmentList.get(position);
        holder.bind(equipment, listener, isActiveList);
    }

    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    public void setEquipmentList(List<Equipment> newList) {
        this.equipmentList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private final TextView nameTextView;
        private final TextView detailsTextView;
        private final TextView statusTextView;
        private final Button actionButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.equipment_image);
            nameTextView = itemView.findViewById(R.id.equipment_name);
            detailsTextView = itemView.findViewById(R.id.equipment_details);
            statusTextView = itemView.findViewById(R.id.equipment_status);
            actionButton = itemView.findViewById(R.id.btn_action);
        }

        public void bind(Equipment equipment, OnEquipmentActionListener listener, boolean isActiveList) {
            // Slika
            if (equipment.getImageResId() != 0) {
                imageView.setImageResource(equipment.getImageResId());
            } else {
                imageView.setImageResource(R.drawable.ic_launcher_foreground);
            }

            // Naziv
            nameTextView.setText(equipment.getName());

            // Detalji (bonusi i trajanje)
            String details = generateDetails(equipment);
            detailsTextView.setText(details);

            // Status (za aktivnu opremu)
            if (isActiveList) {
                statusTextView.setVisibility(View.VISIBLE);
                statusTextView.setText("✓ Aktivno");
                statusTextView.setTextColor(Color.parseColor("#4CAF50")); // zeleno

                // Prikaz trajanja za clothing
                if (equipment instanceof Clothing) {
                    Clothing clothing = (Clothing) equipment;
                    int battlesLeft = clothing.getBattlesRemaining();
                    if (battlesLeft > 0) {
                        statusTextView.setText("✓ Aktivno (" + battlesLeft + " borbi preostalo)");
                    }
                }

                // Dugme "Deactivate"
                actionButton.setText("Deaktiviraj");
                actionButton.setBackgroundColor(Color.parseColor("#F44336")); // crveno
                actionButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeactivateClick(equipment);
                    }
                });
            } else {
                // Neaktivna oprema
                statusTextView.setVisibility(View.GONE);

                // Provera: Da li je PERMANENT potion već consumed?
                if (equipment instanceof Potion) {
                    Potion potion = (Potion) equipment;
                    if (potion.getPotionType() == PotionType.PERMANENT && potion.isConsumed()) {
                        // Consumed permanent potion - sakrij dugme
                        statusTextView.setVisibility(View.VISIBLE);
                        statusTextView.setText("✓ Već iskorišćen (trajno primenjen)");
                        statusTextView.setTextColor(Color.parseColor("#FF9800")); // narandžasto
                        actionButton.setVisibility(View.GONE);
                        return;
                    }
                }

                // Dugme "Activate" za opremu koja može da se aktivira
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setText("Aktiviraj");
                actionButton.setBackgroundColor(Color.parseColor("#4CAF50")); // zeleno
                actionButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onActivateClick(equipment);
                    }
                });
            }
        }

        /**
         * Generiše detalje opreme (bonus vrednosti).
         */
        private String generateDetails(Equipment equipment) {
            StringBuilder sb = new StringBuilder();

            if (equipment instanceof Potion) {
                Potion potion = (Potion) equipment;
                sb.append("Bonus: +").append((int) (potion.getPpBoostPercent() * 100)).append("% PP");
                if (potion.getPotionType() == PotionType.ONETIME) {
                    sb.append(" (jednokratno)");
                } else {
                    sb.append(" (trajno)");
                }
            } else if (equipment instanceof Clothing) {
                Clothing clothing = (Clothing) equipment;
                if (clothing.getPpBoostPercent() > 0) {
                    sb.append("Bonus: +").append((int) (clothing.getPpBoostPercent() * 100)).append("% PP");
                }
                if (clothing.getSuccessChanceBoost() > 0) {
                    sb.append(" | +").append((int) (clothing.getSuccessChanceBoost() * 100)).append("% Success");
                }
                if (clothing.getExtraAttackChance() > 0) {
                    sb.append(" | +").append((int) (clothing.getExtraAttackChance() * 100)).append("% Extra Attack");
                }
            } else if (equipment instanceof Weapon) {
                Weapon weapon = (Weapon) equipment;
                if (weapon.getPpBoostPercent() > 0) {
                    sb.append("Bonus: +").append((int) (weapon.getPpBoostPercent() * 100)).append("% PP");
                }
                if (weapon.getCoinBoostPercent() > 0) {
                    sb.append(" | +").append((int) (weapon.getCoinBoostPercent() * 100)).append("% Coins");
                }
                if (weapon.getUpgradeLevel() > 0) {
                    sb.append(" | Upgrade Level: ").append(weapon.getUpgradeLevel());
                }
            }

            return sb.toString();
        }
    }
}
