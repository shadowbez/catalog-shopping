package com.example.catalogshopping.other;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.example.catalogshopping.model.ProductFirestore;
import com.google.android.material.snackbar.Snackbar;

/**
 * General Util class
 */
public class Util {

    /**
     * Create Dummy object
     * @param id Id of object
     * @return The dummy object
     */
    public static ProductFirestore dummyProductFirestore(String id) {
        return new ProductFirestore(id, "Product was not found in dummy database.", id, 0d);
    }

    /**
     * Easy access toast
     * @param context context of the view
     * @param fast true if message should be showed fast, false otherwise
     * @param message the content of the toast
     */
    public static void showToast(Context context, boolean fast, String message) {
        int length = Toast.LENGTH_LONG;
        if (fast) length = Toast.LENGTH_SHORT;

        Toast.makeText(context, message, length).show();
    }

    /**
     * Easy access snackbar
     * @param view the view it should be attached to
     * @param fast true if message should be showed fast, false otherwise
     * @param message the content of the snackbar
     */
    public static void showSnackbar(View view, boolean fast, String message) {
        int length = Snackbar.LENGTH_LONG;
        if (fast) length = Snackbar.LENGTH_SHORT;

        Snackbar.make(view, message, length).show();
    }
}
