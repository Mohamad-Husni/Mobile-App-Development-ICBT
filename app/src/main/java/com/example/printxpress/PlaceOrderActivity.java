package com.example.printxpress;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class PlaceOrderActivity extends AppCompatActivity {

    private Spinner spinnerMaterial;
    private TextInputEditText etQuantity;
    private TextView tvTotalPrice;
    private MaterialButton btnPlaceOrder;
    private DBHelper dbHelper;

    private static final double PRICE_PER_UNIT = 25.0; // Base price for calculation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_order);

        dbHelper = new DBHelper(this);

        spinnerMaterial = findViewById(R.id.spinnerMaterial);
        etQuantity = findViewById(R.id.etQuantity);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);

        // Dynamic price calculation
        etQuantity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotal();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnPlaceOrder.setOnClickListener(v -> {
            String material = spinnerMaterial.getSelectedItem().toString();
            String quantityStr = etQuantity.getText().toString().trim();

            if (TextUtils.isEmpty(quantityStr)) {
                etQuantity.setError("Enter quantity");
                return;
            }

            int quantity = Integer.parseInt(quantityStr);
            double total = quantity * PRICE_PER_UNIT;

            boolean isInserted = dbHelper.insertOrder(material, quantity, total);
            if (isInserted) {
                Toast.makeText(PlaceOrderActivity.this, "Order Placed Successfully!", Toast.LENGTH_LONG).show();
                finish(); // Close activity
            } else {
                Toast.makeText(PlaceOrderActivity.this, "Order Failed. Try Again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateTotal() {
        String qty = etQuantity.getText().toString();
        if (!TextUtils.isEmpty(qty)) {
            try {
                int quantity = Integer.parseInt(qty);
                double total = quantity * PRICE_PER_UNIT;
                tvTotalPrice.setText(String.format("LKR %.2f", total));
            } catch (NumberFormatException e) {
                tvTotalPrice.setText("LKR 0.00");
            }
        } else {
            tvTotalPrice.setText("LKR 0.00");
        }
    }
}