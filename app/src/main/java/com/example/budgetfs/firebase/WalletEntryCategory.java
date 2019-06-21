package com.example.budgetfs.firebase;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class WalletEntryCategory {
    public String htmlColorCode;
    public String visibleName;

    public WalletEntryCategory() {
    }

    public WalletEntryCategory(String visibleName, String htmlColorCode) {
        this.htmlColorCode = htmlColorCode;
        this.visibleName = visibleName;
    }
}
