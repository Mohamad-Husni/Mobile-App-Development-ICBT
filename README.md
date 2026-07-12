# PrintXpress — Android Application

> **Premium Printing Solutions — B2C Storefront with Role-Based Access**

A native Android application (Java) for a printing services business. Customers browse products, build a cart and place orders. Print Operators manage the fulfilment queue. Admins control the full back-office. All data is stored locally in SQLite and synchronised with Firebase Firestore in real time.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Tech Stack](#tech-stack)
3. [Credentials](#credentials)
4. [Site Map](#site-map)
5. [Feature List](#feature-list)
6. [Architecture Overview](#architecture-overview)
7. [ERD — Entity Relationship Diagram](#erd--entity-relationship-diagram)
8. [SQLite Schema](#sqlite-schema)
9. [Firestore Collections](#firestore-collections)
10. [UML Class Diagram](#uml-class-diagram)
11. [UML Use Case Diagram](#uml-use-case-diagram)
12. [UML Sequence Diagrams](#uml-sequence-diagrams)
13. [Role Routing Logic](#role-routing-logic)
14. [Ordering Flow](#ordering-flow)
15. [Operator Flow](#operator-flow)
16. [Admin Flow](#admin-flow)
17. [Project File Structure](#project-file-structure)
18. [Build & Run Instructions](#build--run-instructions)
19. [APK Installation](#apk-installation)
20. [Colour Palette](#colour-palette)

---

## Project Overview

PrintXpress is a cart-based B2C mobile storefront for a printing company. It supports three distinct user roles:

| Role | Access Level | Entry Point |
|---|---|---|
| **Customer** | Browse, cart, order, track | `HomeActivity` |
| **Print Operator** | View & update order queue | `OperatorDashboardActivity` |
| **Admin** | Full back-office control | `AdminDashboardActivity` |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java (Android SDK 33) |
| Min SDK | API 24 (Android 7.0) |
| UI Framework | Material Design 3 (Material Components 1.9) |
| Local Database | SQLite via `SQLiteOpenHelper` (v9) |
| Cloud Database | Firebase Firestore |
| Authentication | Firebase Authentication (email/password) |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| Build System | Gradle 8.5 with R8 minification |
| Architecture | Single-module, Activity-based MVC |

---

## Credentials

### Customer
Register a new account via the app's **Register** screen. All new accounts default to the `customer` role.

### Admin (Test)
| Field | Value |
|---|---|
| Email | `admin@printxpress.com` |
| Password | `Admin@1234` |
| Setup | Tap the **Admin** tab on the login screen, then tap **"Setup Admin Account"** once. This creates the Firebase Auth user and writes `role: "admin"` to Firestore automatically. |

### Print Operator
An Operator account must be created by the Admin:
1. Log in as Admin
2. Go to **Manage Users**
3. Tap **Add Operator** and enter the operator's email
4. The operator registers normally; Admin then changes their role to `operator` via **Manage Users → Change Role**

---

## Site Map

```
PrintXpress App
│
├── LoginActivity                  ← Launch screen
│   ├── [Customer tab]
│   ├── [Operator tab]
│   └── [Admin tab] → Setup Admin Account button
│
├── RegisterActivity               ← New customer sign-up
│
├── ── CUSTOMER ZONE ──────────────────────────────────────
│
├── HomeActivity                   ← Product grid + promo banner
│   ├── ProductDetailActivity      ← Full product page + Add to Cart
│   ├── CartActivity               ← Cart items, qty edit, checkout
│   ├── MyOrdersActivity           ← Order history + status tracking
│   ├── NotificationsActivity      ← Push notifications list
│   ├── ProfileActivity            ← Account info + address edit
│   └── FAQActivity                ← Frequently asked questions
│
├── ── OPERATOR ZONE ──────────────────────────────────────
│
└── OperatorDashboardActivity      ← Live order queue
│
├── ── ADMIN ZONE ─────────────────────────────────────────
│
└── AdminDashboardActivity         ← Back-office hub
    ├── ManageProductsActivity     ← CRUD products
    ├── ManageUsersActivity        ← View + role management
    ├── ManagePromosActivity       ← Promo banners CRUD
    └── AllOrdersActivity          ← Read-only all-orders view
```

---

## Feature List

### Customer Features
- **Register / Login** with email and password via Firebase Auth
- **Role-based login tabs** — select Customer / Operator / Admin before signing in
- **Inline error alerts** — no popup toasts; errors show inside the form card
- **Product browsing** — grid of product categories loaded from Firestore (cached in SQLite)
- **Product detail page** — full description, specs, price, Add to Cart button
- **Shopping cart** — add multiple items, adjust quantities, remove items, per-item artwork filename
- **Checkout** — choose Pickup or Home Delivery; order written to SQLite + Firestore simultaneously
- **Order history** — view all past orders with item count, total, status chip
- **Order cancellation** — cancel orders in `Processing` status
- **Push notifications** — order confirmation, order ready alerts, promo announcements
- **Notification list** — all notifications stored locally; unread badge on nav icon
- **Profile page** — view name, email, phone, delivery address; edit address
- **FAQ page** — static frequently asked questions
- **Password visibility toggle** — on all password fields

### Operator Features
- **Live order queue** — real-time Firestore listener shows all active orders
- **Status update** — tap any order card to update status (Processing → Printing → Ready → Cancelled)
- **Dual-write sync** — status change updates both Firestore and local SQLite
- **Customer notification trigger** — FCM push sent to customer on every status change

### Admin Features
- **Manage Products** — Add, Edit, Delete products in Firestore; synced to customer SQLite caches
- **Manage Users** — view all registered users, change user roles (customer ↔ operator)
- **Manage Promotions** — Add promo banners with discount %, start/end dates; toggle active state; delete
- **View All Orders** — read-only list of every order across all customers
- **Admin auto-setup** — one-tap admin account creation from the login screen

### Security & Performance
- **Role mismatch protection** — login fails if selected tab doesn't match Firestore role
- **R8 minification + resource shrinking** — release APK is ~4 MB
- **Firestore offline persistence** — app works with cached data when offline
- **No AI service dependencies** — fully self-contained; no third-party AI APIs

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                     Android App                         │
│                                                         │
│  Activities (View + Controller)                         │
│       │                                                 │
│       ├── DBHelper (SQLiteOpenHelper)  ← Local cache    │
│       │        SQLite: PrintXpress.db v9                │
│       │                                                 │
│       └── FirebaseFirestore            ← Cloud source   │
│                    │                                    │
│             FirebaseAuth               ← Auth           │
│             FirebaseMessaging          ← Push notifs    │
└─────────────────────────────────────────────────────────┘
         │                          │
    SQLite (device)           Firestore (cloud)
  5 tables, local first    Real-time sync, source of truth
```

**Data flow:** Firestore is the source of truth. On first load or sync, data is written to SQLite. All UI reads from SQLite for speed. All writes go to both SQLite and Firestore simultaneously (dual-write).

---

## ERD — Entity Relationship Diagram

```
┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│   Customer   │        │    Order     │        │  OrderItem   │
│──────────────│        │──────────────│        │──────────────│
│ customer_id  │──1───<>│ order_id     │──1───<>│ item_id      │
│ name         │        │ customer_id  │        │ order_id     │
│ email        │        │ order_date   │        │ product_id   │
│ phone        │        │ delivery_type│        │ quantity     │
│ address      │        │ status       │        │ unit_price   │
└──────────────┘        │ total_price  │        │ artwork_file │
                        └──────────────┘        └──────┬───────┘
                                                        │ N
                                                        │
                                               ┌────────┴─────┐
                                               │   Product    │
                                               │──────────────│
                                               │ product_id   │
                                               │ category     │
                                               │ base_price   │
                                               │ description  │
                                               └──────────────┘

┌──────────────┐        ┌──────────────┐
│ Notification │        │  Promotion   │
│──────────────│        │──────────────│
│ notif_id     │        │ promo_id     │
│ customer_id  │        │ title        │
│ type         │        │ discount_pct │
│ message      │        │ start_date   │
│ is_read      │        │ end_date     │
│ created_at   │        │ active       │
└──────────────┘        └──────────────┘

Relationships:
  Customer   1 ──── * Order
  Order      1 ──── * OrderItem
  Product    1 ──── * OrderItem
  Customer   1 ──── * Notification
```

---

## SQLite Schema

Database name: `PrintXpress.db` — Version: `9`

### Table: `customers`
| Column | Type | Constraints |
|---|---|---|
| `customer_id` | TEXT | PRIMARY KEY (= Firebase Auth UID) |
| `name` | TEXT | NOT NULL |
| `email` | TEXT | NOT NULL |
| `phone` | TEXT | |
| `address` | TEXT | |

### Table: `products`
| Column | Type | Constraints |
|---|---|---|
| `product_id` | TEXT | PRIMARY KEY (= Firestore doc ID) |
| `category` | TEXT | |
| `base_price` | REAL | |
| `description` | TEXT | |
| `synced_at` | DATETIME | |

### Table: `orders`
| Column | Type | Constraints |
|---|---|---|
| `order_id` | INTEGER | PRIMARY KEY AUTOINCREMENT |
| `firebase_order_id` | TEXT | |
| `customer_id` | TEXT | FOREIGN KEY → customers(customer_id) |
| `order_date` | DATETIME | |
| `delivery_type` | TEXT | `Pickup` or `Home Delivery` |
| `status` | TEXT | `Processing` / `Printing` / `Ready` / `Cancelled` |
| `total_price` | REAL | |

### Table: `order_items`
| Column | Type | Constraints |
|---|---|---|
| `item_id` | INTEGER | PRIMARY KEY AUTOINCREMENT |
| `order_id` | INTEGER | FOREIGN KEY → orders(order_id) |
| `firebase_item_id` | TEXT | |
| `product_id` | TEXT | FOREIGN KEY → products(product_id) |
| `quantity` | INTEGER | |
| `artwork_file_name` | TEXT | |
| `unit_price` | REAL | Snapshot at order time |

### Table: `notifications`
| Column | Type | Constraints |
|---|---|---|
| `notif_id` | INTEGER | PRIMARY KEY AUTOINCREMENT |
| `customer_id` | TEXT | |
| `type` | TEXT | `order_confirmation` / `order_completion` / `promo` |
| `message` | TEXT | |
| `is_read` | INTEGER | `0` = unread, `1` = read |
| `created_at` | DATETIME | |

---

## Firestore Collections

| Collection Path | Purpose | Key Fields |
|---|---|---|
| `users/{uid}` | User profile + role | `name, email, phone, address, role, fcmToken` |
| `Products/{id}` | Product catalogue | `category, basePrice, description, specs[]` |
| `Orders/{id}` | Order header | `customerId, orderDate, deliveryType, status, totalPrice` |
| `Orders/{id}/items/{itemId}` | Order line items | `productId, quantity, artworkFileName, unitPrice` |
| `Promotions/{id}` | Promo banners | `title, discountPct, startDate, endDate, active` |
| `Notifications/{id}` | Push notification log | `userId, type, message, isRead, createdAt` |

---

## UML Class Diagram

```
┌─────────────────────┐      ┌─────────────────────┐
│      Customer       │      │       Product        │
│─────────────────────│      │─────────────────────│
│ - customerId: String│      │ - productId: String  │
│ - name: String      │      │ - category: String   │
│ - email: String     │      │ - basePrice: double  │
│ - phone: String     │      │ - description: String│
│ - address: String   │      └─────────────────────┘
└─────────────────────┘                │
          │ 1                          │ 1
          │                            │
          │ *                          │ *
┌─────────────────────┐      ┌─────────────────────┐
│        Order        │      │      OrderItem       │
│─────────────────────│      │─────────────────────│
│ - orderId: int      │──1─*─│ - itemId: int        │
│ - firebaseOrderId   │      │ - orderId: int       │
│ - customerId: String│      │ - productId: String  │
│ - orderDate: String │      │ - quantity: int      │
│ - deliveryType      │      │ - artworkFileName    │
│ - status: String    │      │ - unitPrice: double  │
│ - totalPrice: double│      └─────────────────────┘
└─────────────────────┘

┌─────────────────────┐      ┌─────────────────────┐
│    Notification     │      │      CartItem        │
│─────────────────────│      │─────────────────────│
│ - notifId: int      │      │ - product: Product   │
│ - customerId: String│      │ - quantity: int      │
│ - type: String      │      │ - artworkFileName    │
│ - message: String   │      │ + getSubtotal()      │
│ - isRead: boolean   │      └─────────────────────┘
│ - createdAt: String │         (in-memory only)
└─────────────────────┘

┌─────────────────────┐      ┌─────────────────────┐
│      DBHelper       │      │   CartManager        │
│─────────────────────│      │─────────────────────│
│ + insertCustomer()  │      │ - items: List        │
│ + insertProduct()   │      │ + addItem()          │
│ + upsertProduct()   │      │ + removeItem()       │
│ + insertOrder()     │      │ + clear()            │
│ + getOrdersByUid()  │      │ + getTotal()         │
│ + insertOrderItems()│      │ + getInstance()      │
│ + insertNotif()     │      └─────────────────────┘
│ + getUnreadCount()  │
└─────────────────────┘
```

---

## UML Use Case Diagram

```
                         ┌──────────────────────────────────────────┐
                         │               PrintXpress App             │
                         │                                          │
  ┌──────────┐           │  (UC1)  Register account                 │
  │          │──────────>│  (UC2)  Login with role selection        │
  │ Customer │           │  (UC3)  Browse product catalogue         │
  │          │──────────>│  (UC4)  View product details             │
  └──────────┘           │  (UC5)  Add product to cart              │
                         │  (UC6)  Manage cart (qty / remove)       │
                         │  (UC7)  Place order (checkout)           │
                         │  (UC8)  View order history               │
                         │  (UC9)  Cancel order                     │
                         │  (UC10) Receive push notifications       │
                         │  (UC11) View notification list           │
                         │  (UC12) Edit profile / address           │
                         │  (UC13) View FAQ                         │
                         │                                          │
  ┌──────────┐           │  (UC14) View live order queue            │
  │ Operator │──────────>│  (UC15) Update order status              │
  └──────────┘           │  (UC16) Trigger customer notification    │
                         │                                          │
  ┌──────────┐           │  (UC17) Manage products (CRUD)           │
  │          │──────────>│  (UC18) Manage users / roles             │
  │  Admin   │──────────>│  (UC19) Manage promotions (CRUD)         │
  │          │──────────>│  (UC20) View all orders                  │
  └──────────┘           │  (UC21) Setup admin account (one-time)   │
                         └──────────────────────────────────────────┘
```

---

## UML Sequence Diagrams

### Customer Places an Order

```
Customer    LoginActivity   HomeActivity  ProductDetail   CartActivity    Firestore    SQLite
    │              │               │              │               │            │           │
    │──Login───>   │               │              │               │            │           │
    │              │──Auth─────────────────────────────────────>  │            │           │
    │              │<─role:customer────────────────────────────── │            │           │
    │              │──startActivity──>│             │             │            │           │
    │              │               │─loadProducts──────────────>  │            │           │
    │              │               │<─products─────────────────── │            │           │
    │              │               │──────────────────────────────────write──> │           │
    │──tapProduct──│──────────────>│              │               │            │           │
    │              │               │─startActivity──>             │            │           │
    │──addToCart───│───────────────│─────────────>│               │            │           │
    │              │               │              │─CartManager.add            │           │
    │──viewCart────│───────────────│──────────────│─────────────>│             │           │
    │──placeOrder──│───────────────│──────────────│─────────────>│             │           │
    │              │               │              │               │─writeOrder────────────>│
    │              │               │              │               │─writeOrder──>│          │
    │              │               │              │               │<─orderId─── │           │
    │<─Notification│               │              │               │─FCM push───>│           │
```

### Operator Updates Order Status

```
Operator  OperatorDashboard   Firestore   SQLite   FCM   Customer
    │              │               │          │      │        │
    │──Login───>   │               │          │      │        │
    │              │─listen────────>│          │      │        │
    │              │<─orders─────── │          │      │        │
    │──tapStatus── │               │          │      │        │
    │              │─showDialog     │          │      │        │
    │──selectReady─│               │          │      │        │
    │              │─update────────>│          │      │        │
    │              │─update─────────────────>  │      │        │
    │              │─sendNotif──────────────────────>│        │
    │              │                │          │      │─push──>│
    │              │<─success─────── │          │      │        │
```

---

## Role Routing Logic

```
User opens app
      │
      ▼
Firebase Auth session exists?
      │ Yes                │ No
      ▼                    ▼
Read users/{uid}.role    Show LoginActivity
from Firestore
      │
      ├── role == "customer"  ──>  HomeActivity
      ├── role == "operator"  ──>  OperatorDashboardActivity
      └── role == "admin"     ──>  AdminDashboardActivity

Role Mismatch Check:
  If selected tab ≠ Firestore role → show inline error → sign out
  If Admin tab selected + no Firestore doc → auto-write admin doc
```

---

## Ordering Flow

```
HomeActivity
  └─ tap product card
       └─ ProductDetailActivity
            └─ tap "Add to Cart"  →  CartManager.addItem()
                 └─ CartActivity
                      ├─ adjust quantities / remove items
                      ├─ enter artwork filename per item
                      ├─ select Pickup or Home Delivery
                      └─ tap "Place Order"
                           ├─ write Order header  →  SQLite + Firestore
                           ├─ write OrderItems    →  SQLite + Firestore
                           ├─ FCM notification    →  Customer device
                           └─ navigate to MyOrdersActivity
```

---

## Operator Flow

```
OperatorDashboardActivity
  └─ Firestore real-time listener (Orders collection)
       └─ RecyclerView of all active orders
            └─ tap order card
                 └─ AlertDialog: Processing / Printing / Ready / Cancelled
                      └─ on confirm
                           ├─ update Firestore Orders/{id}.status
                           ├─ update local SQLite orders.status
                           └─ FCM push to customer fcmToken
```

---

## Admin Flow

```
AdminDashboardActivity
  ├─ Manage Products
  │     ├─ Load products from Firestore
  │     ├─ Add product → Firestore + SQLite upsert
  │     ├─ Edit product → Firestore + SQLite upsert
  │     └─ Delete product → Firestore delete
  │
  ├─ Manage Users
  │     ├─ Load all users from Firestore users collection
  │     └─ Change role dialog → update users/{uid}.role in Firestore
  │
  ├─ Manage Promotions
  │     ├─ Load promotions from Firestore
  │     ├─ Add promo → Firestore
  │     ├─ Toggle active/inactive
  │     └─ Delete promo
  │
  └─ View All Orders
        └─ Read-only list from Firestore Orders collection
```

---

## Project File Structure

```
app/src/main/
│
├── java/com/example/printxpress/
│   │
│   ├── ── Models ──────────────────────────
│   ├── Customer.java
│   ├── Product.java
│   ├── Order.java
│   ├── OrderItem.java
│   ├── Notification.java
│   ├── CartItem.java
│   │
│   ├── ── Database ────────────────────────
│   ├── DBHelper.java              (SQLite, 5-table schema, v9)
│   ├── CartManager.java           (Singleton in-memory cart)
│   │
│   ├── ── Customer Activities ─────────────
│   ├── LoginActivity.java
│   ├── RegisterActivity.java
│   ├── HomeActivity.java
│   ├── ProductDetailActivity.java
│   ├── CartActivity.java
│   ├── MyOrdersActivity.java
│   ├── NotificationsActivity.java
│   ├── ProfileActivity.java
│   ├── FAQActivity.java
│   │
│   ├── ── Operator Activities ─────────────
│   ├── OperatorDashboardActivity.java
│   │
│   ├── ── Admin Activities ────────────────
│   ├── AdminDashboardActivity.java
│   ├── ManageProductsActivity.java
│   ├── ManageUsersActivity.java
│   ├── ManagePromosActivity.java
│   ├── AllOrdersActivity.java
│   │
│   ├── ── Adapters ────────────────────────
│   ├── CartAdapter.java
│   ├── OrderAdapter.java
│   ├── NotificationAdapter.java
│   ├── OperatorOrderAdapter.java
│   │
│   ├── ── Services ────────────────────────
│   ├── MyFirebaseMessagingService.java
│   └── PrintXpressApp.java        (Application class, Firestore offline)
│
├── res/
│   ├── layout/                    (20 XML layout files)
│   ├── drawable/                  (icons + shape drawables)
│   └── values/
│       ├── colors.xml
│       ├── themes.xml
│       └── strings.xml
│
└── AndroidManifest.xml
```

---

## Build & Run Instructions

### Requirements
- Android Studio Hedgehog or newer
- JDK 11+
- `google-services.json` placed in `app/` directory (from Firebase Console)

### Debug Build (USB)
```bash
./gradlew installDebug
```

### Release APK
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

The release keystore is at `app/printxpress-release.jks`:
| Field | Value |
|---|---|
| Store password | `PrintXpress2024` |
| Key alias | `printxpress` |
| Key password | `PrintXpress2024` |

---

## APK Installation

1. Copy `PrintXpress.apk` to the target Android device
2. Open the file using a file manager
3. If prompted, enable **"Install from unknown sources"** for the file manager app
4. Tap Install

**Requirements:** Android 7.0+ (API 24), internet connection for Firebase

---

## Colour Palette

| Token | Hex | Usage |
|---|---|---|
| `primary` | `#0F766E` | Buttons, active tabs, icons |
| `primary_dark` | `#0B544E` | Header gradient start |
| `secondary` | `#0EA5E9` | Accent, links |
| `background` | `#F4F7F7` | Screen backgrounds |
| `surface` | `#FFFFFF` | Cards |
| `text_primary` | `#1B2B2A` | Headings, body text |
| `text_secondary` | `#5F7371` | Subtitles, hints |
| `success` | `#2E7D32` | Ready status, confirmations |
| `warning` | `#B7791F` | Admin cred card, caution |
| `error` | `#C62828` | Inline error banner |
