package com.example.budgetfs;

import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.budgetfs.activities.CircularRevealActivity;
import com.example.budgetfs.firebase.FirebaseElement;
import com.example.budgetfs.firebase.FirebaseObserver;
import com.example.budgetfs.firebase.models.User;
import com.example.budgetfs.firebase.viewmodel_factories.UserProfileViewModelFactory;
import com.example.budgetfs.models.Category;
import com.example.budgetfs.ui.add_entry.EntryCategoriesAdapter;
import com.example.budgetfs.util.CategoriesHelper;
import com.example.budgetfs.util.CurrencyHelper;

import java.util.Calendar;
import java.util.List;

public class AddWalletEntryActivity extends CircularRevealActivity {

    private static final String TAG = AddWalletEntryActivity.class.getSimpleName();

    private Spinner selectCategorySpinner;
    private TextInputEditText selectNameEditText;
    private Calendar chosenDate;
    private TextInputEditText selectAmountEditText;
    private TextView chooseDayTextView;
    private TextView chooseTimeTextView;
    private Spinner selectTypeSpinner;
    private User user;
    private TextInputLayout selectAmountInputLayout;
    private TextInputLayout selectNameInputLayout;

    public AddWalletEntryActivity() {
        super(R.layout.activity_add_wallet_entry, R.id.activity_contact_fab, R.id.root_layout, R.id.root_layout2);
    }

    @Override
    public void onInitialized(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: started!!");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Add Wallet entry");

        selectCategorySpinner = findViewById(R.id.select_category_spinner);
        selectNameEditText = findViewById(R.id.select_name_edittext);
        selectNameInputLayout = findViewById(R.id.select_name_inputlayout);
        selectTypeSpinner = findViewById(R.id.select_type_spinner);
        Button addEntryButton = findViewById(R.id.add_entry_button);
        chooseTimeTextView = findViewById(R.id.choose_time_textview);
        chooseDayTextView = findViewById(R.id.choose_day_textview);
        selectAmountEditText = findViewById(R.id.select_amount_edittext);
        selectAmountInputLayout = findViewById(R.id.select_amount_inputlayout);
        chosenDate = Calendar.getInstance();

        UserProfileViewModelFactory.getModel(getUid(), this)
                .observe(this, new FirebaseObserver<FirebaseElement<User>>() {
                    @Override
                    public void onChanged(FirebaseElement<User> firebaseElement) {
                        if (firebaseElement.hasNoError()) {
                            user = firebaseElement.getElement();
                            dateUpdated();
                        }
                    }
                });
    }

    private void dateUpdated() {
        Log.d(TAG, "dateUpdated: called!!");
        if (user == null) return;

        final List<Category> categories = CategoriesHelper.getCategories(user);

        EntryCategoriesAdapter categoriesAdapter = new EntryCategoriesAdapter(this,
                R.layout.new_entry_type_spinner_row, categories);
        categoriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectCategorySpinner.setAdapter(categoriesAdapter);

        CurrencyHelper.setupAmountEditText(selectAmountEditText, user);
    }


}

