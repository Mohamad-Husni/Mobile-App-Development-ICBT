package com.example.printxpress;

import java.util.ArrayList;
import java.util.List;

public class CartManager {

    private static CartManager instance;
    private final List<CartItem> items = new ArrayList<>();

    private CartManager() {}

    public static CartManager getInstance() {
        if (instance == null) instance = new CartManager();
        return instance;
    }

    public void addItem(CartItem item) {
        for (CartItem existing : items) {
            if (existing.getProduct().getProductId().equals(item.getProduct().getProductId())) {
                existing.setQuantity(existing.getQuantity() + item.getQuantity());
                return;
            }
        }
        items.add(item);
    }

    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) items.remove(index);
    }

    public List<CartItem> getItems() { return items; }

    public int getCount() { return items.size(); }

    public double getTotal() {
        double total = 0;
        for (CartItem item : items) total += item.getSubtotal();
        return total;
    }

    public void clear() { items.clear(); }
}
