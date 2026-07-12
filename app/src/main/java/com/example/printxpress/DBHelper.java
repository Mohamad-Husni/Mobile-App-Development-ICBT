package com.example.printxpress;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "PrintXpress.db";
    private static final int DATABASE_VERSION = 7;

    // User Roles
    public static final String ROLE_CUSTOMER = "Customer";
    public static final String ROLE_OPERATOR = "Print-Operator";
    public static final String ROLE_ADMIN = "Admin";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT, "
                + "email TEXT UNIQUE, "
                + "phone TEXT, "
                + "password TEXT, "
                + "role TEXT)";

        String CREATE_ORDERS_TABLE = "CREATE TABLE orders ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "email TEXT, "
                + "total_price REAL, "
                + "status TEXT, "
                + "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";

        String CREATE_ORDER_ITEMS_TABLE = "CREATE TABLE order_items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "order_id INTEGER, "
                + "material_type TEXT, "
                + "quantity INTEGER, "
                + "unit_price REAL, "
                + "FOREIGN KEY(order_id) REFERENCES orders(id))";

        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_ORDERS_TABLE);
        db.execSQL(CREATE_ORDER_ITEMS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS order_items");
        db.execSQL("DROP TABLE IF EXISTS orders");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    // CRUD: Register User
    public boolean registerUser(String name, String email, String phone, String password, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("email", email);
            values.put("phone", phone);
            values.put("password", password);
            values.put("role", role);
            return db.insert("users", null, values) != -1;
        } finally {
            db.close();
        }
    }

    // CRUD: Login User (Returns role string)
    public String loginUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query("users", new String[]{"role"},
                    "email=? AND password=?", new String[]{email, password},
                    null, null, null);
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
            return null;
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    // Get User Role by Email
    public String getUserRole(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query("users", new String[]{"role"},
                    "email=?", new String[]{email},
                    null, null, null);
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
            return null;
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    // Check if an email exists (used by the password-reset flow)
    public boolean checkEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query("users", new String[]{"id"},
                    "email=?", new String[]{email},
                    null, null, null);
            return cursor.moveToFirst();
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    // Update a user's password (used by the password-reset flow)
    public boolean updatePassword(String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("password", newPassword);
            int rows = db.update("users", values, "email = ?", new String[]{email});
            return rows > 0;
        } finally {
            db.close();
        }
    }

    // CRUD: Insert Order Header
    public long insertOrder(String email, double totalPrice, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("email", email);
            values.put("total_price", totalPrice);
            values.put("status", status);
            return db.insert("orders", null, values);
        } finally {
            db.close();
        }
    }

    // CRUD: Insert Order Item
    public boolean insertOrderItem(long orderId, String material, int quantity, double unitPrice) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("order_id", orderId);
            values.put("material_type", material);
            values.put("quantity", quantity);
            values.put("unit_price", unitPrice);
            return db.insert("order_items", null, values) != -1;
        } finally {
            db.close();
        }
    }

    // Fetch all orders. Caller is responsible for closing the returned cursor.
    public Cursor getAllOrders() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM orders ORDER BY id DESC", null);
    }

    // Update order status
    public boolean updateOrderStatus(long orderId, String newStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("status", newStatus);
            int rows = db.update("orders", values, "id = ?", new String[]{String.valueOf(orderId)});
            return rows > 0;
        } finally {
            db.close();
        }
    }
}
