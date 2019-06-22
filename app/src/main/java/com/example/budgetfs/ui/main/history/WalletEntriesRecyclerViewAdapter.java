package com.example.budgetfs.ui.main.history;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.budgetfs.R;
import com.example.budgetfs.firebase.FirebaseElement;
import com.example.budgetfs.firebase.FirebaseObserver;
import com.example.budgetfs.firebase.ListDataSet;
import com.example.budgetfs.firebase.models.WalletEntry;
import com.example.budgetfs.firebase.models.User;
import com.example.budgetfs.firebase.viewmodel_factories.UserProfileViewModelFactory;
import com.example.budgetfs.firebase.viewmodel_factories.WalletEntriesHistoryViewModelFactory;
import com.example.budgetfs.models.Category;
import com.example.budgetfs.ui.main.history.edit_entry.EditWalletEntryActivity;
import com.example.budgetfs.util.CategoriesHelper;
import com.example.budgetfs.util.CurrencyHelper;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class WalletEntriesRecyclerViewAdapter extends RecyclerView.Adapter<WalletEntryHolder> {

    private static final String TAG = WalletEntriesRecyclerViewAdapter.class.getSimpleName();
    
    private final String uid;
    private final FragmentActivity fragmentActivity;
    private ListDataSet<WalletEntry> walletEntries;

    private User user;
    private boolean firstUserSync = false;

    public WalletEntriesRecyclerViewAdapter(FragmentActivity fragmentActivity, String uid) {
        this.fragmentActivity = fragmentActivity;
        this.uid = uid;

        UserProfileViewModelFactory.getModel(uid,fragmentActivity).observe(fragmentActivity, new FirebaseObserver<FirebaseElement<User>>() {
            @Override
            public void onChanged(FirebaseElement<User> element) {
                if(!element.hasNoError()) return;
                WalletEntriesRecyclerViewAdapter.this.user = element.getElement();
                if(!firstUserSync) {
                    WalletEntriesHistoryViewModelFactory.getModel(uid, fragmentActivity).observe(fragmentActivity, new FirebaseObserver<FirebaseElement<ListDataSet<WalletEntry>>>() {
                        @Override
                        public void onChanged(FirebaseElement<ListDataSet<WalletEntry>> element) {
                            if(element.hasNoError()) {
                                walletEntries = element.getElement();
                                element.getElement().notifyRecycler(WalletEntriesRecyclerViewAdapter.this);
                            }
                        }
                    });
                }
                notifyDataSetChanged();
                firstUserSync = true;
            }
        });

    }

    @Override
    public WalletEntryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(fragmentActivity);
        View view = inflater.inflate(R.layout.history_listview_row, parent, false);
        return new WalletEntryHolder(view);
    }

    @Override
    public void onBindViewHolder(WalletEntryHolder holder, int position) {
        String id = walletEntries.getIDList().get(position);
        WalletEntry walletEntry = walletEntries.getList().get(position);
        Category category = CategoriesHelper.searchCategory(user, walletEntry.categoryID);
        holder.iconImageView.setImageResource(category.getIconResourceID());
        holder.iconImageView.setBackgroundTintList(ColorStateList.valueOf(category.getIconColor()));
        holder.categoryTextView.setText(category.getCategoryVisibleName(fragmentActivity));
        holder.nameTextView.setText(walletEntry.name);

        Date date = new Date(-walletEntry.timestamp);
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        holder.dateTextView.setText(dateFormat.format(date));
        holder.moneyTextView.setText(CurrencyHelper.formatCurrency(user.currency, walletEntry.balanceDifference));
        holder.moneyTextView.setTextColor(ContextCompat.getColor(fragmentActivity,
                walletEntry.balanceDifference < 0 ? R.color.primary_text_expense : R.color.primary_text_income));

        holder.view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                createDeleteDialog(id, uid, walletEntry.balanceDifference, fragmentActivity);
                return false;
            }
        });

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(fragmentActivity, EditWalletEntryActivity.class);
                intent.putExtra("wallet-entry-id", id);
                fragmentActivity.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (walletEntries == null) return 0;
        return walletEntries.getList().size();
    }

    private void createDeleteDialog(String id, String uid, long balanceDifference, Context context) {
        new AlertDialog.Builder(context)
                .setMessage("Do you want to delete?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        FirebaseDatabase.getInstance().getReference()
                                .child("wallet-entries").child(uid).child("default").child(id).removeValue();
                        user.wallet.sum -= balanceDifference;
                        UserProfileViewModelFactory.saveModel(uid, user);
                        dialog.dismiss();
                    }

                })

                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();

                    }
                })
                .create().show();

    }

    public void setDateRange(Calendar calendarStart, Calendar calendarEnd) {
        WalletEntriesHistoryViewModelFactory.getModel(uid, fragmentActivity).setDateFilter(calendarStart, calendarEnd);
    }

}
