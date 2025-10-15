package com.example.teamgame28.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.graphics.Color;
import com.example.teamgame28.R;
import com.example.teamgame28.adapters.TaskAdapter;
import com.example.teamgame28.calendar.DayViewContainer;
import com.example.teamgame28.model.Task;
import com.example.teamgame28.viewmodels.TaskViewModel;
import com.kizitonwose.calendar.view.CalendarView;

import java.text.SimpleDateFormat;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragment koji prikazuje mesečni kalendar i zadatke za izabrani dan.
 * (Jednostavna verzija bez DayBinder-a)
 */
public class TaskCalendarFragment extends Fragment {

 private CalendarView calendarView;
 private TextView monthTitle, textSelectedDateTasks;
 private ImageView prevBtn, nextBtn;
 private ListView listViewTasks;

 private TaskViewModel taskViewModel;
 private TaskAdapter taskAdapter;
 private final List<Task> tasksForDay = new ArrayList<>();

 private Date selectedDate = new Date(); // današnji datum

 @Nullable
 @Override
 public View onCreateView(@NonNull LayoutInflater inflater,
                          @Nullable ViewGroup container,
                          @Nullable Bundle savedInstanceState) {
  return inflater.inflate(R.layout.fragment_task_calendar, container, false);
 }

 @Override
 public void onViewCreated(@NonNull View view,
                           @Nullable Bundle savedInstanceState) {
  super.onViewCreated(view, savedInstanceState);

  // 🔹 Inicijalizacija UI elemenata
  calendarView = view.findViewById(R.id.calendarView);
  calendarView.setDayBinder(new com.kizitonwose.calendar.view.MonthDayBinder<com.example.teamgame28.calendar.DayViewContainer>() {
   @NonNull
   @Override
   public com.example.teamgame28.calendar.DayViewContainer create(@NonNull View view) {
    return new com.example.teamgame28.calendar.DayViewContainer(view);
   }

   @Override
   public void bind(@NonNull com.example.teamgame28.calendar.DayViewContainer container,
                    @NonNull com.kizitonwose.calendar.core.CalendarDay day) {
    container.textView.setText(String.valueOf(day.getDate().getDayOfMonth()));
    container.day = day;
    // Oboj današnji datum
    if (day.getDate().equals(java.time.LocalDate.now())) {
     container.textView.setTextColor(Color.parseColor("#FCA103"));
    } else {
     container.textView.setTextColor(Color.WHITE);
    }
    if (day.getDate().equals(selectedDate.toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate())) {
     container.textView.setBackgroundColor(Color.parseColor("#4CAF50")); // zelena za selektovani
    } else {
     container.textView.setBackgroundColor(Color.TRANSPARENT);
    }
   }
  });

  monthTitle = view.findViewById(R.id.textCurrentMonth);
  prevBtn = view.findViewById(R.id.btnPreviousMonth);
  nextBtn = view.findViewById(R.id.btnNextMonth);
  textSelectedDateTasks = view.findViewById(R.id.textSelectedDateTasks);
  listViewTasks = view.findViewById(R.id.listViewTasksForDate);

  // 🔹 ViewModel
  taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);

  // 🔹 Adapter
  taskAdapter = new TaskAdapter(requireContext(), tasksForDay, (task, v) -> {
   // 👉 Otvori TaskDetailFragment kada se klikne zadatak
   Fragment detailFragment = TaskDetailFragment.newInstance(task.getId());
   requireActivity().getSupportFragmentManager()
           .beginTransaction()
           .replace(R.id.fragment_container, detailFragment)
           .addToBackStack(null)
           .commit();
  }, taskViewModel);

  listViewTasks.setAdapter(taskAdapter);

  // 🔹 Kalendar - prikaz trenutnog meseca
  setupCalendar();

  // 🔹 Klik na dan u kalendaru
  DayViewContainer.setDayClickListener(day -> {
   selectedDate = Date.from(day.getDate()
           .atStartOfDay(java.time.ZoneId.systemDefault())
           .toInstant());

   updateDateHeader(selectedDate);
   updateTaskListForSelectedDate();

   calendarView.notifyCalendarChanged();
  });


  // 🔹 Zadaci za današnji dan
  updateDateHeader(selectedDate);
  observeTasksForUser("12345");
 }

 private void setupCalendar() {
  YearMonth currentMonth = YearMonth.now();
  calendarView.setup(
          currentMonth.minusMonths(12),
          currentMonth.plusMonths(12),
          java.time.DayOfWeek.MONDAY
  );
  calendarView.scrollToMonth(currentMonth);

  updateMonthTitle(currentMonth);

  // 🔸 Reaguje na promenu meseca (skrolovanje)
  calendarView.setMonthScrollListener(calendarMonth -> {
   updateMonthTitle(calendarMonth.getYearMonth());
   return kotlin.Unit.INSTANCE;
  });

  // 🔸 Strelice levo/desno
  prevBtn.setOnClickListener(v -> {
   if (calendarView.findFirstVisibleMonth() != null) {
    YearMonth current = calendarView.findFirstVisibleMonth().getYearMonth();
    calendarView.smoothScrollToMonth(current.minusMonths(1));
   }
  });

  nextBtn.setOnClickListener(v -> {
   if (calendarView.findFirstVisibleMonth() != null) {
    YearMonth current = calendarView.findFirstVisibleMonth().getYearMonth();
    calendarView.smoothScrollToMonth(current.plusMonths(1));
   }
  });
 }

 private void updateMonthTitle(YearMonth month) {
  String monthName = month.getMonth()
          .getDisplayName(TextStyle.FULL, Locale.getDefault());
  monthTitle.setText(monthName.toUpperCase() + " " + month.getYear());
 }

 private void observeTasksForUser(String userId) {
  taskViewModel.getTasksByUser(userId).observe(getViewLifecycleOwner(), allTasks -> {
   tasksForDay.clear();

   if (allTasks != null) {
    for (Task t : allTasks) {
     if (t.getStartDate() != null && isSameDay(t.getStartDate(), selectedDate)) {
      tasksForDay.add(t);
     }
    }
   }

   if (tasksForDay.isEmpty()) {
    Toast.makeText(requireContext(),
            "Nema zadataka za ovaj dan.",
            Toast.LENGTH_SHORT).show();
   }

   taskAdapter.setTasks(tasksForDay);
  });
 }

 private void updateDateHeader(Date date) {
  SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
  textSelectedDateTasks.setText("Zadaci za " + sdf.format(date) + ":");
 }

 private boolean isSameDay(Date d1, Date d2) {
  if (d1 == null || d2 == null) return false;
  Calendar c1 = Calendar.getInstance();
  Calendar c2 = Calendar.getInstance();
  c1.setTime(d1);
  c2.setTime(d2);
  return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
          && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
 }

 private void updateTaskListForSelectedDate() {
  taskViewModel.getTasksByUser("12345").observe(getViewLifecycleOwner(), allTasks -> {
   tasksForDay.clear();

   if (allTasks != null) {
    for (Task t : allTasks) {
     if (t.getStartDate() != null && isSameDay(t.getStartDate(), selectedDate)) {
      tasksForDay.add(t);
     }
    }
   }

   taskAdapter.setTasks(tasksForDay);
  });
 }

}
