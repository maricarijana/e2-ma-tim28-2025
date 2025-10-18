package com.example.teamgame28.service;

import android.util.Log;

import com.example.teamgame28.model.Clothing;
import com.example.teamgame28.model.Equipment;
import com.example.teamgame28.model.EquipmentBoosts;
import com.example.teamgame28.model.EquipmentType;
import com.example.teamgame28.model.Potion;
import com.example.teamgame28.model.PotionType;
import com.example.teamgame28.model.UserProfile;
import com.example.teamgame28.model.Weapon;
import com.example.teamgame28.repository.EquipmentRepository;
import com.example.teamgame28.staticData.ClothingStore;
import com.example.teamgame28.staticData.PotionStore;
import com.example.teamgame28.staticData.WeaponStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Service za upravljanje opremom korisnika.
 * Sadrži business logiku za kupovinu, aktivaciju, upgrade i potrošnju opreme.
 */
public class EquipmentService {

    private static final String TAG = "EquipmentService";
    private final EquipmentRepository equipmentRepository;

    public EquipmentService() {
        this.equipmentRepository = EquipmentRepository.getInstance();
    }

    // ========== KALKULACIJA CENA ==========

    /**
     * Računa cenu opreme na osnovu prethodnog levela.
     * Cena se određuje kao procenat od potencijalne nagrade bosa.
     *
     * @param baseReward nagrada od prethodnog bosa
     * @param percentOfReward procenat (npr. 0.5 za 50%, 2.0 za 200%)
     * @return izračunata cena
     */
    public int calculatePrice(int baseReward, double percentOfReward) {
        return (int) (baseReward * percentOfReward);
    }

    /**
     * Računa cenu upgrade-a oružja (60% od potencijalne nagrade).
     */
    public int calculateUpgradePrice(int baseReward) {
        return calculatePrice(baseReward, 0.6);
    }

    // ========== KUPOVINA OPREME ==========

    /**
     * Validira i izvršava kupovinu opreme.
     *
     * @param userId ID korisnika
     * @param equipment oprema za kupovinu
     * @param userCoins trenutni broj coina korisnika
     * @param callback rezultat operacije
     */
    public void buyEquipment(String userId, Equipment equipment, int userCoins,
                            BuyEquipmentCallback callback) {

        // Validacija: proveri da li korisnik ima dovoljno novčića
        if (userCoins < equipment.getCost()) {
            callback.onFailure("Nemate dovoljno novčića! Potrebno: " +
                              equipment.getCost() + ",imate: " + userCoins);
            return;
        }

        int newBalance = userCoins - equipment.getCost();

        // Dodaj opremu u posedovanu listu i ažuriraj balance
        equipmentRepository.buyEquipment(userId, equipment, newBalance);

        callback.onSuccess("Uspešno kupljena oprema: " + equipment.getName());
        Log.d(TAG, "✅ Kupljena oprema: " + equipment.getName() +
              " za " + equipment.getCost() + " coina");
    }

    // ========== AKTIVACIJA OPREME ==========

    /**
     * Aktivira opremu pre borbe i primenjuje njene efekte.
     * Poštuje pravila:
     * - Samo 1 ONETIME potion može biti aktivan
     * - PERMANENT potions se odmah primenjuju i uklon se iz inventara
     * - Više armor itema može biti aktivno (clothing se sabira)
     * - Weapons se automatski primenjuju
     *
     * @param userId ID korisnika
     * @param equipment oprema za aktivaciju
     * @param currentActiveEquipment trenutno aktivna oprema
     * @param callback rezultat operacije
     */
    public void activateEquipment(String userId, Equipment equipment,
                                  List<Equipment> currentActiveEquipment,
                                  ActivateEquipmentCallback callback) {

        // ========== NAPICI (POTIONS) ==========
        if (equipment instanceof Potion) {
            Potion potion = (Potion) equipment;

            // Provera: da li je PERMANENT napitak već iskorišćen
            if (potion.getPotionType() == PotionType.PERMANENT && potion.isConsumed()) {
                callback.onFailure("Ovaj trajni napitak je već iskorišćen!");
                return;
            }

            // Provera: da li već postoji aktivan ONETIME potion
            if (potion.getPotionType() == PotionType.ONETIME) {
                for (Equipment eq : currentActiveEquipment) {
                    if (eq instanceof Potion) {
                        Potion activePotion = (Potion) eq;
                        if (activePotion.getPotionType() == PotionType.ONETIME && activePotion.isActive()) {
                            callback.onFailure("Već imate aktivan jednokratni napitak!");
                            return;
                        }
                    }
                }

                // Aktiviraj ONETIME napitak (ide u activeEquipment)
                potion.setActive(true);
                equipmentRepository.activateEquipment(userId, potion);
                callback.onSuccess("Jednokratni napitak aktiviran: " + potion.getName());
                Log.d(TAG, "✅ ONETIME potion aktiviran: " + potion.getName());
            }
            else if (potion.getPotionType() == PotionType.PERMANENT) {
                // PERMANENT napitak: povećaj bazni PP odmah i označi kao consumed
                equipmentRepository.applyPermanentPotionBoost(userId, potion.getPpBoostPercent(), potion.getId());
                callback.onSuccess("Trajni napitak primenjen! Bazni PP trajno povećan.");
                Log.d(TAG, "✅ PERMANENT potion primenjen: +" + (potion.getPpBoostPercent() * 100) + "% PP");
            }
            return;
        }

        // ========== ODEĆA (CLOTHING) ==========
        // Clothing može imati više instanci aktivnih odjednom.
        // Svaka instanca ima svoj nezavisan counter za borbe.
        // Bonusi se automatski sabiraju u calculateEquipmentBoosts() kada prolazi kroz SVE aktivne instance.
        // Ne treba posebna logika ovde - samo dodaj kao novu instancu.

        // ========== AKTIVACIJA OPREME (Clothing, Weapons) ==========
        equipment.setActive(true);
        equipmentRepository.activateEquipment(userId, equipment);

        callback.onSuccess("Oprema aktivirana: " + equipment.getName());
        Log.d(TAG, "✅ Oprema aktivirana: " + equipment.getName());
    }

    /**
     * Deaktivira opremu (uklanja iz aktivne liste).
     */
    public void deactivateEquipment(String userId, String equipmentId) {
        equipmentRepository.deactivateEquipment(userId, equipmentId);
        Log.d(TAG, "✅ Oprema deaktivirana: " + equipmentId);
    }

    // ========== UPGRADE ORUŽJA ==========

    /**
     * Unapređuje oružje i povećava verovatnoću za 0.01%.
     *
     * @param userId ID korisnika
     * @param weapon oružje za upgrade
     * @param userCoins trenutni broj coina korisnika
     * @param upgradeCost cena upgrade-a
     * @param callback rezultat operacije
     */
    public void upgradeWeapon(String userId, Weapon weapon, int userCoins,
                             int upgradeCost, UpgradeWeaponCallback callback) {

        // Validacija: proveri da li korisnik ima dovoljno novčića
        if (userCoins < upgradeCost) {
            callback.onFailure("Nemate dovoljno novčića za upgrade!");
            return;
        }

        int newBalance = userCoins - upgradeCost;
        equipmentRepository.upgradeWeapon(userId, weapon, newBalance);

        callback.onSuccess("Oružje unapređeno! Nova verovatnoća: " +
                          (weapon.getProbability() * 100) + "%");
        Log.d(TAG, "✅ Weapon upgraded: " + weapon.getName() +
              " | Level: " + weapon.getUpgradeLevel());
    }

    /**
     * Dodaje duplikat oružja (povećava verovatnoću za 0.02%).
     */
    public void handleDuplicateWeapon(String userId, String weaponId) {
        equipmentRepository.addDuplicateWeapon(userId, weaponId);
        Log.d(TAG, "✅ Duplikat weapon dodat: " + weaponId);
    }

    // ========== POTROŠNJA I TRAJANJE ==========

    /**
     * Potroši jednokratne napitke nakon borbe.
     * Trajni napitci ostaju aktivni.
     */
    public void consumeOnTimePotions(String userId, List<Equipment> activeEquipment) {
        for (Equipment eq : activeEquipment) {
            if (eq instanceof Potion) {
                Potion potion = (Potion) eq;
                if (potion.getPotionType() == PotionType.ONETIME && potion.isActive()) {
                    equipmentRepository.consumePotion(userId, potion.getId());
                    Log.d(TAG, "✅ Jednokratni potion potrošen: " + potion.getName());
                }
            }
        }
    }

    /**
     * Smanji trajanje clothing-a za 1 borbu.
     * Uklanja clothing koji je istekao.
     */
    public void decreaseClothingDuration(String userId) {
        equipmentRepository.decreaseClothingDuration(userId);
        Log.d(TAG, "✅ Clothing trajanje smanjeno");
    }

    /**
     * Obrada nakon borbe: potroši napitke i smanji trajanje odeće.
     */
    public void processPostBattle(String userId, List<Equipment> activeEquipment) {
        consumeOnTimePotions(userId, activeEquipment);
        decreaseClothingDuration(userId);
    }

    // ========== PRIMENA EFEKATA ==========

    /**
     * Računa privremene boostove od aktivne opreme.
     * Ovo se poziva PRE borbe sa bosom.
     *
     * VAŽNO: Ova metoda NE menja bazni PP u UserProfile!
     * Boostovi su PRIVREMENI i važe samo za tu borbu.
     *
     * @param basePP bazni Power Points korisnika (iz UserProfile)
     * @param activeEquipment aktivna oprema
     * @return EquipmentBoosts objekat sa privremenim boostovima
     */
    public EquipmentBoosts calculateEquipmentBoosts(int basePP, List<Equipment> activeEquipment) {
        EquipmentBoosts boosts = new EquipmentBoosts(basePP);

        int ppBoost = 0;
        double successBoost = 0.0;
        double extraAttackChance = 0.0;
        double coinBoost = 0.0;

        for (Equipment eq : activeEquipment) {
            if (!eq.isActive()) continue;

            if (eq instanceof Potion) {
                Potion potion = (Potion) eq;
                // SAMO ONETIME napici daju privremeni boost
                // PERMANENT napici su već uračunati u bazni PP
                if (potion.getPotionType() == PotionType.ONETIME) {
                    ppBoost += (int) (basePP * potion.getPpBoostPercent());
                    Log.d(TAG, "ONETIME Potion applied: +" + (potion.getPpBoostPercent() * 100) + "% PP");
                }
            }
            else if (eq instanceof Clothing) {
                Clothing clothing = (Clothing) eq;
                ppBoost += (int) (basePP * clothing.getPpBoostPercent());
                successBoost += clothing.getSuccessChanceBoost();
                extraAttackChance += clothing.getExtraAttackChance();
                Log.d(TAG, "Clothing applied: " + clothing.getName());
            }
            else if (eq instanceof Weapon) {
                Weapon weapon = (Weapon) eq;
                ppBoost += (int) (basePP * weapon.getPpBoostPercent());
                coinBoost += weapon.getCoinBoostPercent();
                Log.d(TAG, "Weapon applied: " + weapon.getName());
            }
        }

        boosts.setPpBoost(ppBoost);
        boosts.setSuccessBoost(successBoost);
        boosts.setExtraAttackChance(extraAttackChance);
        boosts.setCoinBoost(coinBoost);

        Log.d(TAG, "✅ Equipment boosts calculated: " + boosts.toString());
        return boosts;
    }

    // ========== RAČUNANJE CENA NA OSNOVU LEVELA ==========

    /**
     * Računa nagradu bossa za određeni nivo.
     * Formula: Prvi boss (level 1) = 200 coina, svaki sledeći = prethodni * 1.2
     *
     * @param level nivo bossa
     * @return nagrada u coinima
     */
    private int calculateBossRewardForLevel(int level) {
        if (level <= 0) return 200;
        if (level == 1) return 200;

        int reward = 200;
        for (int i = 2; i <= level; i++) {
            reward = (int) (reward * 1.2);
        }
        return reward;
    }

    /**
     * Setuje dinamičke cene za svu opremu na osnovu trenutnog levela korisnika.
     * Cene se računaju kao procenat od nagrade prethodnog bossa.
     *
     * @param userLevel trenutni nivo korisnika
     */
    public void setPricesBasedOnUserLevel(List<Equipment> equipmentList, int userLevel) {
        // Nagrada od bossa na PRETHODNOM nivou
        int previousLevel = Math.max(1, userLevel); // Ako je user level 0 ili 1, koristimo level 1
        int baseReward = calculateBossRewardForLevel(previousLevel);

        for (Equipment eq : equipmentList) {
            double priceMultiplier = 0.5; // default

            if (eq instanceof Potion) {
                priceMultiplier = PotionStore.getPriceMultiplier(eq.getId());
            } else if (eq instanceof Clothing) {
                priceMultiplier = ClothingStore.getPriceMultiplier(eq.getId());
            }

            int calculatedPrice = (int) (baseReward * priceMultiplier);
            eq.setCost(calculatedPrice);
        }

        Log.d(TAG, "Cene ažurirane za user level " + userLevel +
              " (baseReward=" + baseReward + ")");
    }

    // ========== STATIC DATA GETTERS ==========

    /**
     * Dobija sve dostupne napitke iz prodavnice.
     */
    public List<Potion> getAvailablePotions() {
        return PotionStore.getPotions();
    }

    /**
     * Dobija svu dostupnu odeću iz prodavnice.
     */
    public List<Clothing> getAvailableClothes() {
        return ClothingStore.getClothes();
    }

    /**
     * Dobija sva dostupna oružja (za reference, ne za kupovinu).
     */
    public List<Weapon> getAvailableWeapons() {
        return WeaponStore.getWeapons();
    }

    // ========== CALLBACK INTERFACES ==========

    public interface BuyEquipmentCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public interface ActivateEquipmentCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public interface UpgradeWeaponCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }
}
