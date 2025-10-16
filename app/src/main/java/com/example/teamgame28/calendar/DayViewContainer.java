package com.example.teamgame28.calendar;

import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.teamgame28.R;
import com.kizitonwose.calendar.view.ViewContainer;
import com.kizitonwose.calendar.core.CalendarDay;

import java.util.List;

public class DayViewContainer extends ViewContainer{
    public final TextView textView;
    public final LinearLayout colorIndicatorsContainer;
    public CalendarDay day;

    public DayViewContainer(@NonNull View view) {
        super(view);
        textView = view.findViewById(R.id.dayText);
        colorIndicatorsContainer = view.findViewById(R.id.colorIndicatorsContainer);

        // 🔹 Klik na dan u kalendaru
        view.setOnClickListener(v -> {
            if (day != null && clickListener != null) {
                clickListener.onDateClicked(day);
            }
        });
    }

    /**
     * Prikazuje boje kategorija za zadatke na ovom danu
     * @param colors Lista hex string boja (npr. "#4CAF50")
     */
    public void setColorIndicators(List<String> colors) {
        colorIndicatorsContainer.removeAllViews();

        if (colors == null || colors.isEmpty()) {
            return;
        }

        // Prikaži do 3 boje maksimalno da ne bude prenatrpano
        int maxColors = Math.min(colors.size(), 3);

        for (int i = 0; i < maxColors; i++) {
            View colorDot = new View(colorIndicatorsContainer.getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(6, 6);
            params.setMargins(1, 0, 1, 0);
            colorDot.setLayoutParams(params);

            try {
                colorDot.setBackgroundColor(Color.parseColor(colors.get(i)));
            } catch (Exception e) {
                colorDot.setBackgroundColor(Color.GRAY);
            }

            colorIndicatorsContainer.addView(colorDot);
        }
    }

    // Interfejs za klik događaj
    public interface DayClickListener {
        void onDateClicked(CalendarDay day);
    }

    private static DayClickListener clickListener;

    public static void setDayClickListener(DayClickListener listener) {
        clickListener = listener;
    }
}
