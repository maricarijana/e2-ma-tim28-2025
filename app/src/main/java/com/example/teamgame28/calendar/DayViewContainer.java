package com.example.teamgame28.calendar;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.teamgame28.R;
import com.kizitonwose.calendar.view.ViewContainer;
import com.kizitonwose.calendar.core.CalendarDay;
public class DayViewContainer extends ViewContainer{
    public final TextView textView;
    public CalendarDay day;

    public DayViewContainer(@NonNull View view) {
        super(view);
        textView = view.findViewById(R.id.dayText);

        // 🔹 Klik na dan u kalendaru
        view.setOnClickListener(v -> {
            if (day != null && clickListener != null) {
                clickListener.onDateClicked(day);
            }
        });
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
