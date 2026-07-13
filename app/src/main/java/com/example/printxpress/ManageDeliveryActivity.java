package com.example.printxpress;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageDeliveryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private LinearProgressIndicator progressIndicator;
    private DeliveryAdapter adapter;
    private final List<DeliveryOption> optionList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_delivery);

        db = FirebaseFirestore.getInstance();

        recyclerView      = findViewById(R.id.recyclerDelivery);
        emptyState        = findViewById(R.id.emptyDelivery);
        progressIndicator = findViewById(R.id.progressDelivery);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        FloatingActionButton fab = findViewById(R.id.fabAddDelivery);
        fab.setOnClickListener(v -> showEditDialog(null, -1));

        loadOptions();
    }

    private void loadOptions() {
        progressIndicator.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);

        db.collection("DeliveryOptions")
                .orderBy("name")
                .get()
                .addOnSuccessListener(snapshots -> {
                    optionList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        DeliveryOption opt = new DeliveryOption();
                        opt.setId(doc.getId());
                        opt.setName(doc.getString("name") != null ? doc.getString("name") : "");
                        opt.setDescription(doc.getString("description") != null ? doc.getString("description") : "");
                        Double price = doc.getDouble("price");
                        opt.setPrice(price != null ? price : 0.0);
                        Boolean active = doc.getBoolean("active");
                        opt.setActive(active != null ? active : true);
                        optionList.add(opt);
                    }
                    progressIndicator.setVisibility(View.GONE);
                    showList();
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load delivery options.", Toast.LENGTH_SHORT).show();
                    showList();
                });
    }

    private void showList() {
        if (optionList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter = new DeliveryAdapter(optionList, this::showEditDialog, this::confirmDelete, this::toggleActive);
            recyclerView.setAdapter(adapter);
        }
    }

    private void showEditDialog(DeliveryOption existing, int position) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delivery_option, null);
        EditText etName  = dialogView.findViewById(R.id.etDeliveryName);
        EditText etDesc  = dialogView.findViewById(R.id.etDeliveryDesc);
        EditText etPrice = dialogView.findViewById(R.id.etDeliveryPrice);

        boolean isEdit = existing != null;
        if (isEdit) {
            etName.setText(existing.getName());
            etDesc.setText(existing.getDescription());
            etPrice.setText(String.valueOf(existing.getPrice()));
        }

        new AlertDialog.Builder(this)
                .setTitle(isEdit ? "Edit Delivery Option" : "Add Delivery Option")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name  = etName.getText().toString().trim();
                    String desc  = etDesc.getText().toString().trim();
                    String priceStr = etPrice.getText().toString().trim();

                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(this, "Name is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double price = 0.0;
                    try { price = Double.parseDouble(priceStr); } catch (NumberFormatException ignored) {}

                    Map<String, Object> data = new HashMap<>();
                    data.put("name", name);
                    data.put("description", desc);
                    data.put("price", price);
                    data.put("active", isEdit ? existing.isActive() : true);

                    if (isEdit && existing.getId() != null) {
                        double finalPrice = price;
                        String finalName = name;
                        String finalDesc = desc;
                        db.collection("DeliveryOptions").document(existing.getId())
                                .update(data)
                                .addOnSuccessListener(unused -> {
                                    existing.setName(finalName);
                                    existing.setDescription(finalDesc);
                                    existing.setPrice(finalPrice);
                                    adapter.notifyItemChanged(position);
                                    sendDeliveryUpdateNotification(finalName);
                                    Toast.makeText(this, "Updated.", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        String finalName = name;
                        String finalDesc = desc;
                        double finalPrice = price;
                        db.collection("DeliveryOptions").add(data)
                                .addOnSuccessListener(ref -> {
                                    DeliveryOption newOpt = new DeliveryOption();
                                    newOpt.setId(ref.getId());
                                    newOpt.setName(finalName);
                                    newOpt.setDescription(finalDesc);
                                    newOpt.setPrice(finalPrice);
                                    newOpt.setActive(true);
                                    optionList.add(newOpt);
                                    if (adapter != null) {
                                        adapter.notifyItemInserted(optionList.size() - 1);
                                    } else {
                                        showList();
                                    }
                                    emptyState.setVisibility(View.GONE);
                                    recyclerView.setVisibility(View.VISIBLE);
                                    sendDeliveryUpdateNotification(finalName);
                                    Toast.makeText(this, "Delivery option added.", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete(DeliveryOption opt, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Delivery Option")
                .setMessage("Delete \"" + opt.getName() + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (opt.getId() == null) return;
                    db.collection("DeliveryOptions").document(opt.getId())
                            .delete()
                            .addOnSuccessListener(unused -> {
                                optionList.remove(position);
                                adapter.notifyItemRemoved(position);
                                if (optionList.isEmpty()) {
                                    recyclerView.setVisibility(View.GONE);
                                    emptyState.setVisibility(View.VISIBLE);
                                }
                                Toast.makeText(this, "Deleted.", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleActive(DeliveryOption opt, int position) {
        if (opt.getId() == null) return;
        boolean newActive = !opt.isActive();
        db.collection("DeliveryOptions").document(opt.getId())
                .update("active", newActive)
                .addOnSuccessListener(unused -> {
                    opt.setActive(newActive);
                    adapter.notifyItemChanged(position);
                    Toast.makeText(this, opt.getName() + (newActive ? " enabled." : " disabled."), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendDeliveryUpdateNotification(String optionName) {
        Map<String, Object> notif = new HashMap<>();
        notif.put("userId", "all");
        notif.put("type", "delivery_update");
        notif.put("message", "Delivery option updated: " + optionName);
        notif.put("isRead", false);
        notif.put("createdAt", com.google.firebase.Timestamp.now());
        db.collection("Notifications").add(notif);
    }

    static class DeliveryOption {
        private String id, name, description;
        private double price;
        private boolean active;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    static class DeliveryAdapter extends RecyclerView.Adapter<DeliveryAdapter.DVH> {
        interface OnEdit { void onEdit(DeliveryOption opt, int pos); }
        interface OnDelete { void onDelete(DeliveryOption opt, int pos); }
        interface OnToggle { void onToggle(DeliveryOption opt, int pos); }

        private final List<DeliveryOption> list;
        private final OnEdit editListener;
        private final OnDelete deleteListener;
        private final OnToggle toggleListener;

        DeliveryAdapter(List<DeliveryOption> list, OnEdit e, OnDelete d, OnToggle t) {
            this.list = list; this.editListener = e; this.deleteListener = d; this.toggleListener = t;
        }

        @NonNull @Override
        public DVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_delivery_option, parent, false);
            return new DVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull DVH h, int position) {
            DeliveryOption opt = list.get(position);
            h.tvName.setText(opt.getName());
            h.tvDesc.setText(opt.getDescription());
            h.tvPrice.setText(opt.getPrice() == 0 ? "Free" : String.format("LKR %.2f", opt.getPrice()));
            h.tvStatus.setText(opt.isActive() ? "Active" : "Disabled");
            h.tvStatus.setTextColor(opt.isActive()
                    ? android.graphics.Color.parseColor("#2E7D32")
                    : android.graphics.Color.parseColor("#9E9E9E"));

            h.btnEdit.setOnClickListener(v -> { int p = h.getAdapterPosition(); if (p != RecyclerView.NO_ID) editListener.onEdit(opt, p); });
            h.btnDelete.setOnClickListener(v -> { int p = h.getAdapterPosition(); if (p != RecyclerView.NO_ID) deleteListener.onDelete(opt, p); });
            h.btnToggle.setText(opt.isActive() ? "Disable" : "Enable");
            h.btnToggle.setOnClickListener(v -> { int p = h.getAdapterPosition(); if (p != RecyclerView.NO_ID) toggleListener.onToggle(opt, p); });
        }

        @Override public int getItemCount() { return list.size(); }

        static class DVH extends RecyclerView.ViewHolder {
            TextView tvName, tvDesc, tvPrice, tvStatus;
            MaterialButton btnEdit, btnDelete, btnToggle;
            DVH(@NonNull View v) {
                super(v);
                tvName   = v.findViewById(R.id.tvDeliveryName);
                tvDesc   = v.findViewById(R.id.tvDeliveryDesc);
                tvPrice  = v.findViewById(R.id.tvDeliveryPrice);
                tvStatus = v.findViewById(R.id.tvDeliveryStatus);
                btnEdit   = v.findViewById(R.id.btnEditDelivery);
                btnDelete = v.findViewById(R.id.btnDeleteDelivery);
                btnToggle = v.findViewById(R.id.btnToggleDelivery);
            }
        }
    }
}
