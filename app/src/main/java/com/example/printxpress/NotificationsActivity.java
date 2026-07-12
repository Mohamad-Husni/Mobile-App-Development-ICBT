package com.example.printxpress;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }

        DBHelper dbHelper = new DBHelper(this);
        String uid = user.getUid();

        dbHelper.markAllNotificationsRead(uid);

        List<Notification> notifications = dbHelper.getNotificationsForCustomer(uid);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewNotif);
        LinearLayout emptyView    = findViewById(R.id.emptyNotif);

        if (notifications.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(new NotificationAdapter(notifications));
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
