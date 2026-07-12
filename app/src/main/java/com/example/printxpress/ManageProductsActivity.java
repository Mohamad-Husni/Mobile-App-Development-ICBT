package com.example.printxpress;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

public class ManageProductsActivity extends AppCompatActivity {

    private final List<Product> products = new ArrayList<>();
    private ProductAdminAdapter adapter;
    private FirebaseFirestore firestoreDb;
    private DBHelper dbHelper;
    private RecyclerView recyclerView;
    private CircularProgressIndicator progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_products);

        firestoreDb = FirebaseFirestore.getInstance();
        dbHelper    = new DBHelper(this);

        recyclerView = findViewById(R.id.recyclerProducts);
        progress     = findViewById(R.id.progressIndicator);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ProductAdminAdapter(products,
                this::showEditDialog,
                this::confirmDelete);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddProduct).setOnClickListener(v -> showAddDialog());

        loadProducts();
    }

    private void loadProducts() {
        progress.setVisibility(View.VISIBLE);
        firestoreDb.collection("Products").get()
                .addOnSuccessListener(snapshots -> {
                    products.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Product p = new Product();
                        p.setProductId(doc.getId());
                        p.setCategory(doc.getString("category") != null ? doc.getString("category") : "");
                        Double price = doc.getDouble("basePrice");
                        p.setBasePrice(price != null ? price : 0.0);
                        p.setDescription(doc.getString("description") != null ? doc.getString("description") : "");
                        products.add(p);
                        dbHelper.upsertProduct(p.getProductId(), p.getCategory(), p.getBasePrice(), p.getDescription());
                    }
                    progress.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load products.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddDialog() {
        showProductDialog(null, -1);
    }

    private void showEditDialog(Product product, int position) {
        showProductDialog(product, position);
    }

    private void showProductDialog(Product existing, int position) {
        View dialogView = LayoutInflater.from(this).inflate(android.R.layout.activity_list_item, null);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(48, 24, 48, 8);

        EditText etName  = new EditText(this); etName.setHint("Product name");
        EditText etPrice = new EditText(this); etPrice.setHint("Base price"); etPrice.setInputType(android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_CLASS_NUMBER);
        EditText etDesc  = new EditText(this); etDesc.setHint("Description");

        if (existing != null) {
            etName.setText(existing.getCategory());
            etPrice.setText(String.valueOf(existing.getBasePrice()));
            etDesc.setText(existing.getDescription());
        }
        form.addView(etName); form.addView(etPrice); form.addView(etDesc);

        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Add Product" : "Edit Product")
                .setView(form)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String priceStr = etPrice.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    if (name.isEmpty() || priceStr.isEmpty()) {
                        Toast.makeText(this, "Name and price required.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double price = Double.parseDouble(priceStr);
                    if (existing == null) {
                        saveNewProduct(name, price, desc);
                    } else {
                        updateProduct(existing, name, price, desc, position);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveNewProduct(String name, double price, String desc) {
        Map<String, Object> data = new HashMap<>();
        data.put("category", name); data.put("basePrice", price); data.put("description", desc);
        firestoreDb.collection("Products").add(data)
                .addOnSuccessListener(ref -> {
                    Product p = new Product();
                    p.setProductId(ref.getId()); p.setCategory(name); p.setBasePrice(price); p.setDescription(desc);
                    products.add(p);
                    dbHelper.upsertProduct(ref.getId(), name, price, desc);
                    adapter.notifyItemInserted(products.size() - 1);
                    Toast.makeText(this, "Product added.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProduct(Product existing, String name, double price, String desc, int position) {
        Map<String, Object> data = new HashMap<>();
        data.put("category", name); data.put("basePrice", price); data.put("description", desc);
        firestoreDb.collection("Products").document(existing.getProductId()).update(data)
                .addOnSuccessListener(unused -> {
                    existing.setCategory(name); existing.setBasePrice(price); existing.setDescription(desc);
                    dbHelper.upsertProduct(existing.getProductId(), name, price, desc);
                    adapter.notifyItemChanged(position);
                    Toast.makeText(this, "Product updated.", Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmDelete(Product product, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Delete \"" + product.getCategory() + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    firestoreDb.collection("Products").document(product.getProductId()).delete()
                            .addOnSuccessListener(unused -> {
                                products.remove(position);
                                adapter.notifyItemRemoved(position);
                                Toast.makeText(this, "Deleted.", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    static class ProductAdminAdapter extends RecyclerView.Adapter<ProductAdminAdapter.PVH> {
        interface EditListener { void onEdit(Product p, int pos); }
        interface DeleteListener { void onDelete(Product p, int pos); }
        private final List<Product> list;
        private final EditListener editListener;
        private final DeleteListener deleteListener;

        ProductAdminAdapter(List<Product> list, EditListener e, DeleteListener d) {
            this.list = list; this.editListener = e; this.deleteListener = d;
        }
        @Override public int getItemCount() { return list.size(); }
        @Override public PVH onCreateViewHolder(ViewGroup parent, int vt) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_product_card, parent, false);
            return new PVH(v);
        }
        @Override public void onBindViewHolder(PVH h, int pos) {
            Product p = list.get(pos);
            h.tvName.setText(p.getCategory());
            h.tvPrice.setText(String.format("$%.2f", p.getBasePrice()));
            h.tvDesc.setText(p.getDescription());
            h.btnEdit.setOnClickListener(v -> editListener.onEdit(p, h.getAdapterPosition()));
            h.btnDelete.setOnClickListener(v -> deleteListener.onDelete(p, h.getAdapterPosition()));
        }
        static class PVH extends RecyclerView.ViewHolder {
            TextView tvName, tvPrice, tvDesc;
            MaterialButton btnEdit, btnDelete;
            PVH(View v) { super(v);
                tvName = v.findViewById(R.id.tvAdminProductName);
                tvPrice = v.findViewById(R.id.tvAdminProductPrice);
                tvDesc = v.findViewById(R.id.tvAdminProductDesc);
                btnEdit = v.findViewById(R.id.btnEditProduct);
                btnDelete = v.findViewById(R.id.btnDeleteProduct);
            }
        }
    }
}
