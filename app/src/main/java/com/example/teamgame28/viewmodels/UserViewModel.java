package com.example.teamgame28.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.teamgame28.service.UserService;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

public class UserViewModel extends AndroidViewModel {

    private final UserService userService;

    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>();

    public UserViewModel(@NonNull Application application) {
        super(application);
        userService = new UserService();
        loading.setValue(false);
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void registerUser(String email, String password, String confirmPassword, String username, String avatarName) {
        loading.setValue(true);

        userService.registerUser(email, password, confirmPassword, username, avatarName)
                .addOnSuccessListener(result -> {
                    message.setValue(result);        // uspešno registrovan
                    loading.setValue(false);
                })
                .addOnFailureListener(error -> {
                    message.setValue(error.getMessage()); // prikazuje poruku greške
                    loading.setValue(false);
                });
    }
    public Task<String> loginUser(String email, String password) {
        loading.setValue(true);
        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        userService.loginUser(email, password)
                .addOnSuccessListener(result -> {
                    message.setValue(result);
                    loading.setValue(false);
                    tcs.setResult(result);
                })
                .addOnFailureListener(error -> {
                    message.setValue(error.getMessage());
                    loading.setValue(false);
                    tcs.setException(error);
                });

        return tcs.getTask();
    }


}
