package com.example.budgetfs.firebase.viewmodels;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.Nullable;

import com.example.budgetfs.firebase.FirebaseElement;
import com.example.budgetfs.firebase.FirebaseObserver;
import com.example.budgetfs.firebase.FirebaseQueryLiveDataElement;
import com.example.budgetfs.firebase.models.User;
import com.google.firebase.database.FirebaseDatabase;

public class UserProfileBaseViewModel extends ViewModel {

    private final FirebaseQueryLiveDataElement<User> liveData;

    public UserProfileBaseViewModel(String uid) {
        liveData = new FirebaseQueryLiveDataElement<>(User.class, FirebaseDatabase.getInstance().getReference()
        .child("users").child(uid));
    }

    public void observe(LifecycleOwner owner, FirebaseObserver<FirebaseElement<User>> observer) {
        if (liveData.getValue() != null) {
            observer.onChanged(liveData.getValue());
        }
        liveData.observe(owner, new Observer<FirebaseElement<User>>() {
            @Override
            public void onChanged(@Nullable FirebaseElement<User> firebaseElement) {
                if (firebaseElement != null) {
                    observer.onChanged(firebaseElement);
                }
            }
        });
    }

}
