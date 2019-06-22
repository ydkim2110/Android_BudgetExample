package com.example.budgetfs.ui.add_entry;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.example.budgetfs.R;
import com.example.budgetfs.activities.CircularRevealActivity;
import com.example.budgetfs.exceptions.EmptyStringException;
import com.example.budgetfs.exceptions.ZeroBalanceDifferenceException;
import com.example.budgetfs.firebase.FirebaseElement;
import com.example.budgetfs.firebase.FirebaseObserver;
import com.example.budgetfs.firebase.models.WalletEntry;
import com.example.budgetfs.firebase.models.User;
import com.example.budgetfs.firebase.viewmodel_factories.UserProfileViewModelFactory;
import com.example.budgetfs.models.Category;
import com.example.budgetfs.util.CategoriesHelper;
import com.example.budgetfs.util.CurrencyHelper;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
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

        EntryTypesAdapter typeAdapter = new EntryTypesAdapter(this,
                R.layout.new_entry_type_spinner_row, Arrays.asList(
                new EntryTypeListViewModel("Expense", Color.parseColor("#ef5350"),
                        R.drawable.money_icon),
                new EntryTypeListViewModel("Income", Color.parseColor("#66bb6a"),
                        R.drawable.money_icon)));

        selectTypeSpinner.setAdapter(typeAdapter);

        updateDate();

        chooseDayTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDate();
            }
        });
        chooseTimeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickTime();
            }
        });
        
        addEntryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                  addToWallet(
                          ((selectTypeSpinner.getSelectedItemPosition()*2)-1)*CurrencyHelper.convertAmountStringToLong(selectAmountEditText.getText().toString()),
                          chosenDate.getTime(),
                          ((Category) selectCategorySpinner.getSelectedItem()).getCategoryID(),
                          selectNameEditText.getText().toString()
                  );
                } catch (EmptyStringException e) {
                    selectNameInputLayout.setError(e.getMessage());
                } catch (ZeroBalanceDifferenceException e) {
                    selectAmountInputLayout.setError(e.getMessage());
                }
            }
        });


    }

    private void addToWallet(long balanceDifference, Date entryDate, String entryCategory, String entryName)
            throws ZeroBalanceDifferenceException, EmptyStringException {
        Log.d(TAG, "addToWallet: called!!");
        if (balanceDifference == 0) {
            throw new ZeroBalanceDifferenceException("Balance difference should not be 0");
        }

        if (entryName == null || entryName.length() == 0) {
            throw new EmptyStringException("Entry length should be > 0");
        }

        FirebaseDatabase.getInstance()
                .getReference()
                .child("wallet-entries")
                .child(getUid())
                .child("default")
                .push()
                .setValue(new WalletEntry(entryCategory, entryName, entryDate.getTime(), balanceDifference));

        user.wallet.sum += balanceDifference;
        UserProfileViewModelFactory
                .saveModel(getUid(), user);

        finishWithAnimation();
    }

    private void pickDate() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                chosenDate.set(year, month, dayOfMonth);
                updateDate();
            }
        }, year, month, day).show();
    }

    private void pickTime() {
        new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                chosenDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                chosenDate.set(Calendar.MINUTE, minute);
                updateDate();
            }
        }, chosenDate.get(Calendar.HOUR_OF_DAY), chosenDate.get(Calendar.MINUTE), true).show();
    }

    private void updateDate() {
        SimpleDateFormat dataFormatter = new SimpleDateFormat("yyyy-MM-dd");
        chooseDayTextView.setText(dataFormatter.format(chosenDate.getTime()));

        SimpleDateFormat dataFormatter2 = new SimpleDateFormat("HH:mm");
        chooseTimeTextView.setText(dataFormatter2.format(chosenDate.getTime()));
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

