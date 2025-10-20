package com.example.teamgame28.service;

/**
 * Centralizovani servis za sve kalkulacije vezane za leveling sistem.
 *
 * Formule:
 * - XP za nivo: XP_prethodni * 2 + XP_prethodni / 2 (zaokruženo na najbližu stotinu)
 * - PP nagrada: PP_prethodni + 3/4 * PP_prethodni (zaokruženo)
 * - XP taskova: XP_prethodni + XP_prethodni / 2 (zaokruženo)
 */
public class LevelingService {

    // Bazne vrednosti za level 0
    private static final int BASE_XP_FOR_LEVEL_1 = 200;
    private static final int BASE_PP_REWARD = 40;

    // Bazne XP vrednosti za težinu (level 0)
    private static final int[] BASE_DIFFICULTY_XP = {1, 3, 7, 20};

    // Bazne XP vrednosti za bitnost (level 0)
    private static final int[] BASE_IMPORTANCE_XP = {1, 3, 10, 100};

    /**
     * Računa koliko ukupnog XP-a je potrebno da bi korisnik dostigao određeni nivo.
     *
     * Primer:
     * - Level 1: 200 XP
     * - Level 2: 500 XP
     * - Level 3: 1250 XP
     *
     * @param level Ciljni nivo
     * @return Ukupan XP potreban za dostizanje tog nivoa
     */
    public static int getXpRequiredForLevel(int level) {
        if (level <= 0) return 0;
        if (level == 1) return BASE_XP_FOR_LEVEL_1;

        int previousLevelXp = getXpRequiredForLevel(level - 1);
        int calculatedXp = previousLevelXp * 2 + previousLevelXp / 2;

        // Zaokruživanje na najbližu stotinu
        return roundToNearestHundred(calculatedXp);
    }

    /**
     * Računa koliko PP-a korisnik dobija kada pređe na određeni nivo.
     *
     * Primer:
     * - Level 1: 40 PP
     * - Level 2: 70 PP
     * - Level 3: 123 PP
     *
     * @param level Nivo koji je korisnik dostigao
     * @return Broj PP-a koji dobija za taj nivo
     */
    public static int getPpRewardForLevel(int level) {
        if (level <= 0) return 0;
        if (level == 1) return BASE_PP_REWARD;

        int previousPp = getPpRewardForLevel(level - 1);
        double calculatedPp = previousPp + (3.0 / 4.0 * previousPp);

        return (int) Math.round(calculatedPp);
    }

    /**
     * Računa XP vrednost za određeni indeks težine na određenom nivou.
     *
     * Primer za "Važan" (indeks 1, bazna vrednost 3):
     * - Level 0: 3 XP
     * - Level 1: 5 XP
     * - Level 2: 8 XP
     *
     * @param difficultyIndex Indeks težine (0-3)
     * @param userLevel Trenutni nivo korisnika
     * @return XP vrednost za tu težinu
     */
    public static int getDifficultyXpForLevel(int difficultyIndex, int userLevel) {
        if (difficultyIndex < 0 || difficultyIndex >= BASE_DIFFICULTY_XP.length) {
            return 0;
        }

        int baseXp = BASE_DIFFICULTY_XP[difficultyIndex];
        return calculateScaledXp(baseXp, userLevel);
    }

    /**
     * Računa XP vrednost za određeni indeks bitnosti na određenom nivou.
     *
     * Primer za "Specijalan" (indeks 3, bazna vrednost 100):
     * - Level 0: 100 XP
     * - Level 1: 150 XP
     * - Level 2: 225 XP
     *
     * @param importanceIndex Indeks bitnosti (0-3)
     * @param userLevel Trenutni nivo korisnika
     * @return XP vrednost za tu bitnost
     */
    public static int getImportanceXpForLevel(int importanceIndex, int userLevel) {
        if (importanceIndex < 0 || importanceIndex >= BASE_IMPORTANCE_XP.length) {
            return 0;
        }

        int baseXp = BASE_IMPORTANCE_XP[importanceIndex];
        return calculateScaledXp(baseXp, userLevel);
    }

    /**
     * Vraća labels za difficulty spinner sa ažuriranim XP vrednostima za određeni nivo.
     */
    public static String[] getDifficultyLabels(int userLevel) {
        return new String[]{
            "Veoma lak (" + getDifficultyXpForLevel(0, userLevel) + " XP)",
            "Lak (" + getDifficultyXpForLevel(1, userLevel) + " XP)",
            "Težak (" + getDifficultyXpForLevel(2, userLevel) + " XP)",
            "Ekstremno težak (" + getDifficultyXpForLevel(3, userLevel) + " XP)"
        };
    }

    /**
     * Vraća labels za importance spinner sa ažuriranim XP vrednostima za određeni nivo.
     */
    public static String[] getImportanceLabels(int userLevel) {
        return new String[]{
            "Normalan (" + getImportanceXpForLevel(0, userLevel) + " XP)",
            "Važan (" + getImportanceXpForLevel(1, userLevel) + " XP)",
            "Ekstremno važan (" + getImportanceXpForLevel(2, userLevel) + " XP)",
            "Specijalan (" + getImportanceXpForLevel(3, userLevel) + " XP)"
        };
    }

    /**
     * Računa koliko XP-a još treba korisniku da dostigne sledeći nivo.
     *
     * @param currentTotalXp Ukupan XP koji korisnik trenutno ima
     * @param currentLevel Trenutni nivo korisnika
     * @return Broj XP-a koji još treba do sledećeg nivoa
     */
    public static int getXpRemainingForNextLevel(int currentTotalXp, int currentLevel) {
        int xpRequiredForNextLevel = getXpRequiredForLevel(currentLevel + 1);
        return Math.max(0, xpRequiredForNextLevel - currentTotalXp);
    }

    /**
     * Određuje trenutni nivo na osnovu ukupnog XP-a.
     *
     * @param totalXp Ukupan XP koji korisnik ima
     * @return Trenutni nivo korisnika
     */
    public static int calculateLevelFromXp(int totalXp) {
        int level = 0;
        while (totalXp >= getXpRequiredForLevel(level + 1)) {
            level++;
        }
        return level;
    }

    /**
     * Vraća titulu za određeni nivo.
     * Svaki nivo ima svoju jedinstvenu titulu.
     *
     * @param level Nivo korisnika
     * @return Titula koja odgovara nivou
     */
    public static String getTitleForLevel(int level) {
        switch (level) {
            case 0:
                return "Početnik";
            case 1:
                return "Učenik";
            case 2:
                return "Borac";
            case 3:
                return "Ratnik";
            default:
                return "Ratnik Nivo " + level;
        }
    }

    /**
     * Računa ukupan kumulativni PP koji korisnik treba da ima za određeni nivo.
     * Sabira sve PP nagrade od nivoa 1 do trenutnog nivoa.
     *
     * Primer:
     * - Nivo 0: 0 PP
     * - Nivo 1: 40 PP
     * - Nivo 2: 40 + 70 = 110 PP
     * - Nivo 3: 40 + 70 + 123 = 233 PP
     *
     * @param level Trenutni nivo korisnika
     * @return Ukupan kumulativni PP
     */
    public static int getTotalPpForLevel(int level) {
        int totalPP = 0;
        for (int lvl = 1; lvl <= level; lvl++) {
            totalPP += getPpRewardForLevel(lvl);
        }
        return totalPP;
    }

    // ==================== Helper metode ====================

    /**
     * Računa skaliranu XP vrednost na osnovu nivoa.
     * Formula: XP_prethodni + XP_prethodni / 2 (zaokruženo)
     */
    private static int calculateScaledXp(int baseXp, int level) {
        if (level <= 0) return baseXp;

        int previousXp = calculateScaledXp(baseXp, level - 1);
        double calculatedXp = previousXp + (previousXp / 2.0);

        return (int) Math.round(calculatedXp);
    }

    /**
     * Zaokružuje broj na najbližu stotinu.
     * Primer: 500 → 500, 501 → 500, 550 → 600, 1250 → 1300
     */
    private static int roundToNearestHundred(int value) {
        return (int) (Math.round(value / 100.0) * 100);
    }
}
