package com.example.printxpress;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

/**
 * Senior Developer Implementation of Operator Dashboard.
 * Manages orders with local SQLite persistence and Firebase Realtime sync.
 */
public class OperatorDashboardActivity extends AppCompatActivity {

    private LinearLayout orderListContainer;
    private DBHelper dbHelper;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_dashboard);

        dbHelper = new DBHelper(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        orderListContainer = findViewById(R.id.orderListContainer);

        loadOrders();
    }

    private void loadOrders() {
        orderListContainer.removeAllViews();
        Cursor cursor = dbHelper.getAllOrders();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                final long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                final String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                final String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                final double price = cursor.getDouble(cursor.getColumnIndexOrThrow("total_price"));

                addOrderView(id, email, status, price);
            }
            cursor.close();
        }
    }

    private void addOrderView(final long id, final String email, final String status, double price) {
        // Create a container for the order item
        LinearLayout orderCard = new LinearLayout(this);
        orderCard.setOrientation(LinearLayout.VERTICAL);
        orderCard.setPadding(40, 40, 40, 40);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 30);
        orderCard.setLayoutParams(params);
        
        // Native background styling
        orderCard.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        
        if ("Ready".equals(status)) {
            orderCard.setAlpha(0.5f);
        }

        // Order Details
        TextView tvInfo = new TextView(this);
        tvInfo.setText("Order #" + id + "\nCustomer: " + email + "\nTotal: $" + String.format("%.2f", price));
        tvInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvInfo.setTextColor(Color.BLACK);

        // Status
        TextView tvStatus = new TextView(this);
        tvStatus.setText("Status: " + status);
        tvStatus.setPadding(0, 10, 0, 0);
        tvStatus.setTypeface(null, Typeface.BOLD);
        tvStatus.setTextColor("Ready".equals(status) ? Color.parseColor("#2E7D32") : Color.parseColor("#D32F2F"));

        orderCard.addView(tvInfo);
        orderCard.addView(tvStatus);

        // Click to process order
        orderCard.setOnClickListener(v -> {
            if (!"Ready".equals(status)) {
                processOrder(id, email);
            } else {
                Toast.makeText(this, "Order is already completed", Toast.LENGTH_SHORT).show();
            }
        });

        orderListContainer.addView(orderCard);
    }

    private void processOrder(long orderId, String email) {
        // 1. Update Local SQLite
        if (dbHelper.updateOrderStatus(orderId, "Ready")) {
            
            // 2. Sync to Firebase Cloud
            syncStatusToCloud(email);
            
            // 3. UI Feedback
            Toast.makeText(this, "Order #" + orderId + " Marked as Ready!", Toast.LENGTH_SHORT).show();
            loadOrders(); // Refresh list
        } else {
            Toast.makeText(this, "Failed to update local database", Toast.LENGTH_SHORT).show();
        }
    }

    private void syncStatusToCloud(String email) {
        // Simple search and update in Firebase by email
        Query query = mDatabase.child("Orders").orderByChild("customer_email").equalTo(email);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Update only pending orders for this user to 'Ready'
                    String currentStatus = snapshot.child("status").getValue(String.class);
                    if ("Pending".equals(currentStatus)) {
                        snapshot.getRef().child("status").setValue("Ready");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors
            }
        });
    }
}
