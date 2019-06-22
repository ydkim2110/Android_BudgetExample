package com.example.budgetfs.ui.main.history;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.budgetfs.R;
import com.example.budgetfs.base.BaseFragment;
import com.example.budgetfs.firebase.viewmodel_factories.WalletEntriesHistoryViewModelFactory;
import com.example.budgetfs.ui.options.OptionsActivity;
import com.leavjenn.smoothdaterangepicker.date.SmoothDateRangePickerFragment;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;


/**
 * A simple {@link Fragment} subclass.
 */
public class HistoryFragment extends BaseFragment {

    public static final CharSequence TITLE = "History";

    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    private Calendar calendarStart;
    private Calendar calendarEnd;
    private RecyclerView historyRecyclerView;
    private WalletEntriesRecyclerViewAdapter historyRecyclerViewAdapter;
    private Menu menu;
    private TextView dividerTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        dividerTextView = view.findViewById(R.id.divider_textview);
        dividerTextView.setText("Last 100 elements:");
        historyRecyclerView = view.findViewById(R.id.history_recycler_view);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        historyRecyclerViewAdapter = new WalletEntriesRecyclerViewAdapter(getActivity(), getUid());
        historyRecyclerView.setAdapter(historyRecyclerViewAdapter);

        historyRecyclerViewAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                historyRecyclerView.smoothScrollToPosition(0);
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.history_fragment_menu, menu);
        this.menu = menu;
        updateCalendarIcon();
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void updateCalendarIcon() {
        MenuItem calendarIcon = menu.findItem(R.id.action_date_range);
        if (calendarIcon == null) return;
        WalletEntriesHistoryViewModelFactory.Model model = WalletEntriesHistoryViewModelFactory.getModel(getUid(), getActivity());
        if (model.hasDateSet()) {
            calendarIcon.setIcon(ContextCompat.getDrawable(getContext(), R.drawable.icon_calendar_active));

            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yy");

            dividerTextView.setText("Date range: " + dateFormat.format(model.getStartDate().getTime())
                    + "  -  " + dateFormat.format(model.getEndDate().getTime()));
        } else {
            calendarIcon.setIcon(ContextCompat.getDrawable(getContext(), R.drawable.icon_calendar));

            dividerTextView.setText("Last 100 elements:");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_date_range:
                showSelectDateRangeDialog();
                return true;
            case R.id.action_options:
                startActivity(new Intent(getActivity(), OptionsActivity.class));
                return true;
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
                updateCalendarIcon();
            }
        });
        datePicker.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                calendarStart = null;
                calendarEnd = null;
                calendarUpdated();
                updateCalendarIcon();
            }
        });
        datePicker.show(getActivity().getFragmentManager(), "TAG");
        //todo library doesn't respect other method than deprecated
    }

    private void calendarUpdated() {
        historyRecyclerViewAdapter.setDateRange(calendarStart, calendarEnd);
    }


}
