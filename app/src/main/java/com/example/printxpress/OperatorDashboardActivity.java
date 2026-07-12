package com.example.printxpress;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

/**
 * Manages orders with local SQLite persistence and Firebase Realtime sync.
 */
public class OperatorDashboardActivity extends AppCompatActivity {

    private static final String STATUS_READY = "Ready";

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
        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow("total_price"));

                addOrderView(id, email, status, price);
            }
        } finally {
            cursor.close();
        }

        if (orderListContainer.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No orders yet.");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            orderListContainer.addView(empty);
        }
    }

    private void addOrderView(final long id, final String email, final String status, double price) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_order_card, orderListContainer, false);

        TextView tvOrderId = card.findViewById(R.id.tvOrderId);
        TextView tvCustomerEmail = card.findViewById(R.id.tvCustomerEmail);
        TextView tvTotalPrice = card.findViewById(R.id.tvTotalPrice);
        TextView tvStatus = card.findViewById(R.id.tvStatus);

        tvOrderId.setText(getString(R.string.order_id_format, id));
        tvCustomerEmail.setText(getString(R.string.customer_email_format, email));
        tvTotalPrice.setText(getString(R.string.total_price_format, price));
        tvStatus.setText(status);

        boolean isReady = STATUS_READY.equals(status);
        tvStatus.setBackgroundTintList(ContextCompat.getColorStateList(this,
                isReady ? R.color.success_bg : R.color.warning_bg));
        tvStatus.setTextColor(ContextCompat.getColor(this, isReady ? R.color.success : R.color.warning));
        card.setAlpha(isReady ? 0.6f : 1f);

        card.setOnClickListener(v -> {
            if (isReady) {
                Toast.makeText(this, "This order has already been completed.", Toast.LENGTH_SHORT).show();
            } else {
                processOrder(id, email);
            }
        });

        orderListContainer.addView(card);
    }

    private void processOrder(long orderId, String email) {
        if (dbHelper.updateOrderStatus(orderId, STATUS_READY)) {
            syncStatusToCloud(email);
            Toast.makeText(this, "Order #" + orderId + " marked as ready.", Toast.LENGTH_SHORT).show();
            loadOrders();
        } else {
            Toast.makeText(this, "Couldn't update the order. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void syncStatusToCloud(String email) {
        if (!NetworkUtils.isConnected(this)) {
            return; // Local status is updated; cloud sync will simply be stale until back online.
        }
        Query query = mDatabase.child("Orders").orderByChild("customer_email").equalTo(email);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String currentStatus = snapshot.child("status").getValue(String.class);
                    if ("Pending".equals(currentStatus)) {
                        snapshot.getRef().child("status").setValue(STATUS_READY);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Local SQLite is already updated; cloud sync will be retried on next status change.
            }
        });
    }
}
