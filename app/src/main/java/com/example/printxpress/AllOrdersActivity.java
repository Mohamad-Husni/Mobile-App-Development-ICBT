package com.example.printxpress;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AllOrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private CircularProgressIndicator progress;
    private OperatorOrderAdapter adapter;
    private final List<Order> orders = new ArrayList<>();
    private FirebaseFirestore firestoreDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_orders);

        firestoreDb  = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerAllOrders);
        emptyState   = findViewById(R.id.emptyState);
        progress     = findViewById(R.id.progressIndicator);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OperatorOrderAdapter(orders, (order, pos) -> {});
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        loadAllOrders();
    }

    private void loadAllOrders() {
        progress.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);

        firestoreDb.collection("Orders").get()
                .addOnSuccessListener(snapshots -> {
                    orders.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Order order = new Order();
                        order.setFirebaseOrderId(doc.getId());
                        order.setCustomerId(doc.getString("customerEmail") != null
                                ? doc.getString("customerEmail") : doc.getString("customerId"));
                        order.setStatus(doc.getString("status") != null
                                ? doc.getString("status") : DBHelper.STATUS_PROCESSING);
                        order.setDeliveryType(doc.getString("deliveryType") != null
                                ? doc.getString("deliveryType") : "");
                        Double total = doc.getDouble("totalPrice");
                        order.setTotalPrice(total != null ? total : 0.0);
                        orders.add(order);
                    }
                    progress.setVisibility(View.GONE);
                    if (orders.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load orders.", Toast.LENGTH_SHORT).show();
                });
    }
}
