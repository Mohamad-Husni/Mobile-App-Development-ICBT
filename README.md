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

```mermaid
graph TD
    subgraph AndroidApp["Android App"]
        A[Activities / UI Layer]
        B[DBHelper\nSQLiteOpenHelper]
        C[CartManager\nSingleton]
        A --> B
        A --> C
    end

    subgraph Firebase["Firebase Backend"]
        D[Firebase Auth]
        E[Firestore\nCloud Database]
        F[FCM\nPush Notifications]
    end

    A --> D
    A --> E
    A --> F
    E -- sync --> B
    B -- dual-write --> E
```

**Data flow:** Firestore is the source of truth. On first load or sync, data is written to SQLite. All UI reads from SQLite for speed. All writes go to both SQLite and Firestore simultaneously (dual-write).

---

## ERD — Entity Relationship Diagram

```mermaid
erDiagram
    CUSTOMER {
        string customer_id PK
        string name
        string email
        string phone
        string address
    }
    ORDER {
        int order_id PK
        string firebase_order_id
        string customer_id FK
        datetime order_date
        string delivery_type
        string status
        real total_price
    }
    ORDER_ITEM {
        int item_id PK
        int order_id FK
        string firebase_item_id
        string product_id FK
        int quantity
        string artwork_file_name
        real unit_price
    }
    PRODUCT {
        string product_id PK
        string category
        real base_price
        string description
        datetime synced_at
    }
    NOTIFICATION {
        int notif_id PK
        string customer_id FK
        string type
        string message
        int is_read
        datetime created_at
    }
    PROMOTION {
        string promo_id PK
        string title
        real discount_pct
        datetime start_date
        datetime end_date
        int active
    }

    CUSTOMER ||--o{ ORDER : "places"
    ORDER ||--o{ ORDER_ITEM : "contains"
    PRODUCT ||--o{ ORDER_ITEM : "included in"
    CUSTOMER ||--o{ NOTIFICATION : "receives"
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

```mermaid
classDiagram
    class Customer {
        +String customerId
        +String name
        +String email
        +String phone
        +String address
    }
    class Product {
        +String productId
        +String category
        +double basePrice
        +String description
    }
    class Order {
        +int orderId
        +String firebaseOrderId
        +String customerId
        +String orderDate
        +String deliveryType
        +String status
        +double totalPrice
    }
    class OrderItem {
        +int itemId
        +int orderId
        +String productId
        +int quantity
        +String artworkFileName
        +double unitPrice
    }
    class Notification {
        +int notifId
        +String customerId
        +String type
        +String message
        +boolean isRead
        +String createdAt
    }
    class CartItem {
        +Product product
        +int quantity
        +String artworkFileName
        +getSubtotal() double
    }
    class DBHelper {
        +insertCustomer()
        +insertProduct()
        +upsertProduct()
        +insertOrder()
        +getOrdersByUid()
        +insertOrderItems()
        +insertNotification()
        +getUnreadCount()
        +updateOrderStatus()
    }
    class CartManager {
        -List~CartItem~ items
        +getInstance() CartManager
        +addItem()
        +removeItem()
        +clear()
        +getTotal() double
    }

    Customer "1" --> "many" Order : places
    Order "1" --> "many" OrderItem : contains
    Product "1" --> "many" OrderItem : referenced by
    Customer "1" --> "many" Notification : receives
    CartManager "1" --> "many" CartItem : holds
    CartItem --> Product : references
```

---

## UML Use Case Diagram

```mermaid
graph LR
    Customer(["👤 Customer"])
    Operator(["🖨️ Operator"])
    Admin(["🔧 Admin"])

    subgraph PrintXpress App
        UC1([Register account])
        UC2([Login with role selection])
        UC3([Browse product catalogue])
        UC4([View product details])
        UC5([Add product to cart])
        UC6([Manage cart])
        UC7([Place order])
        UC8([View order history])
        UC9([Cancel order])
        UC10([Receive push notifications])
        UC11([View notification list])
        UC12([Edit profile / address])
        UC13([View FAQ])
        UC14([View live order queue])
        UC15([Update order status])
        UC16([Trigger customer notification])
        UC17([Manage products CRUD])
        UC18([Manage users and roles])
        UC19([Manage promotions CRUD])
        UC20([View all orders])
        UC21([Setup admin account])
    end

    Customer --> UC1
    Customer --> UC2
    Customer --> UC3
    Customer --> UC4
    Customer --> UC5
    Customer --> UC6
    Customer --> UC7
    Customer --> UC8
    Customer --> UC9
    Customer --> UC10
    Customer --> UC11
    Customer --> UC12
    Customer --> UC13

    Operator --> UC2
    Operator --> UC14
    Operator --> UC15
    Operator --> UC16

    Admin --> UC2
    Admin --> UC17
    Admin --> UC18
    Admin --> UC19
    Admin --> UC20
    Admin --> UC21
```

---

## UML Sequence Diagrams

### Customer Places an Order

```mermaid
sequenceDiagram
    actor C as Customer
    participant L as LoginActivity
    participant H as HomeActivity
    participant P as ProductDetailActivity
    participant CA as CartActivity
    participant FS as Firestore
    participant DB as SQLite

    C->>L: Enter credentials + select Customer tab
    L->>FS: signInWithEmailAndPassword()
    FS-->>L: uid + role: customer
    L->>H: startActivity(HomeActivity)
    H->>FS: load Products collection
    FS-->>H: product list
    H->>DB: upsertProducts()
    C->>H: tap product card
    H->>P: startActivity(ProductDetailActivity)
    C->>P: tap Add to Cart
    P->>CA: CartManager.addItem()
    C->>CA: view cart, adjust qty, enter artwork
    C->>CA: tap Place Order
    CA->>FS: write Order header + OrderItems
    CA->>DB: insertOrder() + insertOrderItems()
    FS-->>CA: orderId
    FS->>C: FCM order confirmation notification
    CA->>C: navigate to MyOrdersActivity
```

### Operator Updates Order Status

```mermaid
sequenceDiagram
    actor O as Operator
    participant OD as OperatorDashboardActivity
    participant FS as Firestore
    participant DB as SQLite
    participant FCM as FCM Service
    actor C as Customer

    O->>OD: Login as Operator
    OD->>FS: addSnapshotListener(Orders)
    FS-->>OD: real-time order list
    O->>OD: tap order → select new status
    OD->>OD: show confirmation dialog
    O->>OD: confirm
    OD->>FS: update Orders/{id}.status
    OD->>DB: updateOrderStatus()
    OD->>FCM: send push to customer fcmToken
    FCM->>C: push notification "Order is Ready"
    FS-->>OD: success
```

---

## Role Routing Logic

```mermaid
flowchart TD
    A([User opens app]) --> B{Firebase Auth\nsession exists?}
    B -- Yes --> C[Read users/uid/role\nfrom Firestore]
    B -- No --> D[Show LoginActivity]
    D --> E{Selected tab\nmatches role?}
    E -- Mismatch --> F[Show inline error\nSign out]
    E -- Match --> C
    C --> G{role value}
    G -- customer --> H[HomeActivity]
    G -- operator --> I[OperatorDashboardActivity]
    G -- admin --> J[AdminDashboardActivity]
    C --> K{Admin tab +\nno Firestore doc?}
    K -- Yes --> L[Auto-write admin\ndocument to Firestore]
    L --> J
```

---

## Ordering Flow

```mermaid
flowchart TD
    A[HomeActivity\nProduct Grid] --> B[Tap product card]
    B --> C[ProductDetailActivity\nSpecs + Price]
    C --> D[Tap Add to Cart]
    D --> E[CartManager.addItem]
    E --> F[CartActivity\nCart Items List]
    F --> G[Adjust qty / remove items\nEnter artwork filename]
    G --> H[Select Pickup or\nHome Delivery]
    H --> I[Tap Place Order]
    I --> J[Write Order header\nSQLite + Firestore]
    J --> K[Write OrderItems\nSQLite + Firestore]
    K --> L[FCM Order Confirmation\nto Customer]
    L --> M[MyOrdersActivity]
```

---

## Operator Flow

```mermaid
flowchart TD
    A[OperatorDashboardActivity] --> B[Firestore real-time\nsnapshot listener]
    B --> C[RecyclerView of\nactive orders]
    C --> D[Operator taps order card]
    D --> E[AlertDialog: select new status\nProcessing / Printing / Ready / Cancelled]
    E --> F[Confirm]
    F --> G[Update Firestore\nOrders/id/status]
    F --> H[Update SQLite\norders.status]
    F --> I[FCM push to\ncustomer fcmToken]
    I --> J[Customer receives\npush notification]
```

---

## Admin Flow

```mermaid
flowchart TD
    A[AdminDashboardActivity] --> B[Manage Products]
    A --> C[Manage Users]
    A --> D[Manage Promotions]
    A --> E[View All Orders]

    B --> B1[Load from Firestore]
    B --> B2[Add product\nFirestore + SQLite]
    B --> B3[Edit product\nFirestore + SQLite]
    B --> B4[Delete product\nFirestore]

    C --> C1[Load all users\nfrom Firestore]
    C --> C2[Change role dialog\nupdate users/uid/role]

    D --> D1[Load promotions\nfrom Firestore]
    D --> D2[Add promo]
    D --> D3[Toggle active/inactive]
    D --> D4[Delete promo]

    E --> E1[Read-only list\nFirestore Orders]
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
