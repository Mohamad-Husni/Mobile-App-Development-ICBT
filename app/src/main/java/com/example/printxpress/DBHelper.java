package com.example.printxpress;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "PrintXpress.db";
    private static final int DATABASE_VERSION = 9;

    // --- Table names ---
    public static final String TABLE_CUSTOMERS   = "customers";
    public static final String TABLE_PRODUCTS    = "products";
    public static final String TABLE_ORDERS      = "orders";
    public static final String TABLE_ORDER_ITEMS = "order_items";
    public static final String TABLE_NOTIFICATIONS = "notifications";

    // --- customers columns ---
    public static final String C_CUSTOMER_ID = "customer_id";
    public static final String C_NAME        = "name";
    public static final String C_EMAIL       = "email";
    public static final String C_PHONE       = "phone";
    public static final String C_ADDRESS     = "address";

    // --- products columns ---
    public static final String P_PRODUCT_ID  = "product_id";
    public static final String P_CATEGORY    = "category";
    public static final String P_BASE_PRICE  = "base_price";
    public static final String P_DESCRIPTION = "description";
    public static final String P_SYNCED_AT   = "synced_at";

    // --- orders columns ---
    public static final String O_ORDER_ID         = "order_id";
    public static final String O_FIREBASE_ORDER_ID = "firebase_order_id";
    public static final String O_CUSTOMER_ID      = "customer_id";
    public static final String O_ORDER_DATE       = "order_date";
    public static final String O_DELIVERY_TYPE    = "delivery_type";
    public static final String O_STATUS           = "status";
    public static final String O_TOTAL_PRICE      = "total_price";

    // --- order_items columns ---
    public static final String I_ITEM_ID          = "item_id";
    public static final String I_ORDER_ID         = "order_id";
    public static final String I_FIREBASE_ITEM_ID = "firebase_item_id";
    public static final String I_PRODUCT_ID       = "product_id";
    public static final String I_QUANTITY         = "quantity";
    public static final String I_ARTWORK_FILE_NAME = "artwork_file_name";
    public static final String I_UNIT_PRICE       = "unit_price";

    // --- notifications columns ---
    public static final String N_NOTIF_ID     = "notif_id";
    public static final String N_CUSTOMER_ID  = "customer_id";
    public static final String N_TYPE         = "type";
    public static final String N_MESSAGE      = "message";
    public static final String N_IS_READ      = "is_read";
    public static final String N_CREATED_AT   = "created_at";

    // --- Status constants ---
    public static final String STATUS_PROCESSING = "Processing";
    public static final String STATUS_PRINTING   = "Printing";
    public static final String STATUS_READY      = "Ready";
    public static final String STATUS_CANCELLED  = "Cancelled";

    // --- Notification type constants ---
    public static final String NOTIF_CONFIRMATION = "order_confirmation";
    public static final String NOTIF_COMPLETION   = "order_completion";
    public static final String NOTIF_PROMO        = "promo";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_CUSTOMERS + " ("
                + C_CUSTOMER_ID + " TEXT PRIMARY KEY, "
                + C_NAME        + " TEXT NOT NULL, "
                + C_EMAIL       + " TEXT NOT NULL, "
                + C_PHONE       + " TEXT, "
                + C_ADDRESS     + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_PRODUCTS + " ("
                + P_PRODUCT_ID  + " TEXT PRIMARY KEY, "
                + P_CATEGORY    + " TEXT NOT NULL, "
                + P_BASE_PRICE  + " REAL NOT NULL, "
                + P_DESCRIPTION + " TEXT, "
                + P_SYNCED_AT   + " DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE " + TABLE_ORDERS + " ("
                + O_ORDER_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + O_FIREBASE_ORDER_ID + " TEXT, "
                + O_CUSTOMER_ID       + " TEXT NOT NULL, "
                + O_ORDER_DATE        + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + O_DELIVERY_TYPE     + " TEXT NOT NULL, "
                + O_STATUS            + " TEXT NOT NULL DEFAULT '" + STATUS_PROCESSING + "', "
                + O_TOTAL_PRICE       + " REAL NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_ORDER_ITEMS + " ("
                + I_ITEM_ID           + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + I_ORDER_ID          + " INTEGER NOT NULL, "
                + I_FIREBASE_ITEM_ID  + " TEXT, "
                + I_PRODUCT_ID        + " TEXT NOT NULL, "
                + I_QUANTITY          + " INTEGER NOT NULL, "
                + I_ARTWORK_FILE_NAME + " TEXT, "
                + I_UNIT_PRICE        + " REAL NOT NULL)");

        db.execSQL("CREATE TABLE " + TABLE_NOTIFICATIONS + " ("
                + N_NOTIF_ID    + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + N_CUSTOMER_ID + " TEXT NOT NULL, "
                + N_TYPE        + " TEXT NOT NULL, "
                + N_MESSAGE     + " TEXT NOT NULL, "
                + N_IS_READ     + " INTEGER NOT NULL DEFAULT 0, "
                + N_CREATED_AT  + " DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDER_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CUSTOMERS);
        db.execSQL("DROP TABLE IF EXISTS orders");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    // ===================== CUSTOMERS =====================

    public void upsertCustomer(String customerId, String name, String email, String phone, String address) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues v = new ContentValues();
            v.put(C_CUSTOMER_ID, customerId);
            v.put(C_NAME, name);
            v.put(C_EMAIL, email);
            v.put(C_PHONE, phone != null ? phone : "");
            v.put(C_ADDRESS, address != null ? address : "");
            db.insertWithOnConflict(TABLE_CUSTOMERS, null, v, SQLiteDatabase.CONFLICT_REPLACE);
        } finally {
            db.close();
        }
    }

    public Customer getCustomer(String customerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(TABLE_CUSTOMERS, null, C_CUSTOMER_ID + "=?",
                    new String[]{customerId}, null, null, null);
            if (c.moveToFirst()) {
                Customer customer = new Customer();
                customer.setCustomerId(c.getString(c.getColumnIndexOrThrow(C_CUSTOMER_ID)));
                customer.setName(c.getString(c.getColumnIndexOrThrow(C_NAME)));
                customer.setEmail(c.getString(c.getColumnIndexOrThrow(C_EMAIL)));
                customer.setPhone(c.getString(c.getColumnIndexOrThrow(C_PHONE)));
                customer.setAddress(c.getString(c.getColumnIndexOrThrow(C_ADDRESS)));
                return customer;
            }
        } finally {
            if (c != null) c.close();
            db.close();
        }
        return null;
    }

    public boolean updateCustomerAddress(String customerId, String address) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues v = new ContentValues();
            v.put(C_ADDRESS, address);
            return db.update(TABLE_CUSTOMERS, v, C_CUSTOMER_ID + "=?",
                    new String[]{customerId}) > 0;
        } finally {
            db.close();
        }
    }

    // ===================== PRODUCTS =====================

    public void upsertProduct(String productId, String category, double basePrice, String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues v = new ContentValues();
            v.put(P_PRODUCT_ID, productId);
            v.put(P_CATEGORY, category);
            v.put(P_BASE_PRICE, basePrice);
            v.put(P_DESCRIPTION, description != null ? description : "");
            db.insertWithOnConflict(TABLE_PRODUCTS, null, v, SQLiteDatabase.CONFLICT_REPLACE);
        } finally {
            db.close();
        }
    }

    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(TABLE_PRODUCTS, null, null, null, null, null, P_CATEGORY + " ASC");
            if (c.moveToFirst()) {
                do {
                    Product p = new Product();
                    p.setProductId(c.getString(c.getColumnIndexOrThrow(P_PRODUCT_ID)));
                    p.setCategory(c.getString(c.getColumnIndexOrThrow(P_CATEGORY)));
                    p.setBasePrice(c.getDouble(c.getColumnIndexOrThrow(P_BASE_PRICE)));
                    p.setDescription(c.getString(c.getColumnIndexOrThrow(P_DESCRIPTION)));
                    list.add(p);
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
            db.close();
        }
        return list;
    }

    public Product getProductById(String productId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(TABLE_PRODUCTS, null, P_PRODUCT_ID + "=?",
                    new String[]{productId}, null, null, null);
            if (c.moveToFirst()) {
                Product p = new Product();
                p.setProductId(c.getString(c.getColumnIndexOrThrow(P_PRODUCT_ID)));
                p.setCategory(c.getString(c.getColumnIndexOrThrow(P_CATEGORY)));
                p.setBasePrice(c.getDouble(c.getColumnIndexOrThrow(P_BASE_PRICE)));
                p.setDescription(c.getString(c.getColumnIndexOrThrow(P_DESCRIPTION)));
                return p;
            }
        } finally {
            if (c != null) c.close();
            db.close();
        }
        return null;
    }

    // ===================== ORDERS =====================

    public long insertOrder(String customerId, String deliveryType, double totalPrice, String firebaseOrderId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues v = new ContentValues();
            v.put(O_CUSTOMER_ID, customerId);
            v.put(O_DELIVERY_TYPE, deliveryType);
            v.put(O_TOTAL_PRICE, totalPrice);
            v.put(O_STATUS, STATUS_PROCESSING);
            v.put(O_FIREBASE_ORDER_ID, firebaseOrderId != null ? firebaseOrderId : "");
            return db.insert(TABLE_ORDERS, null, v);
        } finally {
            db.close();
        }
    }

    public boolean updateOrderFirebaseId(long localOrderId, String firebaseOrderId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues v = new ContentValues();
            v.put(O_FIREBASE_ORDER_ID, firebaseOrderId);
            return db.update(TABLE_ORDERS, v, O_ORDER_ID + "=?",
                    new String[]{String.valueOf(localOrderId)}) > 0;
        } finally {
            db.close();
        }
    }

    public boolean updateOrderStatus(long localOrderId, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues v = new ContentValues();
            v.put(O_STATUS, status);
            return db.update(TABLE_ORDERS, v, O_ORDER_ID + "=?",
                    new String[]{String.valueOf(localOrderId)}) > 0;
        } finally {
            db.close();
        }
    }

    public boolean updateOrderStatusByFirebaseId(String firebaseOrderId, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues v = new ContentValues();
            v.put(O_STATUS, status);
            return db.update(TABLE_ORDERS, v, O_FIREBASE_ORDER_ID + "=?",
                    new String[]{firebaseOrderId}) > 0;
        } finally {
            db.close();
        }
    }

    public List<Order> getOrdersForCustomer(String customerId) {
        List<Order> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(TABLE_ORDERS, null, O_CUSTOMER_ID + "=?",
                    new String[]{customerId}, null, null, O_ORDER_DATE + " DESC");
            if (c.moveToFirst()) {
                do {
                    list.add(cursorToOrder(c));
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
            db.close();
        }
        return list;
    }

    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(TABLE_ORDERS, null, null, null, null, null, O_ORDER_DATE + " DESC");
            if (c.moveToFirst()) {
                do {
                    list.add(cursorToOrder(c));
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
            db.close();
        }
        return list;
    }

    private Order cursorToOrder(Cursor c) {
        Order o = new Order();
        o.setLocalId(c.getLong(c.getColumnIndexOrThrow(O_ORDER_ID)));
        o.setFirebaseOrderId(c.getString(c.getColumnIndexOrThrow(O_FIREBASE_ORDER_ID)));
        o.setCustomerId(c.getString(c.getColumnIndexOrThrow(O_CUSTOMER_ID)));
        o.setOrderDate(c.getString(c.getColumnIndexOrThrow(O_ORDER_DATE)));
        o.setDeliveryType(c.getString(c.getColumnIndexOrThrow(O_DELIVERY_TYPE)));
        o.setStatus(c.getString(c.getColumnIndexOrThrow(O_STATUS)));
        o.setTotalPrice(c.getDouble(c.getColumnIndexOrThrow(O_TOTAL_PRICE)));
        return o;
    }

    // ===================== ORDER ITEMS =====================

    public long insertOrderItem(long localOrderId, String productId, int quantity,
                                double unitPrice, String artworkFileName, String firebaseItemId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues v = new ContentValues();
            v.put(I_ORDER_ID, localOrderId);
            v.put(I_PRODUCT_ID, productId);
            v.put(I_QUANTITY, quantity);
            v.put(I_UNIT_PRICE, unitPrice);
            v.put(I_ARTWORK_FILE_NAME, artworkFileName != null ? artworkFileName : "");
            v.put(I_FIREBASE_ITEM_ID, firebaseItemId != null ? firebaseItemId : "");
            return db.insert(TABLE_ORDER_ITEMS, null, v);
        } finally {
            db.close();
        }
    }

    public List<OrderItem> getItemsForOrder(long localOrderId) {
        List<OrderItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(TABLE_ORDER_ITEMS, null, I_ORDER_ID + "=?",
                    new String[]{String.valueOf(localOrderId)}, null, null, null);
            if (c.moveToFirst()) {
                do {
                    OrderItem item = new OrderItem();
                    item.setItemId(c.getLong(c.getColumnIndexOrThrow(I_ITEM_ID)));
                    item.setOrderId(c.getLong(c.getColumnIndexOrThrow(I_ORDER_ID)));
                    item.setFirebaseItemId(c.getString(c.getColumnIndexOrThrow(I_FIREBASE_ITEM_ID)));
                    item.setProductId(c.getString(c.getColumnIndexOrThrow(I_PRODUCT_ID)));
                    item.setQuantity(c.getInt(c.getColumnIndexOrThrow(I_QUANTITY)));
                    item.setUnitPrice(c.getDouble(c.getColumnIndexOrThrow(I_UNIT_PRICE)));
                    item.setArtworkFileName(c.getString(c.getColumnIndexOrThrow(I_ARTWORK_FILE_NAME)));
                    list.add(item);
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
            db.close();
        }
        return list;
    }

    // ===================== NOTIFICATIONS =====================

    public long insertNotification(String customerId, String type, String message) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            Cursor c = db.query(TABLE_NOTIFICATIONS,
                    new String[]{"COUNT(*)"},
                    N_CUSTOMER_ID + "=? AND " + N_MESSAGE + "=?",
                    new String[]{customerId, message}, null, null, null);
            boolean exists = false;
            if (c != null) { if (c.moveToFirst()) exists = c.getInt(0) > 0; c.close(); }
            if (exists) return -1;

            ContentValues v = new ContentValues();
            v.put(N_CUSTOMER_ID, customerId);
            v.put(N_TYPE, type);
            v.put(N_MESSAGE, message);
            v.put(N_IS_READ, 0);
            return db.insert(TABLE_NOTIFICATIONS, null, v);
        } finally {
            db.close();
        }
    }

    public List<Notification> getNotificationsForCustomer(String customerId) {
        List<Notification> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(TABLE_NOTIFICATIONS, null, N_CUSTOMER_ID + "=?",
                    new String[]{customerId}, null, null, N_CREATED_AT + " DESC");
            if (c.moveToFirst()) {
                do {
                    Notification n = new Notification();
                    n.setNotifId(c.getLong(c.getColumnIndexOrThrow(N_NOTIF_ID)));
                    n.setCustomerId(c.getString(c.getColumnIndexOrThrow(N_CUSTOMER_ID)));
                    n.setType(c.getString(c.getColumnIndexOrThrow(N_TYPE)));
                    n.setMessage(c.getString(c.getColumnIndexOrThrow(N_MESSAGE)));
                    n.setRead(c.getInt(c.getColumnIndexOrThrow(N_IS_READ)) == 1);
                    n.setCreatedAt(c.getString(c.getColumnIndexOrThrow(N_CREATED_AT)));
                    list.add(n);
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
            db.close();
        }
        return list;
    }

    public int getUnreadNotificationCount(String customerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(TABLE_NOTIFICATIONS, new String[]{"COUNT(*)"},
                    N_CUSTOMER_ID + "=? AND " + N_IS_READ + "=0",
                    new String[]{customerId}, null, null, null);
            if (c.moveToFirst()) return c.getInt(0);
        } finally {
            if (c != null) c.close();
            db.close();
        }
        return 0;
    }

    public void markAllNotificationsRead(String customerId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues v = new ContentValues();
            v.put(N_IS_READ, 1);
            db.update(TABLE_NOTIFICATIONS, v, N_CUSTOMER_ID + "=?", new String[]{customerId});
        } finally {
            db.close();
        }
    }
}
