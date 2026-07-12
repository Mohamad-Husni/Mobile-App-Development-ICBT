package com.example.printxpress;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private static final String ROLE_CUSTOMER   = "customer";
    private static final String ROLE_OPERATOR   = "operator";
    private static final String ROLE_ADMIN      = "admin";
    static final String         ADMIN_EMAIL     = "admin@printxpress.com";
    private static final String ADMIN_PASSWORD  = "Admin@1234";

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegisterLink, tvRoleHint, tvErrorMessage;
    private TextView tabCustomer, tabOperator, tabAdmin;
    private LinearLayout errorBanner;
    private View adminCredCard;
    private LinearProgressIndicator progressIndicator;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String selectedRole = ROLE_CUSTOMER;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {});

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
        requestNotificationPermissionIfNeeded();

        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        btnLogin          = findViewById(R.id.btnLogin);
        tvRegisterLink    = findViewById(R.id.tvRegisterLink);
        progressIndicator = findViewById(R.id.progressIndicator);
        tvRoleHint        = findViewById(R.id.tvRoleHint);
        tvErrorMessage    = findViewById(R.id.tvErrorMessage);
        errorBanner       = findViewById(R.id.errorBanner);
        tabCustomer       = findViewById(R.id.tabCustomer);
        tabOperator       = findViewById(R.id.tabOperator);
        tabAdmin          = findViewById(R.id.tabAdmin);
        adminCredCard     = findViewById(R.id.adminCredCard);

        tabCustomer.setOnClickListener(v -> selectRole(ROLE_CUSTOMER));
        tabOperator.setOnClickListener(v -> selectRole(ROLE_OPERATOR));
        tabAdmin.setOnClickListener(v -> selectRole(ROLE_ADMIN));

        View btnSetupAdmin = findViewById(R.id.btnSetupAdmin);
        if (btnSetupAdmin != null) {
            btnSetupAdmin.setOnClickListener(v -> setupAdminAccount());
        }

        selectRole(ROLE_CUSTOMER);

        btnLogin.setOnClickListener(v -> {
            hideError();
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                showError("Please enter your email address.");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                showError("Please enter your password.");
                return;
            }
            performLogin(email, password);
        });

        tvRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v ->
                    showError("Enter your email above then tap Forgot Password to reset."));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            setLoading(true);
            resolveRoleAndRoute(currentUser.getUid());
        }
    }

    private void selectRole(String role) {
        selectedRole = role;
        hideError();

        tabCustomer.setBackgroundResource(R.drawable.bg_role_tab_unselected);
        tabOperator.setBackgroundResource(R.drawable.bg_role_tab_unselected);
        tabAdmin.setBackgroundResource(R.drawable.bg_role_tab_unselected);
        tabCustomer.setTextColor(getResources().getColor(R.color.text_secondary, null));
        tabOperator.setTextColor(getResources().getColor(R.color.text_secondary, null));
        tabAdmin.setTextColor(getResources().getColor(R.color.text_secondary, null));

        adminCredCard.setVisibility(View.GONE);
        switch (role) {
            case ROLE_CUSTOMER:
                tabCustomer.setBackgroundResource(R.drawable.bg_role_tab_selected);
                tabCustomer.setTextColor(getResources().getColor(R.color.white, null));
                tvRoleHint.setText("Browse products, place and track your orders");
                break;
            case ROLE_OPERATOR:
                tabOperator.setBackgroundResource(R.drawable.bg_role_tab_selected);
                tabOperator.setTextColor(getResources().getColor(R.color.white, null));
                tvRoleHint.setText("Manage the print queue and update order statuses");
                break;
            case ROLE_ADMIN:
                tabAdmin.setBackgroundResource(R.drawable.bg_role_tab_selected);
                tabAdmin.setTextColor(getResources().getColor(R.color.white, null));
                tvRoleHint.setText("Full control: products, users, promotions and orders");
                adminCredCard.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void performLogin(String email, String password) {
        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && task.getResult() != null
                            && task.getResult().getUser() != null) {
                        resolveRoleAndRoute(task.getResult().getUser().getUid());
                    } else {
                        setLoading(false);
                        String msg = task.getException() != null
                                ? friendlyError(task.getException().getMessage())
                                : "Login failed. Please try again.";
                        showError(msg);
                    }
                });
    }

    private String friendlyError(String raw) {
        if (raw == null) return "Login failed. Please try again.";
        if (raw.contains("no user record") || raw.contains("identifier"))
            return "No account found with this email. Please register first.";
        if (raw.contains("password is invalid") || raw.contains("wrong-password"))
            return "Incorrect password. Please try again.";
        if (raw.contains("network"))
            return "Network error. Please check your connection.";
        if (raw.contains("too-many-requests"))
            return "Too many failed attempts. Please wait a moment and try again.";
        return "Login failed: " + raw;
    }

    private void resolveRoleAndRoute(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    setLoading(false);
                    String role = (doc.exists() && doc.getString("role") != null)
                            ? doc.getString("role") : ROLE_CUSTOMER;

                    // If Admin tab selected but Firestore doc missing, write it now
                    if (!doc.exists() && ROLE_ADMIN.equals(selectedRole)) {
                        java.util.Map<String, Object> adminDoc = new java.util.HashMap<>();
                        adminDoc.put("uid", uid);
                        adminDoc.put("name", "Admin");
                        adminDoc.put("email", ADMIN_EMAIL);
                        adminDoc.put("role", ROLE_ADMIN);
                        adminDoc.put("createdAt", com.google.firebase.Timestamp.now());
                        db.collection("users").document(uid).set(adminDoc);
                        role = ROLE_ADMIN;
                    }

                    // Role mismatch check
                    if (!role.equals(selectedRole)) {
                        showError("This account is registered as \"" + role
                                + "\". Please select the \"" + role + "\" tab and try again.");
                        mAuth.signOut();
                        return;
                    }

                    Intent intent;
                    switch (role) {
                        case ROLE_ADMIN:
                            intent = new Intent(this, AdminDashboardActivity.class); break;
                        case ROLE_OPERATOR:
                            intent = new Intent(this, OperatorDashboardActivity.class); break;
                        default:
                            intent = new Intent(this, HomeActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError("Could not verify your role. Check your internet connection.");
                });
    }

    private void setupAdminAccount() {
        setLoading(true);
        hideError();
        // Try signing in first — if account exists, just write Firestore doc
        mAuth.signInWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    java.util.Map<String, Object> adminDoc = new java.util.HashMap<>();
                    adminDoc.put("uid", uid);
                    adminDoc.put("name", "Admin");
                    adminDoc.put("email", ADMIN_EMAIL);
                    adminDoc.put("role", ROLE_ADMIN);
                    adminDoc.put("createdAt", com.google.firebase.Timestamp.now());
                    db.collection("users").document(uid).set(adminDoc)
                            .addOnSuccessListener(v2 -> {
                                setLoading(false);
                                mAuth.signOut();
                                etEmail.setText(ADMIN_EMAIL);
                                etPassword.setText(ADMIN_PASSWORD);
                                showError("Admin account is ready! Tap Sign In now.");
                                tvErrorMessage.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
                            });
                })
                .addOnFailureListener(e -> {
                    // Account doesn't exist — create it
                    mAuth.createUserWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD)
                            .addOnSuccessListener(result -> {
                                String uid = result.getUser().getUid();
                                java.util.Map<String, Object> adminDoc = new java.util.HashMap<>();
                                adminDoc.put("uid", uid);
                                adminDoc.put("name", "Admin");
                                adminDoc.put("email", ADMIN_EMAIL);
                                adminDoc.put("role", ROLE_ADMIN);
                                adminDoc.put("createdAt", com.google.firebase.Timestamp.now());
                                db.collection("users").document(uid).set(adminDoc)
                                        .addOnSuccessListener(v2 -> {
                                            setLoading(false);
                                            mAuth.signOut();
                                            etEmail.setText(ADMIN_EMAIL);
                                            etPassword.setText(ADMIN_PASSWORD);
                                            showError("Admin account created! Tap Sign In now.");
                                            tvErrorMessage.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
                                        });
                            })
                            .addOnFailureListener(e2 -> {
                                setLoading(false);
                                showError("Setup failed: " + e2.getMessage());
                            });
                });
    }

    private void showError(String message) {
        tvErrorMessage.setText(message);
        errorBanner.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorBanner.setVisibility(View.GONE);
    }

    private void setLoading(boolean loading) {
        progressIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}
