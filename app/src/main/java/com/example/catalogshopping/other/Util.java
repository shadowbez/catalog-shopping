package com.example.catalogshopping.other;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.example.catalogshopping.model.ProductFirestore;
import com.google.android.material.snackbar.Snackbar;

public class Util {

    public static ProductFirestore dummyProductFirestore(String id) {
        return new ProductFirestore(id, "Product was not found in dummy database.", id, 0d);
    }

    public static void showToast(Context context, boolean fast, String message) {
        int length = Toast.LENGTH_LONG;
        if (fast) length = Toast.LENGTH_SHORT;

        Toast.makeText(context, message, length).show();
    }

    public static void showSnackbar(View view, boolean fast, String message) {
        int length = Snackbar.LENGTH_LONG;
        if (fast) length = Snackbar.LENGTH_SHORT;

        Snackbar.make(view, message, length).show();
    }
}
