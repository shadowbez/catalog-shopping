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

public class ProductsAdapter extends RecyclerView.Adapter<ProductsAdapter.ViewHolder> {

    private WeakReference<MainActivity> activity;
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

        Bitmap myBitmap = BitmapFactory.decodeFile(product.getImage().getAbsolutePath());
        holder.productImageView.setImageBitmap(myBitmap);
        holder.productImageView.setOnClickListener(e -> {
            if (activity.get() != null) {
                activity.get().addModelToScene(product.getModel());
            }
        });

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

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView quantityTextView;
        Button closeButton;
        ImageView productImageView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            quantityTextView = (TextView) itemView.findViewById(R.id.text_cart_number);
            closeButton = (Button) itemView.findViewById(R.id.button_cart_close);
            productImageView = (ImageView) itemView.findViewById(R.id.image_cart_main);
        }
    }
}
