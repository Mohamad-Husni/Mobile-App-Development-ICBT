package com.example.printxpress;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Random;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etResetEmail, etOtp, etNewPassword;
    private MaterialButton btnSendOtp, btnVerifyOtp, btnResetPassword;
    private LinearLayout layoutEmail, layoutOtp, layoutNewPassword;
    private TextView tvBackToLogin;
    private DBHelper dbHelper;
    private String generatedOtp;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        dbHelper = new DBHelper(this);

        // Layouts
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutOtp = findViewById(R.id.layoutOtp);
        layoutNewPassword = findViewById(R.id.layoutNewPassword);

        // Inputs
        etResetEmail = findViewById(R.id.etResetEmail);
        etOtp = findViewById(R.id.etOtp);
        etNewPassword = findViewById(R.id.etNewPassword);

        // Buttons
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // Step 1: Send OTP
        btnSendOtp.setOnClickListener(v -> {
            userEmail = etResetEmail.getText().toString().trim();

            if (TextUtils.isEmpty(userEmail)) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbHelper.checkEmail(userEmail)) {
                // Generate 4-digit OTP
                generatedOtp = String.format("%04d", new Random().nextInt(10000));
                
                // In a real app, you would send this via email API (e.g., SendGrid, Mailgun)
                // For this demo, we'll display it in a Toast and Logcat
                Toast.makeText(this, "OTP Sent! (MOCK OTP: " + generatedOtp + ")", Toast.LENGTH_LONG).show();
                
                layoutEmail.setVisibility(View.GONE);
                layoutOtp.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Email not found in our records", Toast.LENGTH_SHORT).show();
            }
        });

        // Step 2: Verify OTP
        btnVerifyOtp.setOnClickListener(v -> {
            String enteredOtp = etOtp.getText().toString().trim();

            if (TextUtils.isEmpty(enteredOtp)) {
                Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show();
                return;
            }

            if (enteredOtp.equals(generatedOtp)) {
                Toast.makeText(this, "OTP Verified", Toast.LENGTH_SHORT).show();
                layoutOtp.setVisibility(View.GONE);
                layoutNewPassword.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });

        // Step 3: Reset Password
        btnResetPassword.setOnClickListener(v -> {
            String newPassword = etNewPassword.getText().toString().trim();

            if (TextUtils.isEmpty(newPassword)) {
                Toast.makeText(this, "Please enter a new password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isUpdated = dbHelper.updatePassword(userEmail, newPassword);
            if (isUpdated) {
                Toast.makeText(this, "Password reset successful! Please login.", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Error updating password. Try again.", Toast.LENGTH_SHORT).show();
            }
        });

        tvBackToLogin.setOnClickListener(v -> finish());
    }
}
