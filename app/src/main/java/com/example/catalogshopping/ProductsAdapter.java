package com.example.catalogshopping;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.catalogshopping.model.Product;
import com.example.catalogshopping.model.ShoppingCart;
import com.example.catalogshopping.view.MainActivity;

import java.lang.ref.WeakReference;

/**
 * Adapter class for RecyclerView of shopping functionality of the application.
 */
public class ProductsAdapter extends RecyclerView.Adapter<ProductsAdapter.ViewHolder> {

    /**
     * WeakReference of MainActivity in order to safely gather activity reference and use properties.
     */
    private WeakReference<MainActivity> activity;

    /**
     * ShoppingCart object which stores all data of a product.
     */
    private ShoppingCart shoppingCart;

    public ProductsAdapter(ShoppingCart shoppingCart, WeakReference<MainActivity> activity) {
        this.shoppingCart = shoppingCart;
        this.activity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View contactView = inflater.inflate(R.layout.cart_item, parent, false);

        return new ViewHolder(contactView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = shoppingCart.getProducts().get(position);

        // LOAD IMAGE FROM FILE AND SETUP CLICK LISTENER
        Bitmap myBitmap = BitmapFactory.decodeFile(product.getImage().getAbsolutePath());
        holder.productImageView.setImageBitmap(myBitmap);
        holder.productImageView.setOnClickListener(e -> {
            if (activity.get() != null) {
                activity.get().addModelToScene(product.getModel());
            }
        });

        // Remove product form shopping cart and notify UI to update
        holder.closeButton.setOnClickListener(e -> {
            shoppingCart.getProducts().remove(position);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return shoppingCart.getProducts().size();
    }

    public ShoppingCart getShoppingCart() {
        return shoppingCart;
    }

    /**
     * Inner class which holds the views for a single item in the RecyclerView.
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        Button closeButton;
        ImageView productImageView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            closeButton = (Button) itemView.findViewById(R.id.button_cart_close);
            productImageView = (ImageView) itemView.findViewById(R.id.image_cart_main);
        }
    }
}
