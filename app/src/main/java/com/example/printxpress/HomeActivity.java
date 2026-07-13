package com.example.printxpress;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessaging;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private static final String[][] DEFAULT_PRODUCTS = {
            {"business_cards",  "Business Cards", "15.0",  "Full-colour 350gsm business cards."},
            {"flyers",          "Flyers",          "20.0",  "A5 or A4 full-colour flyers."},
            {"banners",         "Banners",         "45.0",  "PVC vinyl banners, any size."},
            {"tshirts",         "T-Shirts",        "25.0",  "Custom printed cotton t-shirts."},
            {"stickers",        "Stickers",        "10.0",  "Waterproof vinyl sticker sheets."},
            {"mugs",            "Mugs",            "18.0",  "11oz ceramic custom photo mugs."},
            {"posters",         "Posters",         "30.0",  "Glossy A2/A1 poster prints."}
    };

    private DBHelper dbHelper;
    private FirebaseFirestore firestoreDb;
    private FirebaseUser currentUser;
    private TextView tvNotifBadge;

    private final ActivityResultLauncher<String> notifPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) refreshFcmToken();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        dbHelper    = new DBHelper(this);
        firestoreDb = FirebaseFirestore.getInstance();
        tvNotifBadge = findViewById(R.id.tvNotifBadge);

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        String displayName = currentUser.getDisplayName();
        tvGreeting.setText((displayName != null && !displayName.isEmpty())
                ? "Hi, " + displayName.split(" ")[0] + "!" : "Hi there!");

        syncProductsFromFirestore();
        loadPromoFromFirestore();
        wireUpCategoryCards();
        wireUpBottomNav();
        requestNotificationPermission();
        refreshFcmToken();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotifBadge();
    }

    private void syncProductsFromFirestore() {
        firestoreDb.collection("Products").get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        seedDefaultProducts();
                    } else {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            String id = doc.getId();
                            String category = doc.getString("category");
                            Double price = doc.getDouble("basePrice");
                            String desc = doc.getString("description");
                            if (category != null && price != null) {
                                dbHelper.upsertProduct(id, category, price, desc);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> seedDefaultProducts());
    }

    private void seedDefaultProducts() {
        List<Product> existing = dbHelper.getAllProducts();
        if (existing.isEmpty()) {
            for (String[] p : DEFAULT_PRODUCTS) {
                dbHelper.upsertProduct(p[0], p[1], Double.parseDouble(p[2]), p[3]);
                java.util.Map<String, Object> doc = new java.util.HashMap<>();
                doc.put("category", p[1]);
                doc.put("basePrice", Double.parseDouble(p[2]));
                doc.put("description", p[3]);
                firestoreDb.collection("Products").document(p[0]).set(doc);
            }
        }
    }

    private void loadPromoFromFirestore() {
        TextView tvPromoTitle = findViewById(R.id.tvPromoTitle);
        TextView tvPromoSub   = findViewById(R.id.tvPromoSub);
        firestoreDb.collection("Promotions")
                .whereEqualTo("active", true)
                .limit(1).get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        QueryDocumentSnapshot promo = (QueryDocumentSnapshot) snapshots.getDocuments().get(0);
                        String title = promo.getString("title");
                        String sub   = promo.getString("subtitle");
                        if (title != null) tvPromoTitle.setText(title);
                        if (sub != null) tvPromoSub.setText(sub);
                    }
                });
    }

    private void wireUpCategoryCards() {
        MaterialCardView cardBusinessCards = findViewById(R.id.cardBusinessCards);
        MaterialCardView cardFlyers        = findViewById(R.id.cardFlyers);
        MaterialCardView cardBanners       = findViewById(R.id.cardBanners);
        MaterialCardView cardTShirts       = findViewById(R.id.cardTShirts);

        cardBusinessCards.setOnClickListener(v -> openProductDetail("business_cards", "Business Cards", 15.0, "Full-colour 350gsm business cards."));
        cardFlyers.setOnClickListener(v -> openProductDetail("flyers", "Flyers", 20.0, "A5 or A4 full-colour flyers."));
        cardBanners.setOnClickListener(v -> openProductDetail("banners", "Banners", 45.0, "PVC vinyl banners, any size."));
        cardTShirts.setOnClickListener(v -> openProductDetail("tshirts", "T-Shirts", 25.0, "Custom printed cotton t-shirts."));
    }

    private void wireUpBottomNav() {
        LinearLayout navMyOrders = findViewById(R.id.navMyOrders);
        LinearLayout navProfile  = findViewById(R.id.navProfile);
        LinearLayout navFaq      = findViewById(R.id.navFaq);
        LinearLayout navNotif    = findViewById(R.id.navNotifications);

        navMyOrders.setOnClickListener(v -> startActivity(new Intent(this, MyOrdersActivity.class)));
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        navFaq.setOnClickListener(v -> startActivity(new Intent(this, FAQActivity.class)));
        navNotif.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));
    }

    private void openProductDetail(String productId, String productName, double price, String desc) {
        Product cached = dbHelper.getProductById(productId);
        if (cached != null) {
            price = cached.getBasePrice();
            desc  = cached.getDescription();
        }
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID,   productId);
        intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME, productName);
        intent.putExtra(ProductDetailActivity.EXTRA_UNIT_PRICE,   price);
        intent.putExtra(ProductDetailActivity.EXTRA_DESCRIPTION,  desc);
        startActivity(intent);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void refreshFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            if (token != null) {
                MyFirebaseMessagingService.saveFcmTokenToFirestore(token);
            }
        });
    }

    private void updateNotifBadge() {
        int count = dbHelper.getUnreadNotificationCount(currentUser.getUid());
        if (count > 0) {
            tvNotifBadge.setVisibility(View.VISIBLE);
            tvNotifBadge.setText(count > 9 ? "9+" : String.valueOf(count));
        } else {
            tvNotifBadge.setVisibility(View.GONE);
        }
    }
}
