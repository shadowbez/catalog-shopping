package com.example.catalogshopping.other;

import android.content.Context;
import android.widget.Toast;

import com.example.catalogshopping.model.ProductFirestore;

public class Util {

    public static ProductFirestore dummyProductFirestore(String id) {
        return new ProductFirestore(id, "Product was not found in dummy database.", id, 0d);
    }

    public static void showToast(Context context, boolean fast, String message) {
        int length = Toast.LENGTH_LONG;
        if (fast) length = Toast.LENGTH_SHORT;

        Toast.makeText(context, message, length).show();
    }
}
