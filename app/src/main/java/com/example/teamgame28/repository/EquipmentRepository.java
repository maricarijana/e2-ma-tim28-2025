package com.example.teamgame28.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.teamgame28.model.Clothing;
import com.example.teamgame28.model.Equipment;
import com.example.teamgame28.model.UserProfile;
import com.example.teamgame28.model.Weapon;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EquipmentRepository {

    private static EquipmentRepository instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String USERS_COLLECTION = "app_users";
    private static final String PROFILE_SUBCOLLECTION = "profile";

    private EquipmentRepository() {}

    public static EquipmentRepository getInstance() {
        if (instance == null) {
            instance = new EquipmentRepository();
        }
        return instance;
    }

    // ✅ Vrati ownedEquipment listu iz profila
    public LiveData<List<Equipment>> getOwnedEquipment(String userId) {
        MutableLiveData<List<Equipment>> liveData = new MutableLiveData<>();

        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) {
                        liveData.setValue(new ArrayList<>());
                        return;
                    }

                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile != null && profile.getOwnedEquipment() != null) {
                        liveData.setValue(profile.getOwnedEquipment());
                    } else {
                        liveData.setValue(new ArrayList<>());
                    }
                });

        return liveData;
    }

    // ✅ Vrati activeEquipment listu iz profila
    public LiveData<List<Equipment>> getActiveEquipment(String userId) {
        MutableLiveData<List<Equipment>> liveData = new MutableLiveData<>();

        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) {
                        liveData.setValue(new ArrayList<>());
                        return;
                    }

                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile != null && profile.getActiveEquipment() != null) {
                        liveData.setValue(profile.getActiveEquipment());
                    } else {
                        liveData.setValue(new ArrayList<>());
                    }
                });

        return liveData;
    }

    // ✅ Dodaj item u ownedEquipment i smanji coin balance
    public void buyEquipment(String userId, Equipment equipment, int newCoinBalance) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile == null) return;

                    List<Equipment> owned = profile.getOwnedEquipment();
                    if (owned == null) owned = new ArrayList<>();
                    owned.add(equipment);

                    profile.setOwnedEquipment(owned);
                    profile.setCoins(newCoinBalance);

                    db.collection(USERS_COLLECTION)
                            .document(userId)
                            .collection(PROFILE_SUBCOLLECTION)
                            .document(userId)
                            .set(profile)
                            .addOnSuccessListener(aVoid ->
                                    Log.d("EquipmentRepo", "✅ Kupljena oprema: " + equipment.getName()))
                            .addOnFailureListener(e ->
                                    Log.e("EquipmentRepo", "❌ Greška pri kupovini", e));
                });
    }

    // ✅ Aktiviraj opremu → prebaci u activeEquipment
    public void activateEquipment(String userId, Equipment equipment) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile == null) return;

                    List<Equipment> active = profile.getActiveEquipment();
                    if (active == null) active = new ArrayList<>();
                    active.add(equipment);
                    profile.setActiveEquipment(active);

                    db.collection(USERS_COLLECTION)
                            .document(userId)
                            .collection(PROFILE_SUBCOLLECTION)
                            .document(userId)
                            .set(profile)
                            .addOnSuccessListener(aVoid ->
                                    Log.d("EquipmentRepo", "✅ Aktivirana oprema: " + equipment.getName()));
                });
    }

    // ✅ Potroši ONETIME potion → ukloni ga iz activeEquipment I ownedEquipment
    public void consumePotion(String userId, String potionId) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile == null) return;

                    // Ukloni iz activeEquipment
                    List<Equipment> active = profile.getActiveEquipment();
                    if (active != null) {
                        active.removeIf(eq -> eq.getId().equals(potionId));
                        profile.setActiveEquipment(active);
                    }

                    // Ukloni iz ownedEquipment (ONETIME napitci se potpuno troše)
                    List<Equipment> owned = profile.getOwnedEquipment();
                    if (owned != null) {
                        owned.removeIf(eq -> eq.getId().equals(potionId));
                        profile.setOwnedEquipment(owned);
                    }

                    db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                            .document(userId)
                            .set(profile)
                            .addOnSuccessListener(aVoid ->
                                    Log.d("EquipmentRepo", "✅ ONETIME potion potrošen i uklonjen: " + potionId));
                });
    }

    // ✅ Unapredi weapon u ownedEquipment
    public void upgradeWeapon(String userId, Weapon weapon, int newCoinBalance) {
        weapon.upgrade();

        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile == null || profile.getOwnedEquipment() == null) return;

                    List<Equipment> owned = profile.getOwnedEquipment();
                    for (int i = 0; i < owned.size(); i++) {
                        if (owned.get(i).getId().equals(weapon.getId())) {
                            owned.set(i, weapon);
                            break;
                        }
                    }

                    profile.setOwnedEquipment(owned);
                    profile.setCoins(newCoinBalance);

                    db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                            .document(userId)
                            .set(profile)
                            .addOnSuccessListener(aVoid ->
                                    Log.d("EquipmentRepo", "✅ Weapon unapređen: " + weapon.getName()));
                });
    }

    // ✅ Dodaj duplikat weapona - povećava verovatnoću za 0.02%
    public void addDuplicateWeapon(String userId, String weaponId) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile == null || profile.getOwnedEquipment() == null) return;

                    List<Equipment> owned = profile.getOwnedEquipment();
                    for (Equipment eq : owned) {
                        if (eq.getId().equals(weaponId) && eq instanceof Weapon) {
                            ((Weapon) eq).addDuplicate();
                            break;
                        }
                    }

                    profile.setOwnedEquipment(owned);

                    db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                            .document(userId)
                            .set(profile)
                            .addOnSuccessListener(aVoid ->
                                    Log.d("EquipmentRepo", "✅ Duplikat weapon dodat: " + weaponId));
                });
    }

    // ✅ Deaktiviraj opremu (ukloni iz activeEquipment)
    public void deactivateEquipment(String userId, String equipmentId) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile == null || profile.getActiveEquipment() == null) return;

                    List<Equipment> active = profile.getActiveEquipment();
                    active.removeIf(eq -> eq.getId().equals(equipmentId));
                    profile.setActiveEquipment(active);

                    db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                            .document(userId)
                            .set(profile)
                            .addOnSuccessListener(aVoid ->
                                    Log.d("EquipmentRepo", "✅ Oprema deaktivirana: " + equipmentId));
                });
    }

    // ✅ Smanji trajanje clothing-a za 1 borbu
    public void decreaseClothingDuration(String userId) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile == null || profile.getActiveEquipment() == null) return;

                    List<Equipment> active = profile.getActiveEquipment();
                    List<Equipment> toRemove = new ArrayList<>();

                    for (Equipment eq : active) {
                        if (eq instanceof Clothing) {
                            Clothing clothing = (Clothing) eq;
                            clothing.decreaseBattleDuration();
                            if (!clothing.isActive()) {
                                toRemove.add(eq);
                            }
                        }
                    }

                    active.removeAll(toRemove);
                    profile.setActiveEquipment(active);

                    db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                            .document(userId)
                            .set(profile)
                            .addOnSuccessListener(aVoid ->
                                    Log.d("EquipmentRepo", "✅ Clothing trajanje smanjeno"));
                });
    }

    // ✅ Dobij UserProfile (za Service layer)
    public void getUserProfile(String userId, UserProfileCallback callback) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        callback.onFailure(new Exception("UserProfile ne postoji"));
                        return;
                    }
                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile != null) {
                        callback.onSuccess(profile);
                    } else {
                        callback.onFailure(new Exception("Greška pri parsiranju profila"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ✅ Primeni PERMANENT potion boost - povećaj bazni PP trajno i označi kao consumed
    public void applyPermanentPotionBoost(String userId, double ppBoostPercent, String potionId) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile == null) return;

                    // Povećaj bazni PP
                    int currentPP = profile.getPowerPoints();
                    int boost = (int) (currentPP * ppBoostPercent);
                    int newPP = currentPP + boost;
                    profile.setPowerPoints(newPP);

                    // Označi napitak kao potrošen u ownedEquipment
                    List<Equipment> owned = profile.getOwnedEquipment();
                    if (owned != null) {
                        for (Equipment eq : owned) {
                            if (eq.getId().equals(potionId) && eq instanceof com.example.teamgame28.model.Potion) {
                                ((com.example.teamgame28.model.Potion) eq).setConsumed(true);
                                Log.d("EquipmentRepo", "✅ PERMANENT napitak označen kao consumed: " + eq.getName());
                                break;
                            }
                        }
                        profile.setOwnedEquipment(owned);
                    }

                    db.collection(USERS_COLLECTION)
                            .document(userId)
                            .collection(PROFILE_SUBCOLLECTION)
                            .document(userId)
                            .set(profile)
                            .addOnSuccessListener(aVoid ->
                                    Log.d("EquipmentRepo", "✅ PERMANENT boost primenjen: " +
                                          currentPP + " → " + newPP + " PP"))
                            .addOnFailureListener(e ->
                                    Log.e("EquipmentRepo", "❌ Greška pri trajnom boostu", e));
                });
    }

    // Callback interface
    public interface UserProfileCallback {
        void onSuccess(UserProfile profile);
        void onFailure(Exception e);
    }
}
