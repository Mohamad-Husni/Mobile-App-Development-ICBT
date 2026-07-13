package com.example.printxpress;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID   = "extra_product_id";
    public static final String EXTRA_PRODUCT_NAME = "extra_product_name";
    public static final String EXTRA_UNIT_PRICE   = "extra_unit_price";
    public static final String EXTRA_DESCRIPTION  = "extra_description";

    private String productId;
    private String productName;
    private double unitPrice;
    private String artworkFileName = "";

    private TextInputEditText etQuantity;
    private TextView tvSubtotal;
    private TextView tvArtworkLabel;
    private TextView tvCartBadge;

    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    artworkFileName = uri.getLastPathSegment();
                    if (artworkFileName == null || artworkFileName.isEmpty()) artworkFileName = "artwork_file";
                    tvArtworkLabel.setText(artworkFileName);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        productId   = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        productName = getIntent().getStringExtra(EXTRA_PRODUCT_NAME);
        unitPrice   = getIntent().getDoubleExtra(EXTRA_UNIT_PRICE, 0.0);
        String description = getIntent().getStringExtra(EXTRA_DESCRIPTION);

        ((TextView) findViewById(R.id.tvProductCategory)).setText("PRINT PRODUCT");
        ((TextView) findViewById(R.id.tvProductName)).setText(productName);
        ((TextView) findViewById(R.id.tvProductPrice)).setText(
                String.format("From LKR %.2f / unit", unitPrice));
        ((TextView) findViewById(R.id.tvDescription)).setText(
                description != null && !description.isEmpty() ? description
                        : "High quality printing on premium materials.");

        etQuantity    = findViewById(R.id.etQuantity);
        tvSubtotal    = findViewById(R.id.tvSubtotal);
        tvArtworkLabel = findViewById(R.id.tvArtworkLabel);
        tvCartBadge   = findViewById(R.id.tvCartBadge);

        updateSubtotal();
        updateCartBadge();

        etQuantity.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateSubtotal(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        ((MaterialCardView) findViewById(R.id.cardUploadArtwork)).setOnClickListener(v ->
                filePickerLauncher.launch(new String[]{"image/*", "application/pdf"}));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.ivCartIcon).setOnClickListener(v ->
                startActivity(new Intent(this, CartActivity.class)));
        findViewById(R.id.btnAddToCart).setOnClickListener(v -> addToCart());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
    }

    private void updateSubtotal() {
        int qty = getQty();
        tvSubtotal.setText(String.format("Subtotal: LKR %.2f", qty * unitPrice));
    }

    private int getQty() {
        try {
            int q = Integer.parseInt(
                    etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "1");
            return q > 0 ? q : 1;
        } catch (NumberFormatException e) { return 1; }
    }

    private void updateCartBadge() {
        int count = CartManager.getInstance().getCount();
        if (count > 0) {
            tvCartBadge.setVisibility(android.view.View.VISIBLE);
            tvCartBadge.setText(String.valueOf(count));
        } else {
            tvCartBadge.setVisibility(android.view.View.GONE);
        }
    }

    private void addToCart() {
        Product product = new Product();
        product.setProductId(productId != null ? productId : productName);
        product.setCategory(productName);
        product.setBasePrice(unitPrice);

        CartItem cartItem = new CartItem(product, getQty(), artworkFileName);
        CartManager.getInstance().addItem(cartItem);
        updateCartBadge();
        Toast.makeText(this, productName + " added to cart!", Toast.LENGTH_SHORT).show();
    }
}
