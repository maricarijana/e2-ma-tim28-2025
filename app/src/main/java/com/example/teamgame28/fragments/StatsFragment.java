package com.example.teamgame28.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.teamgame28.R;
import com.example.teamgame28.dto.StatisticsDto;
import com.example.teamgame28.model.UserProfile;
import com.example.teamgame28.repository.UserRepository;
import com.example.teamgame28.service.StatisticsService;
import com.example.teamgame28.viewmodels.StatisticsViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment {

    private TextView statsActiveDays;
    private TextView statsTasksTotal;
    private TextView statsTasksFinished;
    private TextView statsTasksUnfinished;
    private TextView statsTasksCancelled;
    private TextView statsOverallAvgXp;
    private TextView statsOverallCategory;
    private TextView statsOverallDescription;
    private TextView statsLongestStreak;
    private PieChart tasksDonutChart;
    private BarChart categoryBarChart;
    private LineChart xpLineChart;
    private LineChart avgDifficultyLineChart;

    private UserRepository userRepository;
    private StatisticsViewModel statisticsViewModel;
    private FirebaseAuth auth;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        // Inicijalizacija view komponenti
        statsActiveDays = view.findViewById(R.id.stats_active_days);
        statsTasksTotal = view.findViewById(R.id.stats_tasks_total);
        statsTasksFinished = view.findViewById(R.id.stats_tasks_finished);
        statsTasksUnfinished = view.findViewById(R.id.stats_tasks_unfinished);
        statsTasksCancelled = view.findViewById(R.id.stats_tasks_cancelled);
        statsOverallAvgXp = view.findViewById(R.id.stats_overall_avg_xp);
        statsOverallCategory = view.findViewById(R.id.stats_overall_category);
        statsOverallDescription = view.findViewById(R.id.stats_overall_description);
        statsLongestStreak = view.findViewById(R.id.stats_longest_streak);
        tasksDonutChart = view.findViewById(R.id.tasks_donut_chart);
        categoryBarChart = view.findViewById(R.id.category_bar_chart);
        xpLineChart = view.findViewById(R.id.xp_line_chart);
        avgDifficultyLineChart = view.findViewById(R.id.avg_difficulty_line_chart);

        // Firebase, Repository i ViewModel
        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        statisticsViewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);

        // Trenutno prijavljeni korisnik
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            loadStatistics();
        }

        return view;
    }

    private void loadStatistics() {
        // Učitaj aktivne dane iz UserProfile
        userRepository.getUserProfileById(currentUserId, new UserRepository.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile userProfile) {
                if (userProfile != null) {
                    statsActiveDays.setText(String.valueOf(userProfile.getActiveDays()));
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Greška pri učitavanju statistike", Toast.LENGTH_SHORT).show();
            }
        });

        // Učitaj statistiku zadataka preko ViewModel-a
        statisticsViewModel.loadTaskStatistics();

        statisticsViewModel.getTaskStatusCounts().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                displayTaskStats(stats);
            }
        });


        statisticsViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // Observe statistiku po kategorijama
        statisticsViewModel.getCategoryStats().observe(getViewLifecycleOwner(), categoryStats -> {
            if (categoryStats != null && !categoryStats.isEmpty()) {
                displayCategoryBarChart(categoryStats);
            }
        });


        // Observe XP napredak za poslednjih 7 dana
        statisticsViewModel.getXpData().observe(getViewLifecycleOwner(), xpData -> {
            if (xpData != null && !xpData.isEmpty()) {
                displayXpLineChart(xpData);
            }
        });

        // Observe prosečnu težinu završenih zadataka
        statisticsViewModel.getAvgDifficultyData().observe(getViewLifecycleOwner(), avgData -> {
            if (avgData != null && !avgData.isEmpty()) {
                displayAvgDifficultyLineChart(avgData);
            }
        });

        // Observe ukupan prosek težine svih zadataka
        statisticsViewModel.getOverallAvgDifficulty().observe(getViewLifecycleOwner(), overallData -> {
            if (overallData != null) {
                displayOverallAvgDifficulty(overallData);
            }
        });


        // Observe najduži niz uspešno urađenih zadataka
        statisticsViewModel.getLongestStreak().observe(getViewLifecycleOwner(), streakData -> {
            if (streakData != null) {
                displayLongestStreak(streakData);
            }
        });


    }

    private void displayLongestStreak(StatisticsDto.LongestStreakResult data) {
        statsLongestStreak.setText(String.valueOf(data.getLongestStreak()));
    }


    private void displayOverallAvgDifficulty(StatisticsDto.OverallAvgDifficulty data) {
        statsOverallAvgXp.setText(String.format(Locale.getDefault(), "%.1f", data.getAvgDifficulty()));
        statsOverallCategory.setText(data.getCategory());
        statsOverallDescription.setText(data.getDescription());
    }


    private void displayTaskStats(StatisticsDto.TaskStatusStats stats) {
        int total = stats.getTotalCreated();
        int finished = stats.getFinished();
        int unfinished = stats.getUnfinished();
        int cancelled = stats.getCanceled();
        int active = stats.getActive();

        // Ažuriraj tekstualne labele
        statsTasksTotal.setText(String.valueOf(total));
        statsTasksFinished.setText(String.valueOf(finished));
        statsTasksUnfinished.setText(String.valueOf(unfinished));
        statsTasksCancelled.setText(String.valueOf(cancelled));

        // Nacrtaj donut grafikon
        drawDonutChart(total, finished, unfinished, cancelled);
    }


    private void drawDonutChart(int total, int finished, int unfinished, int cancelled) {
        if (total == 0) {
            tasksDonutChart.setNoDataText("Nema kreiranih zadataka");
            tasksDonutChart.invalidate();
            return;
        }

        // Pripremi podatke za grafikon
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(finished, "Završenih"));
        entries.add(new PieEntry(unfinished, "Nezavršenih"));
        entries.add(new PieEntry(cancelled, "Otkazanih"));

        int active = total - finished - unfinished - cancelled;
        if (active > 0) {
            entries.add(new PieEntry(active, "Aktivnih"));
        }

        // Dataset sa bojama
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(
                Color.parseColor("#4CAF50"),   // Zelena - završeni
                Color.parseColor("#FF9800"),   // Narandžasta - nezavršeni
                Color.parseColor("#F44336"),   // Crvena - otkazani
                Color.parseColor("#9C27B0")    // Ljubičasta - aktivni
        );
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);
        dataSet.setSliceSpace(2f);

        // Postavi podatke
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        tasksDonutChart.setData(data);
        tasksDonutChart.setUsePercentValues(false);
        tasksDonutChart.getDescription().setEnabled(false);

        // DONUT EFEKAT - rupa u sredini!
        tasksDonutChart.setDrawHoleEnabled(true);
        tasksDonutChart.setHoleRadius(50f);
        tasksDonutChart.setTransparentCircleRadius(55f);
        tasksDonutChart.setHoleColor(Color.WHITE);

        // Tekst u centru
        tasksDonutChart.setDrawCenterText(true);
        tasksDonutChart.setCenterText(total + "\nukupno");
        tasksDonutChart.setCenterTextSize(18f);
        tasksDonutChart.setCenterTextColor(Color.BLACK);

        // Legenda
        tasksDonutChart.getLegend().setEnabled(false); // Koristimo custom legendu ispod

        tasksDonutChart.animateY(1000);
        tasksDonutChart.invalidate();
    }

    private void displayCategoryBarChart(List<StatisticsDto.CategoryStats> categoryStats) {
        if (categoryStats.isEmpty()) {
            categoryBarChart.setNoDataText("Nema završenih zadataka po kategorijama");
            categoryBarChart.invalidate();
            return;
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> categoryNames = new ArrayList<>();

        int index = 0;
        for (StatisticsDto.CategoryStats stat : categoryStats) {
            entries.add(new BarEntry(index, stat.getCount()));
            categoryNames.add(stat.getCategoryName());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Završeni zadaci");
        dataSet.setColor(Color.parseColor("#9C27B0"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);

        categoryBarChart.setData(data);
        categoryBarChart.getDescription().setEnabled(false);
        categoryBarChart.setFitBars(true);

        XAxis xAxis = categoryBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(categoryNames));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelRotationAngle(-45f);

        categoryBarChart.getAxisLeft().setGranularity(1f);
        categoryBarChart.getAxisRight().setEnabled(false);
        categoryBarChart.getLegend().setEnabled(false);

        categoryBarChart.animateY(1000);
        categoryBarChart.invalidate();
    }


    private void displayXpLineChart(List<StatisticsService.XpDataPoint> xpData) {
        if (xpData == null || xpData.isEmpty()) {
            xpLineChart.setNoDataText("Nema XP podataka za poslednjih 7 dana");
            xpLineChart.invalidate();
            return;
        }

        // Pripremi podatke za line chart
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> dateLabels = new ArrayList<>();
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());

        for (int i = 0; i < xpData.size(); i++) {
            StatisticsService.XpDataPoint dataPoint = xpData.get(i);
            entries.add(new Entry(i, dataPoint.getXp()));

            // Formatiraj datum za prikaz
            try {
                Date date = inputFormat.parse(dataPoint.getDate());
                String formattedDate = outputFormat.format(date);
                dateLabels.add(formattedDate);
            } catch (Exception e) {
                dateLabels.add(dataPoint.getDate());
            }
        }

        // Dataset
        LineDataSet dataSet = new LineDataSet(entries, "XP");
        dataSet.setColor(Color.parseColor("#9C27B0")); // Ljubičasta
        dataSet.setCircleColor(Color.parseColor("#9C27B0"));
        dataSet.setCircleRadius(5f);
        dataSet.setLineWidth(3f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E1BEE7")); // Svetlo ljubičasta
        dataSet.setFillAlpha(100);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Glatke linije

        // Postavi podatke
        LineData data = new LineData(dataSet);

        xpLineChart.setData(data);
        xpLineChart.getDescription().setEnabled(false);

        // X-osa sa datumima
        XAxis xAxis = xpLineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dateLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelRotationAngle(-45f);

        // Y-osa
        xpLineChart.getAxisLeft().setGranularity(1f);
        xpLineChart.getAxisRight().setEnabled(false);

        // Legenda
        xpLineChart.getLegend().setEnabled(false);

        xpLineChart.animateX(1000);
        xpLineChart.invalidate();
    }

    private void displayAvgDifficultyLineChart(List<StatisticsService.AverageDifficultyPoint> avgData) {
        if (avgData == null || avgData.isEmpty()) {
            avgDifficultyLineChart.setNoDataText("Nema podataka o težini zadataka");
            avgDifficultyLineChart.invalidate();
            return;
        }

        // Pripremi podatke za line chart
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> dateLabels = new ArrayList<>();
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());

        for (int i = 0; i < avgData.size(); i++) {
            StatisticsService.AverageDifficultyPoint dataPoint = avgData.get(i);
            entries.add(new Entry(i, (float) dataPoint.getAvgDifficulty()));

            // Formatiraj datum za prikaz
            try {
                Date date = inputFormat.parse(dataPoint.getDate());
                String formattedDate = outputFormat.format(date);
                dateLabels.add(formattedDate);
            } catch (Exception e) {
                dateLabels.add(dataPoint.getDate());
            }
        }

        // Dataset
        LineDataSet dataSet = new LineDataSet(entries, "Prosečna težina");
        dataSet.setColor(Color.parseColor("#FF5722")); // Narandžasto-crvena
        dataSet.setCircleColor(Color.parseColor("#FF5722"));
        dataSet.setCircleRadius(5f);
        dataSet.setLineWidth(3f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#FFCCBC")); // Svetlo narandžasta
        dataSet.setFillAlpha(100);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Glatke linije

        // Custom formatter za vrednosti da prikaže težinu
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                else if (value <= 2) return "VL"; // Veoma lak
                else if (value <= 5) return "L";  // Lak
                else if (value <= 13) return "T"; // Težak
                else return "ET"; // Ekstremno težak
            }
        });

        // Postavi podatke
        LineData data = new LineData(dataSet);

        avgDifficultyLineChart.setData(data);
        avgDifficultyLineChart.getDescription().setEnabled(false);

        // X-osa sa datumima
        XAxis xAxis = avgDifficultyLineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dateLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelRotationAngle(-45f);

        // Y-osa - postavi maksimum na 20 (maksimalna težina)
        avgDifficultyLineChart.getAxisLeft().setAxisMinimum(0f);
        avgDifficultyLineChart.getAxisLeft().setAxisMaximum(20f);
        avgDifficultyLineChart.getAxisLeft().setGranularity(1f);
        avgDifficultyLineChart.getAxisRight().setEnabled(false);

        // Legenda
        avgDifficultyLineChart.getLegend().setEnabled(false);

        avgDifficultyLineChart.animateX(1000);
        avgDifficultyLineChart.invalidate();
    }
}
