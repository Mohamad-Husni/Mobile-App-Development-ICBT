package com.example.printxpress;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class CustomerDashboardActivity extends AppCompatActivity {

    private static final String TAG = "OrderSync";
    private TextInputEditText etMaterial, etQuantity;
    private MaterialButton btnPlaceOrder;
    private DBHelper dbHelper;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        // Native Backend Initialization
        dbHelper = new DBHelper(this);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // UI Binding
        etMaterial = findViewById(R.id.etMaterial);
        etQuantity = findViewById(R.id.etQuantity);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);

        btnPlaceOrder.setOnClickListener(v -> handleOrderPlacement());
    }

    private void handleOrderPlacement() {
        String materialType = etMaterial.getText().toString().trim();
        String qtyInput = etQuantity.getText().toString().trim();

        // Input Validation
        if (TextUtils.isEmpty(materialType) || TextUtils.isEmpty(qtyInput)) {
            Toast.makeText(this, "Please provide material and quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(qtyInput);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show();
            return;
        }
        if (quantity <= 0) {
            Toast.makeText(this, "Quantity must be at least 1", Toast.LENGTH_SHORT).show();
            return;
        }

        double unitPrice = 12.0; // Mock Unit Price
        double totalAmount = quantity * unitPrice;

        FirebaseUser user = mAuth.getCurrentUser();
        String userEmail = (user != null) ? user.getEmail() : "anonymous@printxpress.com";

        try {
            // STEP A: Local DB - Insert Order Header
            long orderId = dbHelper.insertOrder(userEmail, totalAmount, "Pending");

            if (orderId != -1) {
                // STEP B: Local DB - Insert Relational Item Details
                boolean itemSuccess = dbHelper.insertOrderItem(orderId, materialType, quantity, unitPrice);

                if (itemSuccess) {
                    // STEP C: Cloud DB - Sync with Firebase
                    syncToCloud(userEmail, materialType, quantity, totalAmount);
                } else {
                    Toast.makeText(this, "Couldn't save your order details. Please try again.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Couldn't place your order. Please try again.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Critical Database Error: " + e.getMessage());
            Toast.makeText(this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void syncToCloud(String email, String material, int qty, double total) {
        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, "Order saved. It will sync to the cloud once you're back online.", Toast.LENGTH_SHORT).show();
            etMaterial.setText("");
            etQuantity.setText("");
            return;
        }

        String cloudId = mDatabase.child("Orders").push().getKey();

        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("customer_email", email);
        orderMap.put("material", material);
        orderMap.put("quantity", qty);
        orderMap.put("total_price", total);
        orderMap.put("status", "Pending");
        orderMap.put("created_at", System.currentTimeMillis());

        if (cloudId != null) {
            mDatabase.child("Orders").child(cloudId).setValue(orderMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Order Placed & Synced Successfully!", Toast.LENGTH_LONG).show();
                    etMaterial.setText("");
                    etQuantity.setText("");
                })
                .addOnFailureListener(e -> {
                    // Fail gracefully: Order is already in SQLite
                    Toast.makeText(this, "Order saved locally, cloud sync pending.", Toast.LENGTH_SHORT).show();
                });
        }
    }
}
