package com.example.teamgame28.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.teamgame28.model.Task;
import com.example.teamgame28.model.TaskStatus;
import com.example.teamgame28.repository.TaskRepository;
import com.example.teamgame28.service.TaskService;

import java.util.List;

public class TaskViewModel extends AndroidViewModel {

    private final TaskService taskService;

    public TaskViewModel(@NonNull Application application) {
        super(application);
        this.taskService = new TaskService(application);

    }

    public LiveData<List<Task>> getTasksByUser(String userId) {
        return taskService.getTasksByUser(userId);
    }

    public void addTask(Task task) {
        taskService.addTask(task);
    }

    public void markTaskDone(Task task) {
        task.setStatus(TaskStatus.FINISHED);
        taskService.updateTask(task);
    }

    public void deleteTask(String taskId) {
        taskService.deleteTask(taskId);
    }
}
