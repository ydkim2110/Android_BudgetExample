package com.example.budgetfs.firebase.viewmodels;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.Nullable;

import com.example.budgetfs.firebase.FirebaseElement;
import com.example.budgetfs.firebase.FirebaseObserver;
import com.example.budgetfs.firebase.FirebaseQueryLiveDataElement;
import com.example.budgetfs.firebase.models.WalletEntry;
import com.google.firebase.database.FirebaseDatabase;

public class WalletEntryBaseViewModel extends ViewModel {

    protected final FirebaseQueryLiveDataElement<WalletEntry> liveData;
    protected final String uid;

    public WalletEntryBaseViewModel(String uid, String walletEntryId) {
        this.uid=uid;
        liveData = new FirebaseQueryLiveDataElement<>(WalletEntry.class, FirebaseDatabase.getInstance().getReference()
                .child("wallet-entries").child(uid).child("default").child(walletEntryId));    }

    public void observe(LifecycleOwner owner, FirebaseObserver<FirebaseElement<WalletEntry>> observer) {
        if(liveData.getValue() != null) observer.onChanged(liveData.getValue());
        liveData.observe(owner, new Observer<FirebaseElement<WalletEntry>>() {
            @Override
            public void onChanged(@Nullable FirebaseElement<WalletEntry> element) {
                if(element != null) observer.onChanged(element);
            }
        });
    }

    public void removeObserver(Observer<FirebaseElement<WalletEntry>> observer) {
        liveData.removeObserver(observer);
    }

}
