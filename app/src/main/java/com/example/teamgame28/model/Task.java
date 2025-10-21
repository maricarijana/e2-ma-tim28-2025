package com.example.teamgame28.model;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Task implements Serializable {
    private String id;                  // ID zadatka (Firebase/Room)
    private String userId;              // ID korisnika (privremeno hardkodovan)
    private String title;               // Naziv zadatka
    private String description;         // Opis zadatka (opciono)
    private String categoryId;          // ID kategorije (veza sa Category)
    private String categoryName;        // Naziv kategorije (npr. "Zdravlje")
    private String categoryColor;       // Boja kategorije (#HEX)

    private String frequency;           // "jednokratni" ili "ponavljajući"
    private int interval;               // broj (1,2,3...)
    private String intervalUnit;        // "dan" ili "nedelja"
    private Date startDate;           // datum početka
    private Date endDate;             // datum završetka (ako postoji)
    private String time;                // vreme izvršenja (HH:mm)

    private int difficultyXp;           // XP za težinu
    private int importanceXp;           // XP za bitnost
    private int totalXp;                // zbirno XP (difficultyXp + importanceXp)

    private TaskStatus status;          // status: AKTIVAN, URADJEN, OTKAZAN, PAUZIRAN, NEURADJEN
    private boolean recurring;          // da li je ponavljajući
    private String recurringGroupId;    // isti ID za sve instance ponavljanja

    private long creationTimestamp;     // kada je zadatak kreiran
    private long lastActionTimestamp;   // kada je poslednji put ažuriran

    private Date dueDate;             // krajnji datum za jednokratne zadatke
    private List<Long> recurringDates;

    private boolean xpCounted; // da li je XP obračunat za ovaj zadatak
    private String colorHex; // npr. "#FF9800"

    public Task(){
        this.recurringDates = new ArrayList<>();
    }
    public Task(String id, String userId, String title, String description, String categoryId, String categoryName, String categoryColor, String frequency, int interval, String intervalUnit, Date startDate, Date endDate, String time, int difficultyXp, int importanceXp, int totalXp, boolean recurring, String recurringGroupId, TaskStatus status, long creationTimestamp, long lastActionTimestamp, Date dueDate,List<Long> recurringDates, boolean xpCounted, String colorHex) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryColor = categoryColor;
        this.frequency = frequency;
        this.interval = interval;
        this.intervalUnit = intervalUnit;
        this.startDate = startDate;
        this.endDate = endDate;
        this.time = time;
        this.difficultyXp = difficultyXp;
        this.importanceXp = importanceXp;
        this.totalXp = totalXp;
        this.recurring = recurring;
        this.recurringGroupId = recurringGroupId;
        this.status = status;
        this.creationTimestamp = creationTimestamp;
        this.lastActionTimestamp = lastActionTimestamp;
        this.dueDate = dueDate;
        this.recurringDates = (recurringDates != null) ? recurringDates : new ArrayList<>();
        this.xpCounted= xpCounted;
        this.colorHex= colorHex;
    }
    public Task(int difficultyXp, int importanceXp) {
        this.difficultyXp = difficultyXp;
        this.importanceXp = importanceXp;
        this.recurringDates = new ArrayList<>();
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isXpCounted() {
        return xpCounted;
    }

    public void setXpCounted(boolean xpCounted) {
        this.xpCounted = xpCounted;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryColor() {
        return categoryColor;
    }

    public void setCategoryColor(String categoryColor) {
        this.categoryColor = categoryColor;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getIntervalUnit() {
        return intervalUnit;
    }

    public void setIntervalUnit(String intervalUnit) {
        this.intervalUnit = intervalUnit;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getImportanceXp() {
        return importanceXp;
    }

    public void setImportanceXp(int importanceXp) {
        this.importanceXp = importanceXp;
    }

    public int getDifficultyXp() {
        return difficultyXp;
    }

    public void setDifficultyXp(int difficultyXp) {
        this.difficultyXp = difficultyXp;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(int totalXp) {
        this.totalXp = totalXp;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    public String getRecurringGroupId() {
        return recurringGroupId;
    }

    public void setRecurringGroupId(String recurringGroupId) {
        this.recurringGroupId = recurringGroupId;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public long getLastActionTimestamp() {
        return lastActionTimestamp;
    }

    public void setLastActionTimestamp(long lastActionTimestamp) {
        this.lastActionTimestamp = lastActionTimestamp;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }
    public List<Long> getRecurringDates() { return recurringDates; }
    public void setRecurringDates(List<Long> recurringDates) { this.recurringDates = recurringDates; }
    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", categoryColor='" + categoryColor + '\'' +
                ", frequency='" + frequency + '\'' +
                ", interval=" + interval +
                ", intervalUnit='" + intervalUnit + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", time='" + time + '\'' +
                ", difficultyXp=" + difficultyXp +
                ", importanceXp=" + importanceXp +
                ", totalXp=" + totalXp +
                ", status=" + status +
                ", recurring=" + recurring +
                ", recurringGroupId='" + recurringGroupId + '\'' +
                ", creationTimestamp=" + creationTimestamp +
                ", lastActionTimestamp=" + lastActionTimestamp +
                ", dueDate=" + dueDate +
                '}';
    }

    public void calculateTotalXp() {
        this.totalXp = this.difficultyXp + this.importanceXp;
    }
}
