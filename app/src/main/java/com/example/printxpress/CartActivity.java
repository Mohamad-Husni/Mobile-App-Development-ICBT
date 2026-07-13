package com.example.printxpress;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartChangeListener {

    private RecyclerView recyclerView;
    private LinearLayout emptyCart;
    private TextView tvOrderTotal;
    private RadioGroup rgDelivery;
    private MaterialButton btnPlaceOrder;
    private CircularProgressIndicator progressIndicator;
    private CartAdapter adapter;
    private DBHelper dbHelper;
    private FirebaseFirestore firestoreDb;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        dbHelper    = new DBHelper(this);
        firestoreDb = FirebaseFirestore.getInstance();

        recyclerView      = findViewById(R.id.recyclerViewCart);
        emptyCart         = findViewById(R.id.emptyCart);
        tvOrderTotal      = findViewById(R.id.tvOrderTotal);
        rgDelivery        = findViewById(R.id.rgDelivery);
        btnPlaceOrder     = findViewById(R.id.btnPlaceOrder);
        progressIndicator = findViewById(R.id.progressIndicator);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnPlaceOrder.setOnClickListener(v -> confirmAndPlaceOrder());

        refreshCart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCart();
    }

    private void refreshCart() {
        List<CartItem> items = CartManager.getInstance().getItems();
        if (items.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyCart.setVisibility(View.VISIBLE);
            btnPlaceOrder.setEnabled(false);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyCart.setVisibility(View.GONE);
            btnPlaceOrder.setEnabled(true);
            adapter = new CartAdapter(items, this);
            recyclerView.setAdapter(adapter);
        }
        updateTotal();
    }

    @Override
    public void onCartChanged() {
        updateTotal();
        if (CartManager.getInstance().getItems().isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyCart.setVisibility(View.VISIBLE);
            btnPlaceOrder.setEnabled(false);
        }
    }

    private void updateTotal() {
        tvOrderTotal.setText(String.format("LKR %.2f", CartManager.getInstance().getTotal()));
    }

    private void confirmAndPlaceOrder() {
        List<CartItem> cartItems = CartManager.getInstance().getItems();
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        int checkedId = rgDelivery.getCheckedRadioButtonId();
        String deliveryType = (checkedId == R.id.rbDelivery) ? "Home Delivery" : "Store Pickup";
        double totalPrice = CartManager.getInstance().getTotal();

        new AlertDialog.Builder(this)
                .setTitle("Confirm Order")
                .setMessage("Delivery: " + deliveryType
                        + "\nItems: " + cartItems.size()
                        + "\nTotal: LKR " + String.format("%.2f", totalPrice)
                        + "\n\nPlace this order?")
                .setPositiveButton("Yes, Place Order", (dialog, which) -> {
                    setLoading(true);
                    placeOrder(cartItems, deliveryType, totalPrice);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void placeOrder(List<CartItem> cartItems, String deliveryType, double totalPrice) {
        String uid = currentUser.getUid();

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("customerId", uid);
        orderData.put("customerEmail", currentUser.getEmail());
        orderData.put("deliveryType", deliveryType);
        orderData.put("totalPrice", totalPrice);
        orderData.put("status", DBHelper.STATUS_PROCESSING);
        orderData.put("orderDate", com.google.firebase.Timestamp.now());

        firestoreDb.collection("Orders").add(orderData)
                .addOnSuccessListener(orderRef -> {
                    String firebaseOrderId = orderRef.getId();
                    long localOrderId = dbHelper.insertOrder(uid, deliveryType, totalPrice, firebaseOrderId);

                    List<Map<String, Object>> firestoreItems = new ArrayList<>();
                    for (CartItem cartItem : cartItems) {
                        Map<String, Object> itemData = new HashMap<>();
                        itemData.put("productId", cartItem.getProduct().getProductId());
                        itemData.put("productName", cartItem.getProduct().getCategory());
                        itemData.put("quantity", cartItem.getQuantity());
                        itemData.put("unitPrice", cartItem.getProduct().getBasePrice());
                        itemData.put("artworkFileName", cartItem.getArtworkFileName());
                        firestoreItems.add(itemData);

                        orderRef.collection("items").add(itemData)
                                .addOnSuccessListener(itemRef ->
                                        dbHelper.insertOrderItem(localOrderId,
                                                cartItem.getProduct().getProductId(),
                                                cartItem.getQuantity(),
                                                cartItem.getProduct().getBasePrice(),
                                                cartItem.getArtworkFileName(),
                                                itemRef.getId()));
                    }

                    sendConfirmationNotification(uid, firebaseOrderId, cartItems.size());
                    CartManager.getInstance().clear();
                    setLoading(false);
                    Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, MyOrdersActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    long localOrderId = dbHelper.insertOrder(uid, deliveryType, totalPrice,
                            "offline_" + System.currentTimeMillis());
                    for (CartItem cartItem : cartItems) {
                        dbHelper.insertOrderItem(localOrderId,
                                cartItem.getProduct().getProductId(),
                                cartItem.getQuantity(),
                                cartItem.getProduct().getBasePrice(),
                                cartItem.getArtworkFileName(), "");
                    }
                    dbHelper.insertNotification(uid, DBHelper.NOTIF_CONFIRMATION,
                            "Your order has been placed and saved locally.");
                    CartManager.getInstance().clear();
                    setLoading(false);
                    Toast.makeText(this, "Order saved locally. Will sync when online.", Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void sendConfirmationNotification(String uid, String orderId, int itemCount) {
        String message = "Your order (#" + orderId.substring(0, 6) + ") with "
                + itemCount + " item(s) is now Processing.";
        dbHelper.insertNotification(uid, DBHelper.NOTIF_CONFIRMATION, message);

        Map<String, Object> notifData = new HashMap<>();
        notifData.put("userId", uid);
        notifData.put("type", DBHelper.NOTIF_CONFIRMATION);
        notifData.put("message", message);
        notifData.put("isRead", false);
        notifData.put("createdAt", com.google.firebase.Timestamp.now());
        firestoreDb.collection("Notifications").add(notifData);
    }

    private void setLoading(boolean loading) {
        progressIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnPlaceOrder.setEnabled(!loading);
    }
}
