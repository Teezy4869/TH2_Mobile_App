# Fruit Shop Management

Android app (Kotlin + Jetpack Compose + SQLite) for managing a fruit shop with **Admin** and **Customer** roles.

## Demo accounts

| Role     | Username   | Password   |
|----------|------------|------------|
| Admin    | `admin`    | `admin123` |
| Customer | `customer` | `123456`   |

New customers can sign up from the login screen.

## Features

- **Login** with two roles; the session (account id, username, role) is stored in **SharedPreferences**. "Remember me" keeps you signed in across app restarts.
- **SQLite storage** (`SQLiteOpenHelper`) with 4 tables:
  - `Account(id, username, password, role)`: passwords are stored as SHA-256 hashes
  - `Fruit(id, name, category, price, quantity, expiryDate, description)`
  - `Customer(id, fullname, phone, email, accountId)`
  - `Order(id, customerId, orderDate, totalAmount, status)` plus `fruitId` and `quantity`, so each order records what was bought
- **Admin**: add, edit, delete, search, and view fruits; filter All / Fresh / Expired; view all orders and set their status (Pending → Confirmed → Completed).
- **Expiry management**: every fruit shows its expiry date. Fruits that expire within 3 days are highlighted, and expired fruits are flagged in red. Customers cannot order expired fruit: the UI blocks it and the database layer rejects it.
- **Customer**: browse in-stock fruits, search, check expiry dates, place orders (validated against stock), and view order history with status and total spent.

## Project structure

```
app/src/main/java/com/example/myapplication/
├── MainActivity.kt
├── data/
│   ├── Models.kt          # Account, Fruit, Customer, Order, Role, OrderStatus
│   ├── FruitShopDb.kt     # SQLiteOpenHelper: schema, seed data, queries
│   ├── SessionManager.kt  # SharedPreferences login session
│   └── DateUtils.kt       # expiry date helpers
└── ui/
    ├── FruitShopApp.kt    # screen navigation
    ├── AuthScreens.kt     # login + register
    ├── AdminScreen.kt     # fruit CRUD + order status management
    ├── CustomerScreen.kt  # shop, ordering, order history
    └── Components.kt      # shared cards, pills, search field
```

## Build

Open the project in Android Studio and run the `app` configuration, or:

```
./gradlew assembleDebug
```
