package com.example.printxpress;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        findViewById(R.id.cardManageProducts).setOnClickListener(v ->
                startActivity(new Intent(this, ManageProductsActivity.class)));
        findViewById(R.id.cardManageUsers).setOnClickListener(v ->
                startActivity(new Intent(this, ManageUsersActivity.class)));
        findViewById(R.id.cardManagePromos).setOnClickListener(v ->
                startActivity(new Intent(this, ManagePromosActivity.class)));
        findViewById(R.id.cardAllOrders).setOnClickListener(v ->
                startActivity(new Intent(this, AllOrdersActivity.class)));

        findViewById(R.id.btnSignOut).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
