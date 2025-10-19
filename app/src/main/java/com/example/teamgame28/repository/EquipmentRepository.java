package com.example.teamgame28.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.teamgame28.model.Clothing;
import com.example.teamgame28.model.Equipment;
import com.example.teamgame28.model.UserProfile;
import com.example.teamgame28.model.Weapon;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.List;
import com.google.firebase.firestore.FieldValue;
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

    // ✅ Dodaj item u odgovarajuću owned listu i smanji coin balance
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

                    // Dodaj u odgovarajuću listu po tipu
                    if (equipment instanceof com.example.teamgame28.model.Potion) {
                        List<com.example.teamgame28.model.Potion> owned = profile.getOwnedPotions();
                        if (owned == null) owned = new ArrayList<>();
                        owned.add((com.example.teamgame28.model.Potion) equipment);
                        profile.setOwnedPotions(owned);
                    } else if (equipment instanceof Clothing) {
                        List<Clothing> owned = profile.getOwnedClothing();
                        if (owned == null) owned = new ArrayList<>();
                        owned.add((Clothing) equipment);
                        profile.setOwnedClothing(owned);
                    } else if (equipment instanceof Weapon) {
                        List<Weapon> owned = profile.getOwnedWeapons();
                        if (owned == null) owned = new ArrayList<>();
                        owned.add((Weapon) equipment);
                        profile.setOwnedWeapons(owned);
                    }

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



    /**
     * ✅ Atomski dodaje opremu u odgovarajuću listu KORIŠĆENJEM FieldValue.arrayUnion.
     * Ovo je najsigurniji način da se izbegnu "race conditions".
     *
     * @param userId ID korisnika
     * @param equipment Oprema koja se dodaje
     */
    public void addEquipmentAtomically(String userId, Equipment equipment) {
        String fieldToUpdate; // Naziv polja u Firestore dokumentu

        // Odredi u koju listu (polje) treba dodati opremu
        if (equipment instanceof Weapon) {
            fieldToUpdate = "ownedWeapons";
        } else if (equipment instanceof Clothing) {
            fieldToUpdate = "ownedClothing";
        } else if (equipment instanceof com.example.teamgame28.model.Potion) {
            fieldToUpdate = "ownedPotions";
        } else {
            Log.e("EquipmentRepo", "Nepoznat tip opreme, prekidam dodavanje.");
            return;
        }

        Log.d("EquipmentRepo", "Atomski dodajem " + equipment.getName() + " u polje '" + fieldToUpdate + "'");

        // Izvrši atomsko ažuriranje
        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .update(fieldToUpdate, FieldValue.arrayUnion(equipment))
                .addOnSuccessListener(aVoid ->
                        Log.d("EquipmentRepo", "✅ Oprema uspešno atomski dodata: " + equipment.getName()))
                .addOnFailureListener(e ->
                        Log.e("EquipmentRepo", "❌ Greška pri atomskom dodavanju opreme", e));
    }

// Stara `addEquipmentFree` metoda ti više ne treba, možeš je obrisati ili ostaviti
// ako je koristiš na nekom drugom mestu gde nema paralelnog upisa.


    // ✅ Dodaj opremu koja je pala iz borbe (drop) - SA CALLBACK-om
    public void addDroppedEquipment(String userId, Equipment equipment, AddEquipmentCallback callback) {
        if (equipment == null) {
            Log.e("EquipmentRepo", "❌ Equipment je NULL!");
            if (callback != null) callback.onFailure(new Exception("Equipment je NULL"));
            return;
        }

        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Log.d("EquipmentRepo", "📥 Profil učitan iz Firestore");

                    if (!snapshot.exists()) {
                        Log.e("EquipmentRepo", "❌ Profil NE POSTOJI!");
                        if (callback != null) callback.onFailure(new Exception("Profil ne postoji"));
                        return;
                    }

                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile == null) {
                        Log.e("EquipmentRepo", "❌ Profil je NULL!");
                        if (callback != null) callback.onFailure(new Exception("Profil je NULL"));
                        return;
                    }

                    Log.d("EquipmentRepo", "✅ Profil učitan uspešno");

                    if (equipment instanceof Weapon) {
                        List<Weapon> list = profile.getOwnedWeapons();
                        Log.d("EquipmentRepo", "📋 Trenutna ownedWeapons lista: " + (list != null ? list.size() : "NULL"));
                        if (list == null) list = new ArrayList<>();
                        list.add((Weapon) equipment);
                        profile.setOwnedWeapons(list);
                        Log.d("EquipmentRepo", "➕ WEAPON DODAT! Nova veličina: " + list.size());
                    } else if (equipment instanceof Clothing) {
                        List<Clothing> list = profile.getOwnedClothing();
                        Log.d("EquipmentRepo", "📋 Trenutna ownedClothing lista: " + (list != null ? list.size() : "NULL"));
                        if (list == null) list = new ArrayList<>();
                        list.add((Clothing) equipment);
                        profile.setOwnedClothing(list);
                        Log.d("EquipmentRepo", "➕ CLOTHING DODAT! Nova veličina: " + list.size());
                    } else if (equipment instanceof com.example.teamgame28.model.Potion) {
                        List<com.example.teamgame28.model.Potion> list = profile.getOwnedPotions();
                        Log.d("EquipmentRepo", "📋 Trenutna ownedPotions lista: " + (list != null ? list.size() : "NULL"));
                        if (list == null) list = new ArrayList<>();
                        list.add((com.example.teamgame28.model.Potion) equipment);
                        profile.setOwnedPotions(list);
                        Log.d("EquipmentRepo", "➕ POTION DODAT! Nova veličina: " + list.size());
                    } else {
                        Log.e("EquipmentRepo", "❌❌❌ EQUIPMENT NIJE NI WEAPON NI CLOTHING NI POTION!");
                    }

                    Log.d("EquipmentRepo", "💾 SNIMAM profil u Firestore...");

                    // Snimi profil nazad
                    db.collection(USERS_COLLECTION)
                            .document(userId)
                            .collection(PROFILE_SUBCOLLECTION)
                            .document(userId)
                            .set(profile, SetOptions.merge())

                            .addOnSuccessListener(aVoid -> {
                                Log.d("EquipmentRepo", "✅✅✅ PROFIL SAČUVAN USPEŠNO!");
                                if (callback != null) callback.onSuccess();
                                getOwnedEquipment(userId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("EquipmentRepo", "❌❌❌ GREŠKA PRI SNIMANJU: " + e.getMessage());
                                e.printStackTrace();
                                if (callback != null) callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("EquipmentRepo", "❌❌❌ GREŠKA PRI UČITAVANJU PROFILA: " + e.getMessage());
                    e.printStackTrace();
                    if (callback != null) callback.onFailure(e);
                });
    }

    // ✅ Aktiviraj opremu → ukloni iz owned liste i prebaci u active listu
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

                    // Aktiviraj opremu i ukloni iz owned liste, dodaj u active listu
                    if (equipment instanceof com.example.teamgame28.model.Potion) {
                        com.example.teamgame28.model.Potion potion = (com.example.teamgame28.model.Potion) equipment;

                        // ONETIME napici se sklanjaju iz owned liste
                        if (potion.getPotionType() == com.example.teamgame28.model.PotionType.ONETIME) {
                            List<com.example.teamgame28.model.Potion> owned = profile.getOwnedPotions();
                            if (owned != null) {
                                owned.removeIf(p -> p.getId().equals(potion.getId()));
                                profile.setOwnedPotions(owned);
                            }

                            List<com.example.teamgame28.model.Potion> active = profile.getActivePotions();
                            if (active == null) active = new ArrayList<>();
                            active.add(potion);
                            profile.setActivePotions(active);

                            Log.d("EquipmentRepo", "✅ ONETIME potion aktiviran i uklonjen iz owned liste: " + potion.getName());
                        }
                        // PERMANENT napici se NE sklanjaju iz owned liste, samo se označe kao consumed
                        // To se radi u applyPermanentPotionBoost() metodi

                    } else if (equipment instanceof Clothing) {
                        Clothing clothing = (Clothing) equipment;

                        // Ukloni iz owned liste
                        List<Clothing> owned = profile.getOwnedClothing();
                        if (owned != null) {
                            owned.removeIf(c -> c.getId().equals(clothing.getId()));
                            profile.setOwnedClothing(owned);
                        }

                        // Dodaj u active listu
                        List<Clothing> active = profile.getActiveClothing();
                        if (active == null) active = new ArrayList<>();
                        active.add(clothing);
                        profile.setActiveClothing(active);

                        Log.d("EquipmentRepo", "✅ Clothing aktivirana i uklonjena iz owned liste: " + clothing.getName());

                    } else if (equipment instanceof Weapon) {
                        Weapon weapon = (Weapon) equipment;

                        // Ukloni iz owned liste
                        List<Weapon> owned = profile.getOwnedWeapons();
                        if (owned != null) {
                            owned.removeIf(w -> w.getId().equals(weapon.getId()));
                            profile.setOwnedWeapons(owned);
                        }

                        // Dodaj u active listu
                        List<Weapon> active = profile.getActiveWeapons();
                        if (active == null) active = new ArrayList<>();
                        active.add(weapon);
                        profile.setActiveWeapons(active);

                        Log.d("EquipmentRepo", "✅ Weapon aktiviran i uklonjen iz owned liste: " + weapon.getName());
                    }

                    // Sačuvaj izmene u Firestore
                    db.collection(USERS_COLLECTION)
                            .document(userId)
                            .collection(PROFILE_SUBCOLLECTION)
                            .document(userId)
                            .set(profile)
                            .addOnSuccessListener(aVoid ->
                                    Log.d("EquipmentRepo", "✅ Oprema aktivirana i profil ažuriran"))
                            .addOnFailureListener(e ->
                                    Log.e("EquipmentRepo", "❌ Greška pri aktivaciji opreme", e));
                });
    }

    // ✅ Potroši ONETIME potion → ukloni ga iz activePotions I ownedPotions
    // DEPRECATED: Koristi consumeAllOneTimePotions() umesto ove metode da izbegneš race conditions
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

                    // Ukloni iz activePotions
                    List<com.example.teamgame28.model.Potion> active = profile.getActivePotions();
                    if (active != null) {
                        active.removeIf(eq -> eq.getId().equals(potionId));
                        profile.setActivePotions(active);
                    }

                    // Ukloni iz ownedPotions (ONETIME napitci se potpuno troše)
                    List<com.example.teamgame28.model.Potion> owned = profile.getOwnedPotions();
                    if (owned != null) {
                        owned.removeIf(eq -> eq.getId().equals(potionId));
                        profile.setOwnedPotions(owned);
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

    /**
     * ✅ Potroši SVE aktivne ONETIME potions odjednom - izbegava race conditions.
     * Učitava profil JEDNOM, uklanja sve ONETIME potions iz obe liste, i čuva JEDNOM.
     */
    public void consumeAllOneTimePotions(String userId, PostBattleCallback callback) {
        Log.d("EquipmentRepo", "🔧 consumeAllOneTimePotions CALLED for userId: " + userId);

        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Log.d("EquipmentRepo", "📦 Snapshot retrieved");

                    if (!snapshot.exists()) {
                        Log.e("EquipmentRepo", "❌ Snapshot ne postoji!");
                        if (callback != null) callback.onFailure(new Exception("UserProfile ne postoji"));
                        return;
                    }

                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile == null) {
                        Log.e("EquipmentRepo", "❌ Profile je null nakon parsiranja!");
                        if (callback != null) callback.onFailure(new Exception("Greška pri parsiranju profila"));
                        return;
                    }

                    Log.d("EquipmentRepo", "✅ Profile parsed successfully");

                    List<String> consumedPotionIds = new ArrayList<>();

                    // Pronađi sve ONETIME potions koji su aktivni
                    List<com.example.teamgame28.model.Potion> activePotions = profile.getActivePotions();
                    if (activePotions != null) {
                        Log.d("EquipmentRepo", "Checking " + activePotions.size() + " active potions");
                        for (com.example.teamgame28.model.Potion potion : activePotions) {
                            if (potion.getPotionType() == com.example.teamgame28.model.PotionType.ONETIME && potion.isActive()) {
                                consumedPotionIds.add(potion.getId());
                                Log.d("EquipmentRepo", "Found ONETIME potion to consume: " + potion.getName());
                            }
                        }
                    } else {
                        Log.d("EquipmentRepo", "No active potions");
                    }

                    if (consumedPotionIds.isEmpty()) {
                        Log.d("EquipmentRepo", "Nema ONETIME potions za potrošnju - pozivam SUCCESS callback");
                        if (callback != null) {
                            callback.onSuccess();
                        } else {
                            Log.e("EquipmentRepo", "❌ Callback je NULL!");
                        }
                        return;
                    }

                    // Ukloni iz activePotions
                    if (activePotions != null) {
                        activePotions.removeIf(p -> consumedPotionIds.contains(p.getId()));
                        profile.setActivePotions(activePotions);
                    }

                    // Ukloni iz ownedPotions
                    List<com.example.teamgame28.model.Potion> ownedPotions = profile.getOwnedPotions();
                    if (ownedPotions != null) {
                        ownedPotions.removeIf(p -> consumedPotionIds.contains(p.getId()));
                        profile.setOwnedPotions(ownedPotions);
                    }

                    Log.d("EquipmentRepo", "🔥 Potrošeno " + consumedPotionIds.size() + " ONETIME potions: " + consumedPotionIds);
                    Log.d("EquipmentRepo", "💾 Saving profile to Firestore...");

                    // Sačuvaj profil JEDNOM
                    db.collection(USERS_COLLECTION)
                            .document(userId)
                            .collection(PROFILE_SUBCOLLECTION)
                            .document(userId)
                            .set(profile)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("EquipmentRepo", "✅ Profile saved - pozivam SUCCESS callback");
                                if (callback != null) {
                                    callback.onSuccess();
                                } else {
                                    Log.e("EquipmentRepo", "❌ Callback je NULL!");
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("EquipmentRepo", "❌ Greška pri čuvanju profila", e);
                                if (callback != null) {
                                    callback.onFailure(e);
                                } else {
                                    Log.e("EquipmentRepo", "❌ Callback je NULL!");
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("EquipmentRepo", "❌ Greška pri učitavanju profila iz Firestore", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    } else {
                        Log.e("EquipmentRepo", "❌ Callback je NULL!");
                    }
                });
    }

    // ✅ Unapredi weapon u ownedWeapons
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
                    if (profile == null || profile.getOwnedWeapons() == null) return;

                    List<Weapon> owned = profile.getOwnedWeapons();
                    for (int i = 0; i < owned.size(); i++) {
                        if (owned.get(i).getId().equals(weapon.getId())) {
                            owned.set(i, weapon);
                            break;
                        }
                    }

                    profile.setOwnedWeapons(owned);
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
                    if (profile == null || profile.getOwnedWeapons() == null) return;

                    List<Weapon> owned = profile.getOwnedWeapons();
                    for (Weapon weapon : owned) {
                        if (weapon.getId().equals(weaponId)) {
                            weapon.addDuplicate();
                            break;
                        }
                    }

                    profile.setOwnedWeapons(owned);

                    db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                            .document(userId)
                            .set(profile)
                            .addOnSuccessListener(aVoid ->
                                    Log.d("EquipmentRepo", "✅ Duplikat weapon dodat: " + weaponId));
                });
    }

    // ✅ Deaktiviraj opremu → samo ukloni iz active liste (BRIŠE SE ZAUVEK!)
    // Napomena: Ova metoda se ne koristi u UI - nema ručne deaktivacije!
    // Oprema se briše automatski nakon što se potroši (clothing nakon 2 borbe, ONETIME potions nakon 1 borbe)
    @Deprecated
    public void deactivateEquipment(String userId, String equipmentId) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile == null) return;

                    // BRIŠI opremu iz active liste (ne vraća se u owned!)
                    List<com.example.teamgame28.model.Potion> activePotions = profile.getActivePotions();
                    if (activePotions != null) {
                        activePotions.removeIf(eq -> eq.getId().equals(equipmentId));
                        profile.setActivePotions(activePotions);
                    }

                    List<Clothing> activeClothing = profile.getActiveClothing();
                    if (activeClothing != null) {
                        activeClothing.removeIf(eq -> eq.getId().equals(equipmentId));
                        profile.setActiveClothing(activeClothing);
                    }

                    List<Weapon> activeWeapons = profile.getActiveWeapons();
                    if (activeWeapons != null) {
                        activeWeapons.removeIf(eq -> eq.getId().equals(equipmentId));
                        profile.setActiveWeapons(activeWeapons);
                    }

                    Log.d("EquipmentRepo", "🗑️ Oprema deaktivirana i OBRISANA ZAUVEK: " + equipmentId);

                    // Sačuvaj izmene u Firestore
                    db.collection(USERS_COLLECTION)
                            .document(userId)
                            .collection(PROFILE_SUBCOLLECTION)
                            .document(userId)
                            .set(profile)
                            .addOnSuccessListener(aVoid ->
                                    Log.d("EquipmentRepo", "✅ Oprema obrisana i profil ažuriran"))
                            .addOnFailureListener(e ->
                                    Log.e("EquipmentRepo", "❌ Greška pri brisanju opreme", e));
                });
    }

    // ✅ Smanji trajanje clothing-a za 1 borbu. Istekli clothing se BRIŠE ZAUVEK!
    public void decreaseClothingDuration(String userId) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(PROFILE_SUBCOLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) return;

                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile == null || profile.getActiveClothing() == null) return;

                    List<Clothing> active = profile.getActiveClothing();
                    List<Clothing> toRemove = new ArrayList<>();

                    for (Clothing clothing : active) {
                        clothing.decreaseBattleDuration();
                        if (!clothing.isActive()) {
                            toRemove.add(clothing);
                            Log.d("EquipmentRepo", "🗑️ Clothing istekao i OBRISAN ZAUVEK: " + clothing.getName());
                        }
                    }

                    // Ukloni isteklu odeću iz active liste (BRIŠE SE ZAUVEK!)
                    active.removeAll(toRemove);
                    profile.setActiveClothing(active);

                    // Sačuvaj izmene u Firestore
                    db.collection(USERS_COLLECTION)
                            .document(userId)
                            .collection(PROFILE_SUBCOLLECTION)
                            .document(userId)
                            .set(profile)
                            .addOnSuccessListener(aVoid ->
                                    Log.d("EquipmentRepo", "✅ Clothing trajanje smanjeno i profil ažuriran"))
                            .addOnFailureListener(e ->
                                    Log.e("EquipmentRepo", "❌ Greška pri smanjenju clothing duration-a", e));
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

                    // Povećaj bazni PP na osnovu level-based PP, ne trenutnog powerPoints
                    // Zato što powerPoints možda počinje od 0
                    int userLevel = com.example.teamgame28.service.LevelingService.calculateLevelFromXp(profile.getXp());
                    int levelBasedPP = com.example.teamgame28.service.LevelingService.getTotalPpForLevel(userLevel);

                    int currentBoost = profile.getPowerPoints(); // Trenutni trajni boost
                    int additionalBoost = (int) (levelBasedPP * ppBoostPercent);
                    int newTotalBoost = currentBoost + additionalBoost;
                    profile.setPowerPoints(newTotalBoost);

                    // Označi napitak kao potrošen u ownedPotions
                    List<com.example.teamgame28.model.Potion> owned = profile.getOwnedPotions();
                    if (owned != null) {
                        for (com.example.teamgame28.model.Potion potion : owned) {
                            if (potion.getId().equals(potionId)) {
                                potion.setConsumed(true);
                                Log.d("EquipmentRepo", "✅ PERMANENT napitak označen kao consumed: " + potion.getName());
                                break;
                            }
                        }
                        profile.setOwnedPotions(owned);
                    }

                    db.collection(USERS_COLLECTION)
                            .document(userId)
                            .collection(PROFILE_SUBCOLLECTION)
                            .document(userId)
                            .set(profile)
                            .addOnSuccessListener(aVoid ->
                                    Log.d("EquipmentRepo", "✅ PERMANENT boost primenjen: " +
                                          "+" + additionalBoost + " PP (od level-based " + levelBasedPP +
                                          "), total boost: " + currentBoost + " → " + newTotalBoost))
                            .addOnFailureListener(e ->
                                    Log.e("EquipmentRepo", "❌ Greška pri trajnom boostu", e));
                });
    }

    // Callback interface
    public interface UserProfileCallback {
        void onSuccess(UserProfile profile);
        void onFailure(Exception e);
    }

    public interface PostBattleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface AddEquipmentCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}
