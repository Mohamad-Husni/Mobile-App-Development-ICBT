package com.example.printxpress;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface OnCartChangeListener {
        void onCartChanged();
    }

    private final List<CartItem> items;
    private final OnCartChangeListener listener;

    public CartAdapter(List<CartItem> items, OnCartChangeListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart_card, parent, false);
        return new CartViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder h, int position) {
        CartItem item = items.get(position);
        h.tvName.setText(item.getProduct().getCategory());
        h.tvUnitPrice.setText(String.format("LKR %.2f / unit", item.getProduct().getBasePrice()));
        h.tvArtwork.setText("Artwork: " + (item.getArtworkFileName().isEmpty()
                ? "None" : item.getArtworkFileName()));
        h.tvQty.setText(String.valueOf(item.getQuantity()));
        h.tvSubtotal.setText(String.format("Subtotal: LKR %.2f", item.getSubtotal()));

        h.btnIncrement.setOnClickListener(v -> {
            item.setQuantity(item.getQuantity() + 1);
            h.tvQty.setText(String.valueOf(item.getQuantity()));
            h.tvSubtotal.setText(String.format("Subtotal: LKR %.2f", item.getSubtotal()));
            listener.onCartChanged();
        });

        h.btnDecrement.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                h.tvQty.setText(String.valueOf(item.getQuantity()));
                h.tvSubtotal.setText(String.format("Subtotal: LKR %.2f", item.getSubtotal()));
                listener.onCartChanged();
            }
        });

        h.btnRemove.setOnClickListener(v -> {
            int pos = h.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) {
                CartManager.getInstance().removeItem(pos);
                items.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, items.size());
                listener.onCartChanged();
            }
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvUnitPrice, tvArtwork, tvQty, tvSubtotal;
        MaterialButton btnIncrement, btnDecrement;
        View btnRemove;

        CartViewHolder(@NonNull View v) {
            super(v);
            tvName      = v.findViewById(R.id.tvCartProductName);
            tvUnitPrice = v.findViewById(R.id.tvCartUnitPrice);
            tvArtwork   = v.findViewById(R.id.tvCartArtwork);
            tvQty       = v.findViewById(R.id.tvQty);
            tvSubtotal  = v.findViewById(R.id.tvCartSubtotal);
            btnIncrement = v.findViewById(R.id.btnIncrement);
            btnDecrement = v.findViewById(R.id.btnDecrement);
            btnRemove   = v.findViewById(R.id.btnRemove);
        }
    }
}
