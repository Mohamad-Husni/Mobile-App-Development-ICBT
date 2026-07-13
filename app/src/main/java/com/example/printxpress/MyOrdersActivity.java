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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
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
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    progressIndicator.setVisibility(View.GONE);
                    if (error != null || snapshots == null) {
                        showEmpty();
                        return;
                    }
                    orderList = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Order o = new Order();
                        o.setFirebaseOrderId(doc.getId());
                        o.setCustomerId(doc.getString("customerId"));
                        o.setDeliveryType(doc.getString("deliveryType") != null ? doc.getString("deliveryType") : "");
                        o.setStatus(doc.getString("status") != null ? doc.getString("status") : DBHelper.STATUS_PROCESSING);
                        Double price = doc.getDouble("totalPrice");
                        o.setTotalPrice(price != null ? price : 0.0);
                        com.google.firebase.Timestamp ts = doc.getTimestamp("orderDate");
                        o.setOrderDate(ts != null ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(ts.toDate()) : "");
                        orderList.add(o);
                        dbHelper.updateOrderStatusByFirebaseId(doc.getId(), o.getStatus());
                    }
                    if (orderList.isEmpty()) { showEmpty(); return; }
                    emptyState.setVisibility(View.GONE);
                    recyclerViewOrders.setVisibility(View.VISIBLE);
                    if (adapter == null) {
                        adapter = new OrderAdapter(orderList, this::confirmCancel, this::confirmReschedule);
                        recyclerViewOrders.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void showEmpty() {
        emptyState.setVisibility(View.VISIBLE);
        recyclerViewOrders.setVisibility(View.GONE);
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
