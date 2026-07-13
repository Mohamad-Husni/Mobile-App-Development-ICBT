package com.example.printxpress;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class MyOrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerViewOrders;
    private LinearLayout emptyState;
    private CircularProgressIndicator progressIndicator;
    private OrderAdapter adapter;
    private List<Order> orderList;
    private DBHelper dbHelper;
    private FirebaseFirestore firestoreDb;
    private String userId;
    private ListenerRegistration ordersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userId = currentUser.getUid();

        dbHelper    = new DBHelper(this);
        firestoreDb = FirebaseFirestore.getInstance();

        recyclerViewOrders = findViewById(R.id.recyclerViewOrders);
        emptyState         = findViewById(R.id.emptyState);
        progressIndicator  = findViewById(R.id.progressIndicator);

        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ordersListener == null) loadOrders();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListener != null) ordersListener.remove();
    }

    private void loadOrders() {
        progressIndicator.setVisibility(View.VISIBLE);
        recyclerViewOrders.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);

        ordersListener = firestoreDb.collection("Orders")
                .whereEqualTo("customerId", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (snapshots != null) {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                            String status = doc.getString("status");
                            if (status != null) {
                                dbHelper.updateOrderStatusByFirebaseId(doc.getId(), status);
                            }
                        }
                    }
                    displayLocalOrders();
                });
    }

    private void displayLocalOrders() {
        orderList = dbHelper.getOrdersForCustomer(userId);
        for (Order order : orderList) {
            order.setItems(dbHelper.getItemsForOrder(order.getLocalId()));
        }
        progressIndicator.setVisibility(View.GONE);

        if (orderList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerViewOrders.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerViewOrders.setVisibility(View.VISIBLE);
            adapter = new OrderAdapter(orderList, this::confirmCancel, this::confirmReschedule);
            recyclerViewOrders.setAdapter(adapter);
        }
    }

    private void confirmCancel(Order order, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Order")
                .setMessage("Cancel this order? This cannot be undone.")
                .setPositiveButton("Cancel Order", (dialog, which) -> cancelOrder(order, position))
                .setNegativeButton("Keep", null)
                .show();
    }

    private void cancelOrder(Order order, int position) {
        dbHelper.updateOrderStatus(order.getLocalId(), DBHelper.STATUS_CANCELLED);
        order.setStatus(DBHelper.STATUS_CANCELLED);
        adapter.notifyItemChanged(position);

        String fid = order.getFirebaseOrderId();
        if (fid != null && !fid.startsWith("offline_") && !fid.isEmpty()) {
            firestoreDb.collection("Orders").document(fid)
                    .update("status", DBHelper.STATUS_CANCELLED);
        }
        Toast.makeText(this, "Order cancelled.", Toast.LENGTH_SHORT).show();
    }

    private void confirmReschedule(Order order, int position) {
        String[] options = {"Tomorrow", "In 2 days", "In 3 days"};
        new AlertDialog.Builder(this)
                .setTitle("Reschedule Order")
                .setItems(options, (dialog, which) -> {
                    String note = "Rescheduled: " + options[which];
                    Toast.makeText(this, note, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
