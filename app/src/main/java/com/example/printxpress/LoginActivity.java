package com.example.printxpress;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegisterLink, tvForgotPassword;
    private DBHelper dbHelper;
    private FirebaseAuth mAuth;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // Notifications are a nice-to-have; the app functions fine either way.
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DBHelper(this);
        mAuth = FirebaseAuth.getInstance();
        requestNotificationPermissionIfNeeded();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Please enter your email and password.", Toast.LENGTH_SHORT).show();
            } else {
                performLogin(email, password);
            }
        });

        tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateBasedOnRole(currentUser.getEmail());
        }
    }

    private void performLogin(String email, String password) {
        // Firebase Authentication Attempt
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Firebase Login Successful, now check role in local DB
                        navigateBasedOnRole(email);
                    } else {
                        // Fallback to Local SQLite Login for University Assignment flexibility
                        String role = dbHelper.loginUser(email, password);
                        if (role != null) {
                            navigate(role);
                        } else {
                            Toast.makeText(LoginActivity.this, "Incorrect email or password. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void navigateBasedOnRole(String email) {
        String role = dbHelper.getUserRole(email);
        if (role != null) {
            navigate(role);
        } else {
            // Default to Customer if role not found locally
            startActivity(new Intent(LoginActivity.this, CustomerDashboardActivity.class));
            finish();
        }
    }

    private void navigate(String role) {
        Intent intent;
        switch (role) {
            case DBHelper.ROLE_ADMIN:
                intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                break;
            case DBHelper.ROLE_OPERATOR:
                intent = new Intent(LoginActivity.this, OperatorDashboardActivity.class);
                break;
            case DBHelper.ROLE_CUSTOMER:
            default:
                intent = new Intent(LoginActivity.this, CustomerDashboardActivity.class);
                break;
        }
        startActivity(intent);
        finish();
    }
}
