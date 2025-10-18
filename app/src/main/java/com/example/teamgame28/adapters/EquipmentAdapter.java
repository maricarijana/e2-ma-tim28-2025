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
import com.example.teamgame28.model.Equipment;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter za prikaz opreme u RecyclerView-u.
 * Prikazuje naziv, sliku, bonuse i cenu opreme.
 */
public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.EquipmentViewHolder> {

    private List<Equipment> equipmentList;
    private final OnEquipmentClickListener listener;
    private final boolean showBuyButton; // Da li prikazati "Buy" dugme

    public interface OnEquipmentClickListener {
        void onBuyClick(Equipment equipment);
        void onItemClick(Equipment equipment);
    }

    public EquipmentAdapter(OnEquipmentClickListener listener, boolean showBuyButton) {
        this.equipmentList = new ArrayList<>();
        this.listener = listener;
        this.showBuyButton = showBuyButton;
    }

    @NonNull
    @Override
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipment, parent, false);
        return new EquipmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipmentViewHolder holder, int position) {
        Equipment equipment = equipmentList.get(position);
        holder.bind(equipment, listener, showBuyButton);
    }

    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    /**
     * Ažurira listu opreme.
     */
    public void setEquipmentList(List<Equipment> newList) {
        this.equipmentList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * ViewHolder za jedan item opreme.
     */
    static class EquipmentViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private final TextView nameTextView;
        private final TextView descriptionTextView;
        private final TextView priceTextView;
        private final Button buyButton;

        public EquipmentViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.equipment_image);
            nameTextView = itemView.findViewById(R.id.equipment_name);
            descriptionTextView = itemView.findViewById(R.id.equipment_description);
            priceTextView = itemView.findViewById(R.id.equipment_price);
            buyButton = itemView.findViewById(R.id.btn_buy_equipment);
        }

        public void bind(Equipment equipment, OnEquipmentClickListener listener, boolean showBuyButton) {
            // Slika
            if (equipment.getImageResId() != 0) {
                imageView.setImageResource(equipment.getImageResId());
            } else {
                imageView.setImageResource(R.drawable.ic_launcher_foreground); // placeholder
            }

            // Naziv
            nameTextView.setText(equipment.getName());

            // Opis (bonusi)
            String description = generateDescription(equipment);
            descriptionTextView.setText(description);

            // Cena
            if (equipment.getCost() > 0) {
                priceTextView.setText(equipment.getCost() + " coins");
                priceTextView.setVisibility(View.VISIBLE);
            } else {
                priceTextView.setVisibility(View.GONE);
            }

            // Dugme "Buy"
            if (showBuyButton) {
                buyButton.setVisibility(View.VISIBLE);
                buyButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onBuyClick(equipment);
                    }
                });
            } else {
                buyButton.setVisibility(View.GONE);
            }

            // Klik na ceo item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(equipment);
                }
            });
        }

        /**
         * Generiše opis bonusa na osnovu tipa opreme.
         */
        private String generateDescription(Equipment equipment) {
            // Ovde možeš dodati detaljnije opise za svaki tip
            // Za sada vraćamo samo ime kao opis
            return equipment.getName();
        }
    }
}
