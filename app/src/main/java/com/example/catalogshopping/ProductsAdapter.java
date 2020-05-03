package com.example.catalogshopping;

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

public class ProductsAdapter extends RecyclerView.Adapter<ProductsAdapter.ViewHolder> {

    private ShoppingCart shoppingCart;

    public ProductsAdapter(ShoppingCart shoppingCart) {
        this.shoppingCart = shoppingCart;
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

        holder.closeButton.setOnClickListener(e -> {

        });
//        holder.productImageView = product.image
        holder.quantityTextView.setText("x " + product.getQuantity());
    }

    @Override
    public int getItemCount() {
        return shoppingCart.getProducts().size();
    }

    public ShoppingCart getShoppingCart() {
        return shoppingCart;
    }

    public void setShoppingCart(ShoppingCart shoppingCart) {
        this.shoppingCart = shoppingCart;
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
