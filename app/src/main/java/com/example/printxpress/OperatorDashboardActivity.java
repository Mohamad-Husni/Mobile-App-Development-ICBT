package com.example.printxpress;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OperatorDashboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private LinearProgressIndicator progressIndicator;
    private OperatorOrderAdapter adapter;
    private final List<Order> orderList = new ArrayList<>();
    private FirebaseFirestore firestoreDb;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_dashboard);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { startActivity(new Intent(this, LoginActivity.class)); finish(); return; }

        firestoreDb = FirebaseFirestore.getInstance();
        dbHelper    = new DBHelper(this);

        recyclerView      = findViewById(R.id.recyclerViewOrders);
        emptyState        = findViewById(R.id.emptyState);
        progressIndicator = findViewById(R.id.progressIndicator);

        TextView tvName = findViewById(R.id.tvOperatorName);
        String name = user.getDisplayName();
        tvName.setText(name != null ? "Operator: " + name.split(" ")[0] : "Operator");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnSignOut).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        loadOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void loadOrders() {
        progressIndicator.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);

        firestoreDb.collection("Orders")
                .whereNotEqualTo("status", DBHelper.STATUS_CANCELLED)
                .get()
                .addOnSuccessListener(snapshots -> {
                    orderList.clear();
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
                        orderList.add(order);
                    }
                    progressIndicator.setVisibility(View.GONE);
                    showOrders();
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load orders.", Toast.LENGTH_SHORT).show();
                    showOrders();
                });
    }

    private void showOrders() {
        if (orderList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter = new OperatorOrderAdapter(orderList, this::showStatusDialog);
            recyclerView.setAdapter(adapter);
        }
    }

    private void showStatusDialog(Order order, int position) {
        String current = order.getStatus();
        String[] options;
        if (DBHelper.STATUS_PROCESSING.equals(current)) {
            options = new String[]{DBHelper.STATUS_PRINTING, DBHelper.STATUS_READY};
        } else if (DBHelper.STATUS_PRINTING.equals(current)) {
            options = new String[]{DBHelper.STATUS_READY};
        } else {
            Toast.makeText(this, "Order already " + current, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Update Status")
                .setItems(options, (dialog, which) -> updateStatus(order, position, options[which]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateStatus(Order order, int position, String newStatus) {
        String fid = order.getFirebaseOrderId();
        if (fid == null || fid.isEmpty()) return;

        Map<String, Object> update = new HashMap<>();
        update.put("status", newStatus);

        firestoreDb.collection("Orders").document(fid)
                .update(update)
                .addOnSuccessListener(unused -> {
                    adapter.updateStatus(position, newStatus);
                    dbHelper.updateOrderStatusByFirebaseId(fid, newStatus);
                    sendStatusNotification(order, newStatus);
                    Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void sendStatusNotification(Order order, String newStatus) {
        String fid = order.getFirebaseOrderId();
        if (fid == null || fid.isEmpty()) return;

        firestoreDb.collection("Orders").document(fid).get()
                .addOnSuccessListener(doc -> {
                    String customerId = doc.getString("customerId");
                    String deliveryType = doc.getString("deliveryType");
                    if (customerId == null || customerId.isEmpty()) return;

                    String message;
                    String type;
                    if (DBHelper.STATUS_READY.equals(newStatus)) {
                        message = "Your order is Ready for " + (deliveryType != null ? deliveryType : "pickup") + "!";
                        type = DBHelper.NOTIF_COMPLETION;
                    } else if (DBHelper.STATUS_PRINTING.equals(newStatus)) {
                        message = "Your order is now being printed.";
                        type = DBHelper.NOTIF_CONFIRMATION;
                    } else {
                        message = "Your order status updated to " + newStatus + ".";
                        type = DBHelper.NOTIF_CONFIRMATION;
                    }

                    Map<String, Object> notif = new HashMap<>();
                    notif.put("userId", customerId);
                    notif.put("type", type);
                    notif.put("message", message);
                    notif.put("isRead", false);
                    notif.put("createdAt", com.google.firebase.Timestamp.now());
                    firestoreDb.collection("Notifications").add(notif);

                    String finalMessage = message;
                    String finalType = type;
                    firestoreDb.collection("Users").document(customerId).get()
                            .addOnSuccessListener(userDoc -> {
                                String fcmToken = userDoc.getString("fcmToken");
                                if (fcmToken != null && !fcmToken.isEmpty()) {
                                    sendFcmPush(fcmToken, "Order Update", finalMessage, finalType);
                                }
                            });
                });
    }

    private void sendFcmPush(String fcmToken, String title, String message, String type) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("https://fcm.googleapis.com/fcm/send");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setUseCaches(false);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "key=" + getFcmServerKey());
                conn.setRequestProperty("Content-Type", "application/json");

                org.json.JSONObject notification = new org.json.JSONObject();
                notification.put("title", title);
                notification.put("body", message);
                notification.put("sound", "default");

                org.json.JSONObject data = new org.json.JSONObject();
                data.put("title", title);
                data.put("body", message);
                data.put("type", type);

                org.json.JSONObject payload = new org.json.JSONObject();
                payload.put("to", fcmToken);
                payload.put("notification", notification);
                payload.put("data", data);
                payload.put("priority", "high");

                java.io.OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                android.util.Log.d("FCM_PUSH", "Response: " + code);
                conn.disconnect();
            } catch (Exception e) {
                android.util.Log.e("FCM_PUSH", "Failed: " + e.getMessage());
            }
        }).start();
    }

    private String getFcmServerKey() {
        return getString(R.string.fcm_server_key);
    }
}
