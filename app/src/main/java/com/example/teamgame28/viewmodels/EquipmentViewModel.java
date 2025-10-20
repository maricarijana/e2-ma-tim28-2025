package com.example.teamgame28.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.teamgame28.model.Clothing;
import com.example.teamgame28.model.Equipment;
import com.example.teamgame28.model.EquipmentBoosts;
import com.example.teamgame28.model.Potion;
import com.example.teamgame28.model.UserProfile;
import com.example.teamgame28.model.Weapon;
import com.example.teamgame28.repository.EquipmentRepository;
import com.example.teamgame28.service.EquipmentService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel za upravljanje opremom korisnika.
 * Sadrži LiveData za UI i poziva EquipmentService za business logiku.
 */
public class EquipmentViewModel extends AndroidViewModel {

    private final EquipmentService equipmentService;
    private final EquipmentRepository equipmentRepository;

    // LiveData za UI stanje
    private final MutableLiveData<List<Equipment>> ownedEquipment = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Equipment>> activeEquipment = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Potion>> availablePotions = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Clothing>> availableClothes = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Weapon>> availableWeapons = new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public EquipmentViewModel(@NonNull Application application) {
        super(application);
        this.equipmentService = new EquipmentService();
        this.equipmentRepository = EquipmentRepository.getInstance();

        // Učitaj statičke podatke za prodavnicu
        loadShopItems();
    }

    // ========== GETTERS FOR LIVEDATA ==========

    public LiveData<List<Equipment>> getOwnedEquipment() {
        return ownedEquipment;
    }

    public LiveData<List<Equipment>> getActiveEquipment() {
        return activeEquipment;
    }

    public LiveData<List<Potion>> getAvailablePotions() {
        return availablePotions;
    }

    public LiveData<List<Clothing>> getAvailableClothes() {
        return availableClothes;
    }

    public LiveData<List<Weapon>> getAvailableWeapons() {
        return availableWeapons;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    // ========== LOAD DATA ==========

    /**
     * Učitava posedovanu opremu iz Firebase-a.
     */
    public void loadOwnedEquipment(String userId) {
        loading.setValue(true);
        LiveData<List<Equipment>> data = equipmentRepository.getOwnedEquipment(userId);
        data.observeForever(equipment -> {
            ownedEquipment.setValue(equipment);
            loading.setValue(false);
        });
    }

    /**
     * Učitava aktivnu opremu iz Firebase-a.
     */
    public void loadActiveEquipment(String userId) {
        loading.setValue(true);
        LiveData<List<Equipment>> data = equipmentRepository.getActiveEquipment(userId);
        data.observeForever(equipment -> {
            activeEquipment.setValue(equipment);
            loading.setValue(false);
        });
    }

    /**
     * Učitava stavke iz prodavnice (statički podaci).
     */
    private void loadShopItems() {
        availablePotions.setValue(equipmentService.getAvailablePotions());
        availableClothes.setValue(equipmentService.getAvailableClothes());
        availableWeapons.setValue(equipmentService.getAvailableWeapons());
    }

    // ========== KUPOVINA OPREME ==========

    /**
     * Kupuje opremu iz prodavnice.
     *
     * @param equipment oprema za kupovinu
     * @param userCoins trenutni broj coina korisnika
     */
    public void buyEquipment(Equipment equipment, int userCoins) {
        String userId = getCurrentUserId();
        if (userId == null) {
            error.setValue("Korisnik nije prijavljen!");
            return;
        }

        loading.setValue(true);

        equipmentService.buyEquipment(userId, equipment, userCoins,
                new EquipmentService.BuyEquipmentCallback() {
                    @Override
                    public void onSuccess(String msg) {
                        message.setValue(msg);
                        loading.setValue(false);
                        loadOwnedEquipment(userId); // Osvježi listu
                    }

                    @Override
                    public void onFailure(String err) {
                        error.setValue(err);
                        loading.setValue(false);
                    }
                });
    }

    // ========== AKTIVACIJA OPREME ==========

    /**
     * Aktivira opremu pre borbe.
     *
     * @param equipment oprema za aktivaciju
     */
    public void activateEquipment(Equipment equipment) {
        String userId = getCurrentUserId();
        if (userId == null) {
            error.setValue("Korisnik nije prijavljen!");
            return;
        }

        loading.setValue(true);

        List<Equipment> currentActive = activeEquipment.getValue();
        if (currentActive == null) currentActive = new ArrayList<>();

        equipmentService.activateEquipment(userId, equipment, currentActive,
                new EquipmentService.ActivateEquipmentCallback() {
                    @Override
                    public void onSuccess(String msg) {
                        message.setValue(msg);
                        loading.setValue(false);
                        loadActiveEquipment(userId); // Osvježi aktivnu opremu
                    }

                    @Override
                    public void onFailure(String err) {
                        error.setValue(err);
                        loading.setValue(false);
                    }
                });
    }

    /**
     * Deaktivira opremu.
     *
     * @param equipmentId ID opreme za deaktivaciju
     */
    public void deactivateEquipment(String equipmentId) {
        String userId = getCurrentUserId();
        if (userId == null) {
            error.setValue("Korisnik nije prijavljen!");
            return;
        }

        equipmentService.deactivateEquipment(userId, equipmentId);
        message.setValue("Oprema deaktivirana");
        loadActiveEquipment(userId);
    }

    // ========== UPGRADE ORUŽJA ==========

    /**
     * Unapređuje oružje.
     *
     * @param weapon oružje za upgrade
     * @param userCoins trenutni broj coina korisnika
     * @param upgradeCost cena upgrade-a
     */
    public void upgradeWeapon(Weapon weapon, int userCoins, int upgradeCost) {
        String userId = getCurrentUserId();
        if (userId == null) {
            error.setValue("Korisnik nije prijavljen!");
            return;
        }

        loading.setValue(true);

        equipmentService.upgradeWeapon(userId, weapon, userCoins, upgradeCost,
                new EquipmentService.UpgradeWeaponCallback() {
                    @Override
                    public void onSuccess(String msg) {
                        message.setValue(msg);
                        loading.setValue(false);
                        loadOwnedEquipment(userId); // Osvježi listu
                    }

                    @Override
                    public void onFailure(String err) {
                        error.setValue(err);
                        loading.setValue(false);
                    }
                });
    }

    // ========== POST-BATTLE PROCESSING ==========

    /**
     * Obrađuje potrošnju napitaka i smanjenje trajanja odeće nakon borbe.
     */
    public void processPostBattle() {
        String userId = getCurrentUserId();
        if (userId == null) return;

        equipmentService.processPostBattle(userId, new EquipmentService.PostBattleCallback() {
            @Override
            public void onSuccess() {
                loadActiveEquipment(userId); // Osvježi aktivnu opremu
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("EquipmentViewModel", "❌ Greška pri post-battle obradi", e);
                loadActiveEquipment(userId); // Ipak osvježi opremu
            }
        });
    }

    // ========== PRIMENA EFEKATA ==========

    /**
     * Računa privremene boostove od aktivne opreme.
     * Poziva se pre borbe.
     *
     * @param basePP bazni PP korisnika
     * @return EquipmentBoosts objekat sa privremenim boostovima
     */
    public EquipmentBoosts calculateEquipmentBoosts(int basePP) {
        List<Equipment> currentActive = activeEquipment.getValue();
        if (currentActive == null || currentActive.isEmpty()) {
            return new EquipmentBoosts(basePP);
        }

        return equipmentService.calculateEquipmentBoosts(basePP, currentActive);
    }

    // ========== HELPER ==========

    /**
     * Dobija ID trenutno prijavljenog korisnika.
     */
    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * Resetuje poruku (za ponovne akcije).
     */
    public void clearMessages() {
        message.setValue(null);
        error.setValue(null);
    }
}
