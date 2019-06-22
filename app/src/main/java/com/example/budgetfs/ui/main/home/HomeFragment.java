package com.example.budgetfs.ui.main.home;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.example.budgetfs.R;
import com.example.budgetfs.base.BaseFragment;
import com.example.budgetfs.firebase.FirebaseElement;
import com.example.budgetfs.firebase.FirebaseObserver;
import com.example.budgetfs.firebase.ListDataSet;
import com.example.budgetfs.firebase.models.UserSettings;
import com.example.budgetfs.firebase.models.WalletEntry;
import com.example.budgetfs.firebase.models.User;
import com.example.budgetfs.firebase.viewmodel_factories.TopWalletEntriesViewModelFactory;
import com.example.budgetfs.firebase.viewmodel_factories.UserProfileViewModelFactory;
import com.example.budgetfs.libraries.Gauge;
import com.example.budgetfs.models.Category;
import com.example.budgetfs.ui.options.OptionsActivity;
import com.example.budgetfs.util.CalendarHelper;
import com.example.budgetfs.util.CategoriesHelper;
import com.example.budgetfs.util.CurrencyHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends BaseFragment {

    private static final String TAG = HomeFragment.class.getSimpleName();

    public static final CharSequence TITLE = "Home";

    private User user;
    private ListDataSet<WalletEntry> walletEntryListDataSet;

    private ArrayList<TopCategoryListViewModel> categoryModelsHome;

    private Gauge gauge;

    private TextView totalBalanceTextView;
    private TextView gaugeLeftBalanceTextView;
    private TextView gaugeLeftLine1TextView;
    private TextView gaugeLeftLine2TextView;
    private TextView gaugeRightBalanceTextView;
    private TextView gaugeRightLine1TextView;
    private TextView gaugeRightLine2TextView;
    private TextView gaugeBalanceLeftTextView;

    private TopCategoriesAdapter adapter;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.home_fragment_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_options:
                startActivity(new Intent(getActivity(), OptionsActivity.class));
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        categoryModelsHome = new ArrayList<>();

        gauge = view.findViewById(R.id.gauge);

        totalBalanceTextView = view.findViewById(R.id.total_balance_textview);
        gaugeLeftBalanceTextView = view.findViewById(R.id.gauge_left_balance_text_view);
        gaugeLeftLine1TextView = view.findViewById(R.id.gauge_left_line1_textview);
        gaugeLeftLine2TextView = view.findViewById(R.id.gauge_left_line2_textview);
        gaugeRightBalanceTextView = view.findViewById(R.id.gauge_right_balance_text_view);
        gaugeRightLine1TextView = view.findViewById(R.id.gauge_right_line1_textview);
        gaugeRightLine2TextView = view.findViewById(R.id.gauge_right_line2_textview);
        gaugeBalanceLeftTextView = view.findViewById(R.id.left_balance_textview);

        ListView favoriteListView = view.findViewById(R.id.favourite_categories_list_view);
        adapter = new TopCategoriesAdapter(categoryModelsHome, getActivity().getApplicationContext());
        favoriteListView.setAdapter(adapter);

        TopWalletEntriesViewModelFactory.getModel(getUid(), getActivity())
                .observe(this, new FirebaseObserver<FirebaseElement<ListDataSet<WalletEntry>>>() {
                    @Override
                    public void onChanged(FirebaseElement<ListDataSet<WalletEntry>> firebaseElement) {
                        if (firebaseElement.hasNoError()) {
                            HomeFragment.this.walletEntryListDataSet = firebaseElement.getElement();
                            dataUpdated();
                        }
                    }
                });

        UserProfileViewModelFactory.getModel(getUid(), getActivity()).observe(this, new FirebaseObserver<FirebaseElement<User>>() {
            @Override
            public void onChanged(FirebaseElement<User> firebaseElement) {
                if (firebaseElement.hasNoError()) {
                    HomeFragment.this.user = firebaseElement.getElement();
                    dataUpdated();

                    Calendar startDate = CalendarHelper.getUserPeriodStartDate(user);
                    Calendar endDate = CalendarHelper.getUserPeriodEndDate(user);
                    TopWalletEntriesViewModelFactory.getModel(getUid(), getActivity()).setDateFilter(startDate, endDate);
                }
            }
        });

    }

    private void dataUpdated() {
        Log.d(TAG, "dataUpdated: called!!");
        if (user == null || walletEntryListDataSet == null) return;

        List<WalletEntry> entryList = new ArrayList<>(walletEntryListDataSet.getList());

        Calendar startDate = CalendarHelper.getUserPeriodStartDate(user);
        Calendar endDate = CalendarHelper.getUserPeriodEndDate(user);

        DateFormat dateFormat = new SimpleDateFormat("dd-MM");

        long expensesSumInDateRange = 0;
        long incomesSumInDateRange = 0;

        HashMap<Category, Long> categoryModels = new HashMap<>();

        for (WalletEntry walletEntry : entryList) {
            if (walletEntry.balanceDifference > 0) {
                incomesSumInDateRange += walletEntry.balanceDifference;
                continue;
            }
            expensesSumInDateRange += walletEntry.balanceDifference;
            Category category = CategoriesHelper.searchCategory(user, walletEntry.categoryID);
            if (categoryModels.get(category) != null)
                categoryModels.put(category, categoryModels.get(category) + walletEntry.balanceDifference);
            else
                categoryModels.put(category, walletEntry.balanceDifference);
        }

        Log.d(TAG, "dataUpdated: categoryModels size: "+categoryModels.size());

        categoryModelsHome.clear();
        for (Map.Entry<Category, Long> categoryModel : categoryModels.entrySet()) {
            categoryModelsHome.add(new TopCategoryListViewModel(categoryModel.getKey(), categoryModel.getKey().getCategoryVisibleName(getContext()),
                    user.currency, categoryModel.getValue()));
        }

        Collections.sort(categoryModelsHome, new Comparator<TopCategoryListViewModel>() {
            @Override
            public int compare(TopCategoryListViewModel o1, TopCategoryListViewModel o2) {
                return Long.compare(o1.getMoney(), o2.getMoney());
            }
        });

        adapter.notifyDataSetChanged();
        totalBalanceTextView.setText(CurrencyHelper.formatCurrency(user.currency, user.wallet.sum));

        if (user.userSettings.homeCounterType == UserSettings.HOME_COUNTER_TYPE_SHOW_LIMIT) {
            gaugeLeftBalanceTextView.setText(CurrencyHelper.formatCurrency(user.currency, 0));
            gaugeLeftLine1TextView.setText(dateFormat.format(startDate.getTime()));
            gaugeLeftLine2TextView.setVisibility(View.INVISIBLE);
            gaugeRightBalanceTextView.setText(CurrencyHelper.formatCurrency(user.currency, user.userSettings.limit));
            gaugeRightLine1TextView.setText(dateFormat.format(endDate.getTime()));
            gaugeRightLine2TextView.setVisibility(View.INVISIBLE);

            gauge.setPointStartColor(ContextCompat.getColor(getContext(), R.color.gauge_white));
            gauge.setPointEndColor(ContextCompat.getColor(getContext(), R.color.gauge_white));
            gauge.setStrokeColor(ContextCompat.getColor(getContext(), R.color.gauge_gray));

            long limit = user.userSettings.limit;
            long expenses = -expensesSumInDateRange;
            int percentage = (int) (expenses * 100 / (double) limit);
            if (percentage > 100) percentage = 100;
            gauge.setValue(percentage);
            gaugeBalanceLeftTextView.setText(CurrencyHelper.formatCurrency(user.currency, limit - expenses) + " left");


        } else {
            gaugeLeftBalanceTextView.setText(CurrencyHelper.formatCurrency(user.currency, incomesSumInDateRange));
            gaugeLeftLine1TextView.setText("Incomes");
            gaugeLeftLine2TextView.setVisibility(View.INVISIBLE);
            gaugeRightBalanceTextView.setText(CurrencyHelper.formatCurrency(user.currency, expensesSumInDateRange));
            gaugeRightLine1TextView.setText("Expenses");
            gaugeRightLine2TextView.setVisibility(View.INVISIBLE);

            gauge.setPointStartColor(ContextCompat.getColor(getContext(), R.color.gauge_income));
            gauge.setPointEndColor(ContextCompat.getColor(getContext(), R.color.gauge_income));
            gauge.setStrokeColor(ContextCompat.getColor(getContext(), R.color.gauge_expense));
            if (incomesSumInDateRange - expensesSumInDateRange != 0)
                gauge.setValue((int) (incomesSumInDateRange * 100 / (double) (incomesSumInDateRange - expensesSumInDateRange)));

            gaugeBalanceLeftTextView.setText(dateFormat.format(startDate.getTime()) + " - " +
                    dateFormat.format(endDate.getTime()));
        }
    }
}
