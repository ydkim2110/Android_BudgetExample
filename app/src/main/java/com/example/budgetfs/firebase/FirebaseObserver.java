package com.example.budgetfs.firebase;

public interface FirebaseObserver<T> {
    void onChanged(T t);
}
