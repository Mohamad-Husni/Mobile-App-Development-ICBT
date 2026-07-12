package com.example.printxpress;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
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

public class ManageUsersActivity extends AppCompatActivity {

    private final List<Map<String, String>> users = new ArrayList<>();
    private UserAdminAdapter adapter;
    private FirebaseFirestore firestoreDb;
    private RecyclerView recyclerView;
    private CircularProgressIndicator progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_manage_users);
        firestoreDb = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recyclerUsers);
        progress     = findViewById(R.id.progressIndicator);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdminAdapter(users, this::showRoleDialog);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddOperator).setOnClickListener(v -> showAddOperatorInfo());
        loadUsers();
    }

    private void loadUsers() {
        progress.setVisibility(View.VISIBLE);
        firestoreDb.collection("users").get()
                .addOnSuccessListener(snapshots -> {
                    users.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Map<String, String> u = new HashMap<>();
                        u.put("uid", doc.getId());
                        u.put("name", doc.getString("name") != null ? doc.getString("name") : "");
                        u.put("email", doc.getString("email") != null ? doc.getString("email") : "");
                        u.put("role", doc.getString("role") != null ? doc.getString("role") : "customer");
                        users.add(u);
                    }
                    progress.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load users.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showRoleDialog(Map<String, String> user, int position) {
        String[] roles = {"customer", "operator", "admin"};
        new AlertDialog.Builder(this)
                .setTitle("Change Role: " + user.get("name"))
                .setItems(roles, (dialog, which) -> {
                    String newRole = roles[which];
                    firestoreDb.collection("users").document(user.get("uid"))
                            .update("role", newRole)
                            .addOnSuccessListener(unused -> {
                                user.put("role", newRole);
                                adapter.notifyItemChanged(position);
                                Toast.makeText(this, "Role updated to " + newRole, Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddOperatorInfo() {
        new AlertDialog.Builder(this)
                .setTitle("Add Operator")
                .setMessage("To add an operator:\n1. Ask them to register normally.\n2. Find them in this list.\n3. Change their role to 'operator'.")
                .setPositiveButton("OK", null)
                .show();
    }

    static class UserAdminAdapter extends RecyclerView.Adapter<UserAdminAdapter.UVH> {
        interface RoleListener { void onChangeRole(Map<String, String> user, int pos); }
        private final List<Map<String, String>> list;
        private final RoleListener listener;
        UserAdminAdapter(List<Map<String, String>> list, RoleListener l) { this.list = list; this.listener = l; }
        @Override public int getItemCount() { return list.size(); }
        @Override public UVH onCreateViewHolder(ViewGroup parent, int vt) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user_card, parent, false);
            return new UVH(v);
        }
        @Override public void onBindViewHolder(UVH h, int pos) {
            Map<String, String> u = list.get(pos);
            h.tvName.setText(u.get("name"));
            h.tvEmail.setText(u.get("email"));
            h.tvRole.setText(u.get("role"));
            h.btnChangeRole.setOnClickListener(v -> listener.onChangeRole(u, h.getAdapterPosition()));
        }
        static class UVH extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail, tvRole;
            MaterialButton btnChangeRole;
            UVH(View v) { super(v);
                tvName = v.findViewById(R.id.tvUserName);
                tvEmail = v.findViewById(R.id.tvUserEmail);
                tvRole = v.findViewById(R.id.tvUserRole);
                btnChangeRole = v.findViewById(R.id.btnChangeRole);
            }
        }
    }
}
