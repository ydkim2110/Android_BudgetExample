package com.example.budgetfs.util;

import android.graphics.Color;

import com.example.budgetfs.R;
import com.example.budgetfs.firebase.models.User;
import com.example.budgetfs.firebase.models.WalletEntryCategory;
import com.example.budgetfs.models.Category;
import com.example.budgetfs.models.DefaultCategories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CategoriesHelper {

    public static Category searchCategory(User user, String categoryName) {
        for(Category category : DefaultCategories.getDefaultCategories()) {
            if(category.getCategoryID().equals(categoryName)) return category;
        }
        for(Map.Entry<String, WalletEntryCategory> entry : user.customCategories.entrySet()) {
            if(entry.getKey().equals(categoryName)) {
                return new Category(categoryName, entry.getValue().visibleName, R.drawable.category_default, Color.parseColor(entry.getValue().htmlColorCode));
            }
        }
        return DefaultCategories.createDefaultCategoryModel("Others");
    }

    public static List<Category> getCategories(User user) {
        List<Category> categories = new ArrayList<>();

        categories.addAll(Arrays.asList(DefaultCategories.getDefaultCategories()));
        categories.addAll(getCustomCategories(user));
        return categories;
    }

    public static List<Category> getCustomCategories(User user) {
        ArrayList<Category> categories = new ArrayList<>();
        for (Map.Entry<String, WalletEntryCategory> entry : user.customCategories.entrySet()) {
            String categoryName = entry.getKey();
            categories.add(new Category(categoryName, entry.getValue().visibleName, R.drawable.category_default, Color.parseColor(entry.getValue().htmlColorCode)));
        }

        return categories;
    }

}
