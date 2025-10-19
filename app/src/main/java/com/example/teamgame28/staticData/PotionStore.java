package com.example.teamgame28.staticData;

import com.example.teamgame28.R;
import com.example.teamgame28.model.Potion;
import com.example.teamgame28.model.PotionType;

import java.util.ArrayList;
import java.util.List;

public class PotionStore {

    /**
     * Dobija sve dostupne napitke sa placeholder cenom (0).
     * Cene se računaju dinamički u EquipmentService na osnovu user levela.
     *
     * Cena se određuje kao: baseReward * priceMultiplier
     * gde je baseReward nagrada od prethodnog bossa.
     */
    public static List<Potion> getPotions() {
        List<Potion> potions = new ArrayList<>();

        // Potion +20% PP = 50% nagrade od prethodnog bossa
        potions.add(new Potion("potion_20", "Potion +20% PP", 0,
                PotionType.ONETIME, 0.2, R.drawable.potion20));

        // Potion +40% PP = 70% nagrade od prethodnog bossa
        potions.add(new Potion("potion_40", "Potion +40% PP", 0,
                PotionType.ONETIME, 0.4, R.drawable.potion40));

        // Permanent +5% PP = 200% nagrade od prethodnog bossa
        potions.add(new Potion("potion_perm5", "Potion Permanent +5% PP", 0,
                PotionType.PERMANENT, 0.05, R.drawable.potion_perm5));

        // Permanent +10% PP = 1000% nagrade od prethodnog bossa
        potions.add(new Potion("potion_perm10", "Potion Permanent +10% PP", 0,
                PotionType.PERMANENT, 0.10, R.drawable.potion_perm10));

        return potions;
    }

    /**
     * Dobija price multiplier za određeni napitak (postotak nagrade od prethodnog bossa).
     */
    public static double getPriceMultiplier(String potionId) {
        switch (potionId) {
            case "potion_20": return 0.5;   // 50%
            case "potion_40": return 0.7;   // 70%
            case "potion_perm5": return 2.0;  // 200%
            case "potion_perm10": return 10.0; // 1000%
            default: return 0.5;
        }
    }
}
