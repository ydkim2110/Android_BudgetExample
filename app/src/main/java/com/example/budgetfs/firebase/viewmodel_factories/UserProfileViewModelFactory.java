package com.example.budgetfs.firebase.viewmodel_factories;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

import com.example.budgetfs.firebase.models.User;
import com.example.budgetfs.firebase.viewmodels.UserProfileBaseViewModel;
import com.google.firebase.database.FirebaseDatabase;

public class UserProfileViewModelFactory implements ViewModelProvider.Factory {

    private String uid;

    public UserProfileViewModelFactory(String uid) {
        this.uid = uid;
    }

    public static void saveModel(String uid, User user) {
        FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(uid)
                .setValue(user);
    }


    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new UserProfileBaseViewModel(uid);
    }

    public static UserProfileBaseViewModel getModel(String uid, FragmentActivity activity) {
        return ViewModelProviders.of(activity, new UserProfileViewModelFactory(uid)).get(UserProfileBaseViewModel.class);
    }
}
