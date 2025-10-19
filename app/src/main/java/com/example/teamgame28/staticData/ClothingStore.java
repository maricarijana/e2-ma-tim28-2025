package com.example.teamgame28.staticData;

import com.example.teamgame28.R;
import com.example.teamgame28.model.Clothing;

import java.util.ArrayList;
import java.util.List;

public class ClothingStore {

    /**
     * Dobija svu dostupnu odeću sa placeholder cenom (0).
     * Cene se računaju dinamički u EquipmentService na osnovu user levela.
     */
    public static List<Clothing> getClothes() {
        List<Clothing> clothes = new ArrayList<>();

        // Gloves +10% PP = 60% nagrade od prethodnog bossa
        Clothing gloves = new Clothing();
        gloves.setId("gloves");
        gloves.setName("Gloves +10% PP");
        gloves.setPpBoostPercent(0.1);
        gloves.setBattlesRemaining(2);
        gloves.setCost(0);
        gloves.setImageResId(R.drawable.gloves);
        clothes.add(gloves);

        // Shield +10% Success Chance = 60% nagrade od prethodnog bossa
        Clothing shield = new Clothing();
        shield.setId("shield");
        shield.setName("Shield +10% Success Chance");
        shield.setSuccessChanceBoost(0.1);
        shield.setBattlesRemaining(2);
        shield.setCost(0);
        shield.setImageResId(R.drawable.shield);
        clothes.add(shield);

        // Boots +40% Extra Attack Chance = 80% nagrade od prethodnog bossa
        Clothing boots = new Clothing();
        boots.setId("boots");
        boots.setName("Boots +40% Extra Attack Chance");
        boots.setExtraAttackChance(0.4);
        boots.setBattlesRemaining(2);
        boots.setCost(0);
        boots.setImageResId(R.drawable.boots);
        clothes.add(boots);

        return clothes;
    }

    /**
     * Dobija price multiplier za određenu odeću (postotak nagrade od prethodnog bossa).
     */
    public static double getPriceMultiplier(String clothingId) {
        switch (clothingId) {
            case "gloves": return 0.6;  // 60%
            case "shield": return 0.6;  // 60%
            case "boots": return 0.8;   // 80%
            default: return 0.6;
        }
    }
}
