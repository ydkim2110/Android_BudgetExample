package com.example.budgetfs.ui.main.statistics;


import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.budgetfs.R;
import com.example.budgetfs.base.BaseFragment;
import com.example.budgetfs.firebase.FirebaseElement;
import com.example.budgetfs.firebase.FirebaseObserver;
import com.example.budgetfs.firebase.ListDataSet;
import com.example.budgetfs.firebase.models.User;
import com.example.budgetfs.firebase.models.WalletEntry;
import com.example.budgetfs.firebase.viewmodel_factories.TopWalletEntriesStatisticsViewModelFactory;
import com.example.budgetfs.firebase.viewmodel_factories.UserProfileViewModelFactory;
import com.example.budgetfs.models.Category;
import com.example.budgetfs.ui.options.OptionsActivity;
import com.example.budgetfs.util.CalendarHelper;
import com.example.budgetfs.util.CategoriesHelper;
import com.example.budgetfs.util.CurrencyHelper;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.leavjenn.smoothdaterangepicker.date.SmoothDateRangePickerFragment;

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
public class StatisticsFragment extends BaseFragment {

    private static final String TAG = StatisticsFragment.class.getSimpleName();

    public static final CharSequence TITLE = "Statistics";
    private Menu menu;
    private Calendar calendarStart;
    private Calendar calendarEnd;
    private User user;
    private ListDataSet<WalletEntry> walletEntryListDataSet;
    private PieChart pieChart;
    private ArrayList<TopCategoryStatisticsListViewModel> categoryModelsHome;
    private TopCategoriesStatisticsAdapter adapter;
    private TextView dividerTextView;
    private ProgressBar incomesExpensesProgressBar;
    private TextView incomesTextView;
    private TextView expensesTextView;


    public static StatisticsFragment newInstance() {
        return new StatisticsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        pieChart = view.findViewById(R.id.pie_chart);
        dividerTextView = view.findViewById(R.id.divider_textview);
        View incomesExpensesView = view.findViewById(R.id.incomes_expenses_view);
        incomesExpensesProgressBar = incomesExpensesView.findViewById(R.id.progress_bar);
        expensesTextView = incomesExpensesView.findViewById(R.id.expenses_textview);
        incomesTextView = incomesExpensesView.findViewById(R.id.incomes_textview);

        categoryModelsHome = new ArrayList<>();
        ListView favoriteListView = view.findViewById(R.id.favourite_categories_list_view);
        adapter = new TopCategoriesStatisticsAdapter(categoryModelsHome, getActivity().getApplicationContext());
        favoriteListView.setAdapter(adapter);

        TopWalletEntriesStatisticsViewModelFactory.getModel(getUid(), getActivity())
                .observe(this, new FirebaseObserver<FirebaseElement<ListDataSet<WalletEntry>>>() {
                    @Override
                    public void onChanged(FirebaseElement<ListDataSet<WalletEntry>> firebaseElement) {
                        if (firebaseElement.hasNoError()) {
                            StatisticsFragment.this.walletEntryListDataSet = firebaseElement.getElement();
                            dataUpdated();
                        }
                    }
                });

        UserProfileViewModelFactory.getModel(getUid(), getActivity())
                .observe(this, new FirebaseObserver<FirebaseElement<User>>() {
                    @Override
                    public void onChanged(FirebaseElement<User> firebaseElement) {
                        if (firebaseElement.hasNoError()) {
                            StatisticsFragment.this.user = firebaseElement.getElement();

                            calendarStart = CalendarHelper.getUserPeriodStartDate(user);
                            calendarEnd = CalendarHelper.getUserPeriodEndDate(user);

                            updateCalendarIcon(false);
                            calendarUpdated();
                            dataUpdated();
                        }
                    }
                });
    }

    private void dataUpdated() {
        if (calendarStart != null && calendarEnd != null && walletEntryListDataSet != null) {
            List<WalletEntry> entryList = new ArrayList<>(walletEntryListDataSet.getList());

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

            categoryModelsHome.clear();

            ArrayList<PieEntry> pieEntries = new ArrayList<>();
            ArrayList<Integer> pieColors = new ArrayList<>();

            for (Map.Entry<Category, Long> categoryModel : categoryModels.entrySet()) {
                float percentage = categoryModel.getValue() / (float) expensesSumInDateRange;
                final float minPercentageToShowLabelOnChart = 0.1f;
                categoryModelsHome.add(new TopCategoryStatisticsListViewModel(categoryModel.getKey(), categoryModel.getKey().getCategoryVisibleName(getContext()),
                        user.currency, categoryModel.getValue(), percentage));
                if (percentage > minPercentageToShowLabelOnChart) {
                    Drawable drawable = getContext().getDrawable(categoryModel.getKey().getIconResourceID());
                    drawable.setTint(Color.parseColor("#FFFFFF"));
                    pieEntries.add(new PieEntry(-categoryModel.getValue(), drawable));

                } else {
                    pieEntries.add(new PieEntry(-categoryModel.getValue()));
                }
                pieColors.add(categoryModel.getKey().getIconColor());
            }

            PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
            pieDataSet.setDrawValues(false);
            pieDataSet.setColors(pieColors);
            pieDataSet.setSliceSpace(2f);

            PieData data = new PieData(pieDataSet);
            pieChart.setData(data);
            pieChart.setTouchEnabled(false);
            pieChart.getLegend().setEnabled(false);
            pieChart.getDescription().setEnabled(false);

            pieChart.setDrawHoleEnabled(true);
            pieChart.setHoleColor(ContextCompat.getColor(getContext(), R.color.backgroundPrimary));
            pieChart.setHoleRadius(55f);
            pieChart.setTransparentCircleRadius(55f);
            pieChart.setDrawCenterText(true);
            pieChart.setRotationAngle(270);
            pieChart.setRotationEnabled(false);
            pieChart.setHighlightPerTapEnabled(true);

            pieChart.invalidate();

            Collections.sort(categoryModelsHome, new Comparator<TopCategoryStatisticsListViewModel>() {
                @Override
                public int compare(TopCategoryStatisticsListViewModel o1, TopCategoryStatisticsListViewModel o2) {
                    return Long.compare(o1.getMoney(), o2.getMoney());
                }
            });


            adapter.notifyDataSetChanged();

            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yy");

            dividerTextView.setText("Date range: " + dateFormat.format(calendarStart.getTime())
                    + "  -  " + dateFormat.format(calendarEnd.getTime()));

            expensesTextView.setText(CurrencyHelper.formatCurrency(user.currency, expensesSumInDateRange));
            incomesTextView.setText(CurrencyHelper.formatCurrency(user.currency, incomesSumInDateRange));

            float progress = 100 * incomesSumInDateRange / (float) (incomesSumInDateRange - expensesSumInDateRange);
            incomesExpensesProgressBar.setProgress((int) progress);

        }
    }

    private void calendarUpdated() {
        TopWalletEntriesStatisticsViewModelFactory.getModel(getUid(), getActivity()).setDateFilter(calendarStart, calendarEnd);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.statistics_fragment_menu, menu);
        this.menu = menu;
        updateCalendarIcon(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_date_range:
                showSelectDateRangeDialog();
                return true;
            case R.id.action_options:
                startActivity(new Intent(getActivity(), OptionsActivity.class));
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showSelectDateRangeDialog() {
        SmoothDateRangePickerFragment datePicker = SmoothDateRangePickerFragment.newInstance(new SmoothDateRangePickerFragment.OnDateRangeSetListener() {
            @Override
            public void onDateRangeSet(SmoothDateRangePickerFragment view, int yearStart, int monthStart, int dayStart, int yearEnd, int monthEnd, int dayEnd) {
                calendarStart = Calendar.getInstance();
                calendarStart.set(yearStart, monthStart, dayStart);
                calendarStart.set(Calendar.HOUR_OF_DAY, 0);
                calendarStart.set(Calendar.MINUTE, 0);
                calendarStart.set(Calendar.SECOND, 0);

                calendarEnd = Calendar.getInstance();
                calendarEnd.set(yearEnd, monthEnd, dayEnd);
                calendarEnd.set(Calendar.HOUR_OF_DAY, 23);
                calendarEnd.set(Calendar.MINUTE, 59);
                calendarEnd.set(Calendar.SECOND, 59);
                calendarUpdated();
                updateCalendarIcon(true);
            }
        });
        datePicker.show(getActivity().getFragmentManager(), "TAG");
        //todo library doesn't respect other method than deprecated
    }

    private void updateCalendarIcon(boolean updatedFromUI) {
        if (menu == null) return;
        MenuItem calendarIcon = menu.findItem(R.id.action_date_range);
        if (calendarIcon == null) return;
        if (updatedFromUI) {
            calendarIcon.setIcon(ContextCompat.getDrawable(getContext(), R.drawable.icon_calendar_active));
        } else {
            calendarIcon.setIcon(ContextCompat.getDrawable(getContext(), R.drawable.icon_calendar));
        }
    }
}
