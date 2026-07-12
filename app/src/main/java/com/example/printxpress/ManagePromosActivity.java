package com.example.printxpress;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.view.ViewGroup;
import android.widget.TextView;

public class ManagePromosActivity extends AppCompatActivity {

    private final List<Map<String, Object>> promos = new ArrayList<>();
    private PromoAdapter adapter;
    private FirebaseFirestore firestoreDb;
    private RecyclerView recyclerView;
    private CircularProgressIndicator progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_promos);

        firestoreDb = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerPromos);
        progress     = findViewById(R.id.progressIndicator);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PromoAdapter(promos, this::toggleActive, this::confirmDelete);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddPromo).setOnClickListener(v -> showAddPromoDialog());
        loadPromos();
    }

    private void loadPromos() {
        progress.setVisibility(View.VISIBLE);
        firestoreDb.collection("Promotions").get()
                .addOnSuccessListener(snapshots -> {
                    promos.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Map<String, Object> p = new HashMap<>(doc.getData());
                        p.put("id", doc.getId());
                        promos.add(p);
                    }
                    progress.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load promos.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddPromoDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(48, 24, 48, 8);
        EditText etTitle = new EditText(this); etTitle.setHint("Title");
        EditText etSub   = new EditText(this); etSub.setHint("Subtitle");
        CheckBox cbActive = new CheckBox(this); cbActive.setText("Active"); cbActive.setChecked(true);
        form.addView(etTitle); form.addView(etSub); form.addView(cbActive);

        new AlertDialog.Builder(this)
                .setTitle("Add Promotion")
                .setView(form)
                .setPositiveButton("Save", (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    String sub   = etSub.getText().toString().trim();
                    if (title.isEmpty()) { Toast.makeText(this, "Title required.", Toast.LENGTH_SHORT).show(); return; }
                    Map<String, Object> data = new HashMap<>();
                    data.put("title", title); data.put("subtitle", sub); data.put("active", cbActive.isChecked());
                    firestoreDb.collection("Promotions").add(data)
                            .addOnSuccessListener(ref -> {
                                data.put("id", ref.getId());
                                promos.add(data);
                                adapter.notifyItemInserted(promos.size() - 1);
                                Toast.makeText(this, "Promo added.", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void toggleActive(Map<String, Object> promo, int position) {
        boolean current = Boolean.TRUE.equals(promo.get("active"));
        firestoreDb.collection("Promotions").document((String) promo.get("id"))
                .update("active", !current)
                .addOnSuccessListener(unused -> {
                    promo.put("active", !current);
                    adapter.notifyItemChanged(position);
                });
    }

    private void confirmDelete(Map<String, Object> promo, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Promo")
                .setMessage("Delete \"" + promo.get("title") + "\"?")
                .setPositiveButton("Delete", (d, w) ->
                        firestoreDb.collection("Promotions").document((String) promo.get("id")).delete()
                                .addOnSuccessListener(unused -> {
                                    promos.remove(position);
                                    adapter.notifyItemRemoved(position);
                                }))
                .setNegativeButton("Cancel", null).show();
    }

    static class PromoAdapter extends RecyclerView.Adapter<PromoAdapter.PVH> {
        interface ToggleListener { void onToggle(Map<String, Object> p, int pos); }
        interface DeleteListener { void onDelete(Map<String, Object> p, int pos); }
        private final List<Map<String, Object>> list;
        private final ToggleListener toggleListener;
        private final DeleteListener deleteListener;
        PromoAdapter(List<Map<String, Object>> list, ToggleListener t, DeleteListener d) {
            this.list = list; this.toggleListener = t; this.deleteListener = d;
        }
        @Override public int getItemCount() { return list.size(); }
        @Override public PVH onCreateViewHolder(ViewGroup parent, int vt) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_promo_card, parent, false);
            return new PVH(v);
        }
        @Override public void onBindViewHolder(PVH h, int pos) {
            Map<String, Object> p = list.get(pos);
            h.tvTitle.setText((String) p.get("title"));
            h.tvSub.setText((String) p.get("subtitle"));
            boolean active = Boolean.TRUE.equals(p.get("active"));
            h.tvActive.setText(active ? "Active" : "Inactive");
            h.tvActive.setTextColor(active ? 0xFF2E7D32 : 0xFFB71C1C);
            h.btnToggle.setText(active ? "Deactivate" : "Activate");
            h.btnToggle.setOnClickListener(v -> toggleListener.onToggle(p, h.getAdapterPosition()));
            h.btnDeletePromo.setOnClickListener(v -> deleteListener.onDelete(p, h.getAdapterPosition()));
        }
        static class PVH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSub, tvActive;
            MaterialButton btnToggle, btnDeletePromo;
            PVH(View v) { super(v);
                tvTitle = v.findViewById(R.id.tvPromoTitleAdmin);
                tvSub   = v.findViewById(R.id.tvPromoSubAdmin);
                tvActive = v.findViewById(R.id.tvPromoActive);
                btnToggle = v.findViewById(R.id.btnTogglePromo);
                btnDeletePromo = v.findViewById(R.id.btnDeletePromo);
            }
        }
    }
}
