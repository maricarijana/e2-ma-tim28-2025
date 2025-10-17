package com.example.teamgame28.dto;

import java.util.List;

public class StatisticsDto {

    // ---- Task Status Stats ----
    public static class TaskStatusStats {
        private int totalCreated;
        private int finished;
        private int unfinished;
        private int canceled;
        private int active;

        public TaskStatusStats(int totalCreated, int finished, int unfinished, int canceled, int active) {
            this.totalCreated = totalCreated;
            this.finished = finished;
            this.unfinished = unfinished;
            this.canceled = canceled;
            this.active = active;
        }

        public int getTotalCreated() { return totalCreated; }
        public int getFinished() { return finished; }
        public int getUnfinished() { return unfinished; }
        public int getCanceled() { return canceled; }
        public int getActive() { return active; }
    }

    // ---- Category Stats ----
    public static class CategoryStats {
        private String categoryName;
        private int count;

        public CategoryStats(String categoryName, int count) {
            this.categoryName = categoryName;
            this.count = count;
        }

        public String getCategoryName() { return categoryName; }
        public int getCount() { return count; }
    }

    // ---- Overall Avg Difficulty ----
    public static class OverallAvgDifficulty {
        private double avgDifficulty;
        private int totalTasks;
        private String category;
        private String description;

        public OverallAvgDifficulty(double avgDifficulty, int totalTasks, String category, String description) {
            this.avgDifficulty = avgDifficulty;
            this.totalTasks = totalTasks;
            this.category = category;
            this.description = description;
        }

        public double getAvgDifficulty() { return avgDifficulty; }
        public int getTotalTasks() { return totalTasks; }
        public String getCategory() { return category; }
        public String getDescription() { return description; }
    }

    // ---- Longest Streak ----
    public static class LongestStreakResult {
        private int longestStreak;
        private String streakDescription;

        public LongestStreakResult(int longestStreak, String streakDescription) {
            this.longestStreak = longestStreak;
            this.streakDescription = streakDescription;
        }

        public int getLongestStreak() { return longestStreak; }
        public String getStreakDescription() { return streakDescription; }
    }
}
