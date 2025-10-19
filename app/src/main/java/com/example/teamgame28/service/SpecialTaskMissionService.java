package com.example.teamgame28.service;

import android.util.Log;
import com.example.teamgame28.model.AllianceMissionProgress;
import com.example.teamgame28.model.Badge;
import com.example.teamgame28.model.Clothing;
import com.example.teamgame28.model.Potion;
import com.example.teamgame28.model.PotionType;
import com.example.teamgame28.model.Task;
import com.example.teamgame28.model.TaskStatus;
import com.example.teamgame28.model.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

/**
 * Servis koji povezuje regularne zadatke sa specijalnim misijama saveza.
 * Svaki završen zadatak doprinosi oduzimanju HP-a bossu.
 */
public class SpecialTaskMissionService {

    private final FirebaseFirestore db;
    private final AllianceMissionProgressService progressService;

    public SpecialTaskMissionService() {
        this.db = FirebaseFirestore.getInstance();
        this.progressService = new AllianceMissionProgressService();
    }


    // === 1) JAVNI API: pozovi ovo kad kupovina uspe ===
    public void recordShopPurchase(Task maybeTask, String explicitUserIdIfAny) {
        // 1) utvrdi userId (iz Task-a ako ga ima ili iz parametra / FirebaseAuth)
        final String userId;
        if (maybeTask != null && maybeTask.getUserId() != null && !maybeTask.getUserId().isEmpty()) {
            userId = maybeTask.getUserId();
        } else if (explicitUserIdIfAny != null && !explicitUserIdIfAny.isEmpty()) {
            userId = explicitUserIdIfAny;
        } else {
            String temp = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
            if (temp == null) return;
            userId = temp;
        }

        // 2) nađi savez korisnika
        db.collection("alliances")
                .whereArrayContains("members", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(alliances -> {
                    if (alliances.isEmpty()) {
                        Log.d("SpecialMission", "⛔ Korisnik nije u savezu.");
                        return;
                    }
                    String allianceId = alliances.getDocuments().get(0).getId();

                    // 3) nađi AKTIVNU specijalnu misiju tog saveza
                    db.collection("alliance_missions")
                            .whereEqualTo("allianceId", allianceId)
                            .whereEqualTo("active", true)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(missions -> {
                                if (missions.isEmpty()) {
                                    Log.d("SpecialMission", "⛔ Nema aktivne misije.");
                                    return;
                                }
                                String missionId = missions.getDocuments().get(0).getId();

                                // 4) odradi transakciju: +1 kupovina (max 5), -2 HP po kupovini
                                applyShopPurchaseTxn(userId, missionId);
                            })
                            .addOnFailureListener(e -> Log.e("SpecialMission", "❌ Greška misije", e));
                })
                .addOnFailureListener(e -> Log.e("SpecialMission", "❌ Greška saveza", e));
    }

    // === 2) PRIVATNO: transakcija koja poštuje limit 5 i smanjuje bossHp za 2 ===
    // === 2) PRIVATNO: transakcija koja poštuje limit 5 i smanjuje bossHp za 2 ===
    private void applyShopPurchaseTxn(String userId, String missionId) {
        applyMissionProgress(userId, missionId, current -> {
            int already = current.getShopPurchases();
            if (already < 5) {
                current.setShopPurchases(already + 1);
                return 2; // ✅ VRATI ŠTETU ovde
            }
            return 0; // ✅ VRATI 0 ako je limit dostignut
        });
    }

    // === Boss hit (max 10) -2 HP ===
    public void recordBossHit(String userId) {
        if (userId == null || userId.isEmpty()) return;

        db.collection("alliances")
                .whereArrayContains("members", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(alliances -> {
                    if (alliances.isEmpty()) {
                        Log.d("SpecialMission", "⛔ Korisnik nije u savezu.");
                        return;
                    }
                    String allianceId = alliances.getDocuments().get(0).getId();

                    db.collection("alliance_missions")
                            .whereEqualTo("allianceId", allianceId)
                            .whereEqualTo("active", true)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(missions -> {
                                if (missions.isEmpty()) {
                                    Log.d("SpecialMission", "⛔ Nema aktivne misije.");
                                    return;
                                }
                                String missionId = missions.getDocuments().get(0).getId();
                                applyBossHitTxn(userId, missionId);
                            })
                            .addOnFailureListener(e -> Log.e("SpecialMission", "❌ Greška misije", e));
                })
                .addOnFailureListener(e -> Log.e("SpecialMission", "❌ Greška saveza", e));
    }

    private void applyBossHitTxn(String userId, String missionId) {
        applyMissionProgress(userId, missionId, current -> {
            int already = current.getBossHits();
            if (already < 10) {
                current.setBossHits(already + 1);
                return 2; // -2 HP po udarcu
            }
            return 0;
        });
    }

    // === Rešavanje zadataka (max 10) -1 HP, a laki/normalni se broje kao 2 ===
    public void recordTaskCompletion(Task task) {
        if (task == null || task.getStatus() != TaskStatus.FINISHED) return;

        final String userId = (task.getUserId() != null && !task.getUserId().isEmpty())
                ? task.getUserId()
                : FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        db.collection("alliances")
                .whereArrayContains("members", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(alliances -> {
                    if (alliances.isEmpty()) {
                        Log.d("SpecialMission", "⛔ Korisnik nije u savezu.");
                        return;
                    }
                    String allianceId = alliances.getDocuments().get(0).getId();

                    db.collection("alliance_missions")
                            .whereEqualTo("allianceId", allianceId)
                            .whereEqualTo("active", true)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(missions -> {
                                if (missions.isEmpty()) {
                                    Log.d("SpecialMission", "⛔ Nema aktivne misije.");
                                    return;
                                }
                                String missionId = missions.getDocuments().get(0).getId();
                                applyTaskCompletionTxn(userId, missionId, task);
                            })
                            .addOnFailureListener(e -> Log.e("SpecialMission", "❌ Greška misije", e));
                })
                .addOnFailureListener(e -> Log.e("SpecialMission", "❌ Greška saveza", e));
    }

    private void applyTaskCompletionTxn(String userId, String missionId, Task task) {
        int diff = task.getDifficultyXp();
        int imp  = task.getImportanceXp();

        // Kombinacija "lak" (3 XP) i "normalan" (1 XP) računa se kao 2 puta
        final boolean isLightAndNormal = (diff == 3 && imp == 1);
        final int slotsWanted = isLightAndNormal ? 2 : 1;

        applyMissionProgress(userId, missionId, current -> {
            int used = current.getTaskPoints();
            int remaining = 10 - used;
            if (remaining <= 0) return 0;

            int slotsApplied = Math.min(slotsWanted, remaining);
            current.setTaskPoints(used + slotsApplied);

            // Svaki završen zadatak uvek nanosi 1 HP štete
            return 1;
        });
    }


    // === Ostali zadaci (max 6) -4 HP ===
    public void recordOtherTask(Task task) {
        if (task == null || task.getStatus() != TaskStatus.FINISHED) return;

        final String userId = (task.getUserId() != null && !task.getUserId().isEmpty())
                ? task.getUserId()
                : FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        db.collection("alliances")
                .whereArrayContains("members", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(alliances -> {
                    if (alliances.isEmpty()) {
                        Log.d("SpecialMission", "⛔ Korisnik nije u savezu.");
                        return;
                    }
                    String allianceId = alliances.getDocuments().get(0).getId();

                    db.collection("alliance_missions")
                            .whereEqualTo("allianceId", allianceId)
                            .whereEqualTo("active", true)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(missions -> {
                                if (missions.isEmpty()) {
                                    Log.d("SpecialMission", "⛔ Nema aktivne misije.");
                                    return;
                                }
                                String missionId = missions.getDocuments().get(0).getId();
                                applyOtherTaskTxn(userId, missionId);
                            });
                });
    }

    private void applyOtherTaskTxn(String userId, String missionId) {
        applyMissionProgress(userId, missionId, current -> {
            int already = current.getTasksCompleted();
            if (already < 6) {
                current.setTasksCompleted(already + 1);
                return 4; // -4 HP
            }
            return 0;
        });
    }


    // === Nema nerešenih zadataka -10 HP ===
    public void recordNoUnfinishedTasks(String userId, boolean hasNoUnfinished) {
        if (userId == null || userId.isEmpty() || !hasNoUnfinished) return;

        db.collection("alliances")
                .whereArrayContains("members", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(alliances -> {
                    if (alliances.isEmpty()) return;
                    String allianceId = alliances.getDocuments().get(0).getId();

                    db.collection("alliance_missions")
                            .whereEqualTo("allianceId", allianceId)
                            .whereEqualTo("active", true)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(missions -> {
                                if (missions.isEmpty()) return;
                                String missionId = missions.getDocuments().get(0).getId();
                                applyNoUnfinishedTasksTxn(userId, missionId);
                            });
                });
    }

    private void applyNoUnfinishedTasksTxn(String userId, String missionId) {
        applyMissionProgress(userId, missionId, current -> {
            if (!current.isNoUnfinishedTasks()) {
                current.setNoUnfinishedTasks(true);
                return 10; // -10 HP
            }
            return 0;
        });
    }

    // === Slanje poruke savezu (1 po danu) -4 HP ===
    public void recordAllianceMessage(String userId) {
        if (userId == null || userId.isEmpty()) return;

        db.collection("alliances")
                .whereArrayContains("members", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(alliances -> {
                    if (alliances.isEmpty()) return;
                    String allianceId = alliances.getDocuments().get(0).getId();

                    db.collection("alliance_missions")
                            .whereEqualTo("allianceId", allianceId)
                            .whereEqualTo("active", true)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(missions -> {
                                if (missions.isEmpty()) return;
                                String missionId = missions.getDocuments().get(0).getId();
                                applyMessageDayTxn(userId, missionId);
                            });
                });
    }

    private void applyMessageDayTxn(String userId, String missionId) {
        final var progressRef = db.collection("alliance_mission_progress").document(userId + "_" + missionId);
        final var missionRef = db.collection("alliance_missions").document(missionId);

        db.runTransaction(transaction -> {
            var snap = transaction.get(progressRef);
            AllianceMissionProgress current = snap.exists()
                    ? snap.toObject(AllianceMissionProgress.class)
                    : new AllianceMissionProgress();

            if (current == null) current = new AllianceMissionProgress();
            if (current.getMissionId() == null) current.setMissionId(missionId);
            if (current.getUserId() == null) current.setUserId(userId);

            // ✅ Nova logika — zabrani više poruka u istom danu
            long now = System.currentTimeMillis();
            if (isSameDay(current.getLastMessageTimestamp(), now)) {
                Log.d("SpecialMission", "📅 Poruka već poslata danas — nema dodatnog HP smanjenja.");
                return 0;
            }

            // ✅ Limit 14 dana
            if (current.getDaysWithMessages() >= 14) {
                Log.d("SpecialMission", "⚠️ Limit poruka dostignut (14/14).");
                return 0;
            }

            // ✅ Ažuriraj stanje
            current.setLastMessageTimestamp(now);
            current.setDaysWithMessages(current.getDaysWithMessages() + 1);
            current.setDamageDealt(current.getDamageDealt() + 4);
            transaction.set(progressRef, current);

            // ✅ Umanji boss HP
            transaction.update(missionRef, "bossHp",
                    com.google.firebase.firestore.FieldValue.increment(-4));

            return 4;
        }).addOnSuccessListener(dmg -> {
            if (dmg > 0)
                Log.d("SpecialMission", "💬 Poruka u savezu! Boss HP -" + dmg);
        }).addOnFailureListener(e -> Log.e("SpecialMission", "❌ Transakcija poruke neuspešna", e));
    }
    /**
     * Centralna metoda za ažuriranje misije i HP-a bossa.
     */
    /**
     * Centralna metoda za ažuriranje misije i HP-a bossa.
     * Logika ažuriranja sada vraća iznos štete koju treba primeniti.
     */
    private void applyMissionProgress(String userId, String missionId,
                                      java.util.function.Function<AllianceMissionProgress, Integer> updateAndCalculateDamageLogic) {
        final var progressRef = db.collection("alliance_mission_progress").document(userId + "_" + missionId);
        final var missionRef = db.collection("alliance_missions").document(missionId);

        db.runTransaction(transaction -> {
            var snap = transaction.get(progressRef);
            AllianceMissionProgress current = snap.exists()
                    ? snap.toObject(AllianceMissionProgress.class)
                    : new AllianceMissionProgress();

            if (current == null) current = new AllianceMissionProgress();
            if (current.getMissionId() == null) current.setMissionId(missionId);
            if (current.getUserId() == null) current.setUserId(userId);

            // 🔹 Primeni prilagođenu logiku koja sada vraća iznos štete
            int damageDealtInTxn = updateAndCalculateDamageLogic.apply(current);

            // 🔹 Ažuriraj damage i boss HP samo ako je šteta naneta
            if (damageDealtInTxn > 0) {
                current.setDamageDealt(current.getDamageDealt() + damageDealtInTxn);
                transaction.set(progressRef, current);
                transaction.update(missionRef, "bossHp",
                        com.google.firebase.firestore.FieldValue.increment(-damageDealtInTxn));
            }

            return damageDealtInTxn;
        }).addOnSuccessListener(dmg -> {
            if (dmg > 0) {
                Log.d("SpecialMission", "✅ Uspešna akcija! Boss HP -" + dmg);
                checkBossDefeat(missionId);
            } else {
                Log.d("SpecialMission", "✋ Akcija nije primenila štetu (limit dostignut).");
            }
        }).addOnFailureListener(e -> Log.e("SpecialMission", "❌ Transakcija neuspešna", e));
    }
    // ===== ⬇️ IZMENA #2 (Logika za proveru isteka misije) ⬇️ =====
    /**
     * Proverava da li je boss poražen i dodeljuje nagrade
     * ili proverava da li je misija istekla bez pobede.
     */
    private void checkBossDefeat(String missionId) {
        db.collection("alliance_missions").document(missionId)
                .get()
                .addOnSuccessListener(missionDoc -> {
                    if (!missionDoc.exists()) return;

                    // 🔹 Proveri da li je misija i dalje aktivna
                    Boolean isActive = missionDoc.getBoolean("active");
                    if (isActive == null || !isActive) {
                        Log.d("SpecialMission", "Misija " + missionId + " je već neaktivna.");
                        return;
                    }

                    Long bossHp = missionDoc.getLong("bossHp");
                    Long endTime = missionDoc.getLong("endTime");
                    long now = System.currentTimeMillis();
                    String allianceId = missionDoc.getString("allianceId");

                    if (allianceId == null) {
                        Log.e("SpecialMission", "❌ Misija nema definisan allianceId.");
                        return;
                    }

                    // 🔸 Ako je misija istekla (bez obzira na HP)
                    if (endTime != null && now > endTime) {
                        Log.d("SpecialMission", "🕒 Misija " + missionId + " je istekla. Proveravam nerešene zadatke...");
                        checkUnfinishedTasksForAlliance(allianceId);

                        // Deaktiviraj misiju da se ne ponavlja
                        missionDoc.getReference().update("active", false);
                        return;
                    }

                    // 🔹 Ako je boss poražen (HP <= 0)
                    if (bossHp != null && bossHp <= 0) {
                        Log.d("SpecialMission", "💀 Boss poražen! Dodeljujem nagrade...");

                        // ✅ Prvo proveri da li članovi imaju nerešene zadatke
                        checkUnfinishedTasksForAlliance(allianceId);
                        // Deaktiviraj misiju da se ne ponavlja dodela
                        missionDoc.getReference().update("active", false);

                        db.collection("alliances").document(allianceId)
                                .get()
                                .addOnSuccessListener(allianceDoc -> {
                                    if (!allianceDoc.exists()) return;
                                    List<String> members = (List<String>) allianceDoc.get("members");

                                    if (members != null) {
                                        for (String memberId : members) {
                                            // 🎁 Nagrada: napitak, odeća, 50% coina i bedž
                                            grantMissionRewardsToUser(memberId);
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("SpecialMission", "❌ Greška pri proveri stanja misije " + missionId, e));
    }
    public void triggerMissionCheck(String missionId) {
        checkBossDefeat(missionId);
    }
    // === 🎁 NAGRADA KORISNIKU NAKON POBEDE BOSSA U MISIJI ===
    public static void grantMissionRewardsToUser(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        EquipmentService equipmentService = new EquipmentService();

        // ✅ Tražimo profil korisnika u podkolekciji app_users/{userId}/profile
        db.collection("app_users")
                .document(userId)
                .collection("profile")
                .get()
                .addOnSuccessListener(profileQuery -> {
                    if (profileQuery.isEmpty()) {
                        Log.e("SpecialMission", "❌ Nema profila u 'app_users/" + userId + "/profile'");
                        return;
                    }

                    // Uzimamo prvi dokument iz profila (jer imaš samo jedan)
                    DocumentSnapshot doc = profileQuery.getDocuments().get(0);
                    UserProfile profile = doc.toObject(UserProfile.class);
                    if (profile == null) {
                        Log.e("SpecialMission", "❌ Profil korisnika je null!");
                        return;
                    }

                    int level = profile.getLevel();

                    // 💰 1️⃣ Novčići
                    int nextLevelReward = equipmentService.calculateBossRewardForLevel(level + 1);
                    int rewardCoins = nextLevelReward / 2;
                    profile.addCoins(rewardCoins);

                    // 🧪 2️⃣ Napitak
                    List<Potion> potions = equipmentService.getAvailablePotions();
                    if (!potions.isEmpty()) {
                        profile.getOwnedPotions().add(potions.get(0));
                    }

                    // 👕 3️⃣ Odeća
                    List<Clothing> clothes = equipmentService.getAvailableClothes();
                    if (!clothes.isEmpty()) {
                        profile.getOwnedClothing().add(clothes.get(0));
                    }

                    // 🏅 4️⃣ Bedž
                    int completedCount = 0;
                    Badge existingBadge = null;

                    if (profile.getBadges() != null) {
                        for (Badge b : profile.getBadges()) {
                            if ("Specijalni pobednik".equals(b.getName())) {
                                existingBadge = b;
                                break;
                            }
                        }
                    }

                    if (existingBadge != null) {
                        completedCount = existingBadge.getCount();
                        profile.getBadges().remove(existingBadge);
                    }

                    Badge newBadge = new Badge(
                            "badge_special_" + System.currentTimeMillis(),
                            "Specijalni pobednik",
                            "Broj uspešno završenih specijalnih misija: " + (completedCount + 1),
                            completedCount + 1,
                            "ic_badge_special",
                            System.currentTimeMillis()
                    );
                    newBadge.setCount(completedCount + 1);
                    profile.addBadge(newBadge);

                    // 💾 5️⃣ Sačuvaj profil nazad u isti dokument
                    db.collection("app_users")
                            .document(userId)
                            .collection("profile")
                            .document(doc.getId())  // koristi isti dokument ID
                            .set(profile)
                            .addOnSuccessListener(a ->
                                    Log.d("SpecialMission", "🎁 Nagrade uspešno dodeljene za " + userId))
                            .addOnFailureListener(e ->
                                    Log.e("SpecialMission", "❌ Greška pri upisu nagrada", e));
                })
                .addOnFailureListener(e ->
                        Log.e("SpecialMission", "❌ Nije moguće učitati profil korisnika", e));
    }

    /**
     * Proverava da li su dva timestamp-a (u milisekundama) u istom danu.
     */
    private boolean isSameDay(long t1, long t2) {
        if (t1 <= 0 || t2 <= 0) return false;

        java.util.Calendar c1 = java.util.Calendar.getInstance();
        java.util.Calendar c2 = java.util.Calendar.getInstance();
        c1.setTimeInMillis(t1);
        c2.setTimeInMillis(t2);

        return c1.get(java.util.Calendar.YEAR) == c2.get(java.util.Calendar.YEAR)
                && c1.get(java.util.Calendar.DAY_OF_YEAR) == c2.get(java.util.Calendar.DAY_OF_YEAR);
    }

    /**
     * Proverava da li članovi saveza imaju nerešene zadatke
     * i dodeljuje -10 HP ako su svi završeni.
     */
    /**
     * Proverava da li članovi saveza imaju nerešene zadatke
     * i dodeljuje -10 HP ako su svi završeni.
     */
    private void checkUnfinishedTasksForAlliance(String allianceId) {
        db.collection("alliances").document(allianceId)
                .get()
                .addOnSuccessListener(allianceDoc -> {
                    if (!allianceDoc.exists()) return;
                    List<String> members = (List<String>) allianceDoc.get("members");
                    if (members == null) return;

                    for (String memberId : members) {
                        // 🔹 Proveri da li korisnik ima aktivne ili nerešene zadatke
                        db.collection("tasks")
                                .whereEqualTo("userId", memberId)
                                .whereIn("status", List.of("ACTIVE", "UNFINISHED"))
                                .get()
                                .addOnSuccessListener(tasks -> {
                                    boolean hasUnfinished = !tasks.isEmpty();
                                    if (!hasUnfinished) {
                                        // ✅ Ako nema nijedan aktivan/nerešen zadatak — dodeli bonus (-10 HP)
                                        recordNoUnfinishedTasks(memberId, true);
                                        Log.d("SpecialMission", "🎯 Korisnik " + memberId + " nema nerešenih zadataka (-10 HP).");
                                    } else {
                                        Log.d("SpecialMission", "📋 Korisnik " + memberId + " ima nerešene zadatke (" + tasks.size() + ").");
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Log.e("SpecialMission", "❌ Greška pri proveri zadataka za " + memberId, e));
                    }
                });
    }


}
