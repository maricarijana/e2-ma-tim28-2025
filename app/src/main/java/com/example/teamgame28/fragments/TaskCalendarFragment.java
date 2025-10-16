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
import com.example.teamgame28.model.TaskCategory;
import com.example.teamgame28.viewmodels.CategoryViewModel;
import com.example.teamgame28.viewmodels.TaskViewModel;
import com.google.firebase.auth.FirebaseAuth;
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
 private CategoryViewModel categoryViewModel;
 private TaskAdapter taskAdapter;
 private final List<Task> tasksForDay = new ArrayList<>();
 private List<Task> allTasks = new ArrayList<>();
 private List<TaskCategory> allCategories = new ArrayList<>();

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

    // 🎨 Prikaz boja kategorija za ovaj dan
    List<String> colorsForDay = getColorsForDate(day.getDate());
    container.setColorIndicators(colorsForDay);
   }
  });

  monthTitle = view.findViewById(R.id.textCurrentMonth);
  prevBtn = view.findViewById(R.id.btnPreviousMonth);
  nextBtn = view.findViewById(R.id.btnNextMonth);
  textSelectedDateTasks = view.findViewById(R.id.textSelectedDateTasks);
  listViewTasks = view.findViewById(R.id.listViewTasksForDate);

  // 🔹 ViewModel
  taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
  categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

  // 🔹 Učitaj kategorije
  String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
          ? FirebaseAuth.getInstance().getCurrentUser().getUid()
          : "12345";

  categoryViewModel.getCategoriesByUser(currentUserId).observe(getViewLifecycleOwner(), categories -> {
   allCategories = categories != null ? categories : new ArrayList<>();
   calendarView.notifyCalendarChanged();
  });

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
  observeTasksForUser(currentUserId);
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
  taskViewModel.getTasksByUser(userId).observe(getViewLifecycleOwner(), tasks -> {
   allTasks = tasks != null ? tasks : new ArrayList<>();
   tasksForDay.clear();

   for (Task t : allTasks) {
    if (t.getStartDate() != null && isSameDay(t.getStartDate(), selectedDate)) {
     tasksForDay.add(t);
    } else if (t.isRecurring() && t.getRecurringDates() != null) {
     for (Long ts : t.getRecurringDates()) {
      Date recurringDate = new Date(ts);
      if (isSameDay(recurringDate, selectedDate)) {
       tasksForDay.add(t);
       break;
      }
     }
    }
   }

   taskAdapter.setTasks(tasksForDay);
   calendarView.notifyCalendarChanged();

   if (tasksForDay.isEmpty()) {
    Toast.makeText(requireContext(),
            "Nema zadataka za ovaj dan.",
            Toast.LENGTH_SHORT).show();
   }
  });
 }

 private void updateTaskListForSelectedDate() {
  String userId = FirebaseAuth.getInstance().getCurrentUser() != null
          ? FirebaseAuth.getInstance().getCurrentUser().getUid()
          : "12345";

  taskViewModel.getTasksByUser(userId).observe(getViewLifecycleOwner(), tasks -> {
   allTasks = tasks != null ? tasks : new ArrayList<>();
   tasksForDay.clear();

   for (Task t : allTasks) {
    if (t.getStartDate() != null && isSameDay(t.getStartDate(), selectedDate)) {
     tasksForDay.add(t);
    } else if (t.isRecurring() && t.getRecurringDates() != null) {
     for (Long ts : t.getRecurringDates()) {
      Date recurringDate = new Date(ts);
      if (isSameDay(recurringDate, selectedDate)) {
       tasksForDay.add(t);
       break;
      }
     }
    }
   }

   taskAdapter.setTasks(tasksForDay);
   calendarView.notifyCalendarChanged();
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


 /**
  * Vraća listu boja kategorija za zadatke na određeni datum
  * @param localDate Datum za koji se traže boje
  * @return Lista hex string boja
  */
 private List<String> getColorsForDate(java.time.LocalDate localDate) {
  List<String> colors = new ArrayList<>();

  // Konvertuj LocalDate u Date za poređenje
  Date date = Date.from(localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());

  // Pronađi sve zadatke za ovaj dan
  for (Task task : allTasks) {
   if (task.getStartDate() != null && isSameDay(task.getStartDate(), date)) {
    // Pronađi kategoriju ovog zadatka
    String categoryId = task.getCategoryId();
    if (categoryId != null) {
     for (TaskCategory category : allCategories) {
      if (category.getId() != null && category.getId().equals(categoryId)) {
       String color = category.getColor();
       // Dodaj boju samo ako već nije u listi (izbjegni duplikate)
       if (color != null && !colors.contains(color)) {
        colors.add(color);
       }
       break;
      }
     }
    }
   }
  }

  return colors;
 }

}
