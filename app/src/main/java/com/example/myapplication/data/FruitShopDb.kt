package com.example.myapplication.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

class FruitShopDb private constructor(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE Account(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                role TEXT NOT NULL)"""
        )
        db.execSQL(
            """CREATE TABLE Fruit(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                price REAL NOT NULL,
                quantity INTEGER NOT NULL,
                expiryDate TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '')"""
        )
        db.execSQL(
            """CREATE TABLE Customer(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fullname TEXT NOT NULL,
                phone TEXT NOT NULL,
                email TEXT NOT NULL,
                accountId INTEGER NOT NULL UNIQUE REFERENCES Account(id) ON DELETE CASCADE)"""
        )
        db.execSQL(
            """CREATE TABLE $T_ORDER(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                customerId INTEGER NOT NULL REFERENCES Customer(id) ON DELETE CASCADE,
                orderDate TEXT NOT NULL,
                totalAmount REAL NOT NULL,
                status TEXT NOT NULL DEFAULT '${OrderStatus.PENDING}',
                fruitId INTEGER REFERENCES Fruit(id) ON DELETE SET NULL,
                quantity INTEGER NOT NULL DEFAULT 1)"""
        )
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T_ORDER")
        db.execSQL("DROP TABLE IF EXISTS Customer")
        db.execSQL("DROP TABLE IF EXISTS Fruit")
        db.execSQL("DROP TABLE IF EXISTS Account")
        onCreate(db)
    }

    private fun seed(db: SQLiteDatabase) {
        db.insert("Account", null, accountValues("admin", "admin123", Role.ADMIN))
        val customerAccountId = db.insert("Account", null, accountValues("customer", "123456", Role.CUSTOMER))
        db.insert(
            "Customer", null,
            customerValues("Demo Customer", "0901234567", "customer@fruitshop.com", customerAccountId)
        )
        listOf(
            Fruit(name = "Apple", category = "Imported", price = 3.5, quantity = 100, expiryDate = DateUtils.offset(30), description = "Crisp red Fuji apples"),
            Fruit(name = "Banana", category = "Tropical", price = 1.2, quantity = 150, expiryDate = DateUtils.offset(5), description = "Sweet ripe bananas"),
            Fruit(name = "Mango", category = "Tropical", price = 2.8, quantity = 60, expiryDate = DateUtils.offset(10), description = "Golden Cat Chu mangoes"),
            Fruit(name = "Orange", category = "Citrus", price = 2.0, quantity = 80, expiryDate = DateUtils.offset(20), description = "Juicy navel oranges"),
            Fruit(name = "Grapes", category = "Berries", price = 4.5, quantity = 50, expiryDate = DateUtils.offset(2), description = "Seedless green grapes"),
            Fruit(name = "Strawberry", category = "Berries", price = 5.0, quantity = 40, expiryDate = DateUtils.offset(-2), description = "Fresh Da Lat strawberries"),
            Fruit(name = "Dragon Fruit", category = "Tropical", price = 3.0, quantity = 0, expiryDate = DateUtils.offset(7), description = "White-flesh dragon fruit")
        ).forEach { db.insert("Fruit", null, fruitValues(it)) }
    }

    // ---------------- Account / Customer ----------------

    fun login(username: String, password: String): Account? =
        readableDatabase.rawQuery(
            "SELECT * FROM Account WHERE username = ? AND password = ?",
            arrayOf(username, hash(password))
        ).use { c ->
            if (c.moveToFirst()) Account(c.long("id"), c.str("username"), c.str("password"), c.str("role")) else null
        }

    fun usernameExists(username: String): Boolean =
        readableDatabase.rawQuery("SELECT 1 FROM Account WHERE username = ?", arrayOf(username))
            .use { it.moveToFirst() }

    /** Returns an error message, or null on success. */
    fun registerCustomer(username: String, password: String, fullname: String, phone: String, email: String): String? {
        if (usernameExists(username)) return "Username \"$username\" is already taken"
        val db = writableDatabase
        db.beginTransaction()
        try {
            val accountId = db.insertOrThrow("Account", null, accountValues(username, password, Role.CUSTOMER))
            db.insertOrThrow("Customer", null, customerValues(fullname, phone, email, accountId))
            db.setTransactionSuccessful()
            return null
        } finally {
            db.endTransaction()
        }
    }

    fun getCustomerByAccountId(accountId: Long): Customer? =
        readableDatabase.rawQuery("SELECT * FROM Customer WHERE accountId = ?", arrayOf(accountId.toString()))
            .use { c ->
                if (c.moveToFirst()) {
                    Customer(c.long("id"), c.str("fullname"), c.str("phone"), c.str("email"), c.long("accountId"))
                } else null
            }

    // ---------------- Fruit ----------------

    /** Search by name or category. [inStockOnly] hides fruits with quantity 0. */
    fun getFruits(query: String = "", inStockOnly: Boolean = false): List<Fruit> {
        val like = "%${query.trim()}%"
        val stockClause = if (inStockOnly) "AND quantity > 0" else ""
        return readableDatabase.rawQuery(
            "SELECT * FROM Fruit WHERE (name LIKE ? OR category LIKE ?) $stockClause ORDER BY name COLLATE NOCASE",
            arrayOf(like, like)
        ).use { c -> buildList { while (c.moveToNext()) add(c.toFruit()) } }
    }

    fun getFruit(id: Long, db: SQLiteDatabase = readableDatabase): Fruit? =
        db.rawQuery("SELECT * FROM Fruit WHERE id = ?", arrayOf(id.toString()))
            .use { c -> if (c.moveToFirst()) c.toFruit() else null }

    fun insertFruit(fruit: Fruit): Long = writableDatabase.insert("Fruit", null, fruitValues(fruit))

    fun updateFruit(fruit: Fruit): Int =
        writableDatabase.update("Fruit", fruitValues(fruit), "id = ?", arrayOf(fruit.id.toString()))

    fun deleteFruit(id: Long): Int = writableDatabase.delete("Fruit", "id = ?", arrayOf(id.toString()))

    // ---------------- Order ----------------

    /**
     * Places an order, rejecting expired fruits and quantities above stock,
     * and deducts the stock in the same transaction.
     * Returns an error message, or null on success.
     */
    fun placeOrder(customerId: Long, fruitId: Long, quantity: Int): String? {
        if (quantity <= 0) return "Quantity must be at least 1"
        val db = writableDatabase
        db.beginTransaction()
        try {
            val fruit = getFruit(fruitId, db) ?: return "This fruit no longer exists"
            if (fruit.isExpired) return "${fruit.name} expired on ${fruit.expiryDate} and cannot be ordered"
            if (quantity > fruit.quantity) return "Only ${fruit.quantity} ${fruit.name} left in stock"
            val values = ContentValues().apply {
                put("customerId", customerId)
                put("orderDate", DateUtils.now())
                put("totalAmount", fruit.price * quantity)
                put("status", OrderStatus.PENDING)
                put("fruitId", fruitId)
                put("quantity", quantity)
            }
            db.insertOrThrow(T_ORDER, null, values)
            db.execSQL("UPDATE Fruit SET quantity = quantity - ? WHERE id = ?", arrayOf<Any>(quantity, fruitId))
            db.setTransactionSuccessful()
            return null
        } finally {
            db.endTransaction()
        }
    }

    fun getOrdersByCustomer(customerId: Long): List<Order> = queryOrders("WHERE o.customerId = ?", arrayOf(customerId.toString()))

    fun getAllOrders(): List<Order> = queryOrders("", emptyArray())

    fun updateOrderStatus(orderId: Long, status: String): Int =
        writableDatabase.update(
            T_ORDER,
            ContentValues().apply { put("status", status) },
            "id = ?",
            arrayOf(orderId.toString())
        )

    private fun queryOrders(where: String, args: Array<String>): List<Order> =
        readableDatabase.rawQuery(
            """SELECT o.*, COALESCE(f.name, 'Deleted fruit') AS fruitName, c.fullname AS customerName
               FROM $T_ORDER o
               LEFT JOIN Fruit f ON f.id = o.fruitId
               LEFT JOIN Customer c ON c.id = o.customerId
               $where
               ORDER BY o.id DESC""",
            args
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val fruitIdIdx = c.getColumnIndexOrThrow("fruitId")
                    add(
                        Order(
                            id = c.long("id"),
                            customerId = c.long("customerId"),
                            orderDate = c.str("orderDate"),
                            totalAmount = c.double("totalAmount"),
                            status = c.str("status"),
                            fruitId = if (c.isNull(fruitIdIdx)) null else c.getLong(fruitIdIdx),
                            quantity = c.int("quantity"),
                            fruitName = c.str("fruitName"),
                            customerName = c.str("customerName")
                        )
                    )
                }
            }
        }

    // ---------------- Helpers ----------------

    private fun accountValues(username: String, password: String, role: String) = ContentValues().apply {
        put("username", username)
        put("password", hash(password))
        put("role", role)
    }

    private fun customerValues(fullname: String, phone: String, email: String, accountId: Long) = ContentValues().apply {
        put("fullname", fullname)
        put("phone", phone)
        put("email", email)
        put("accountId", accountId)
    }

    private fun fruitValues(f: Fruit) = ContentValues().apply {
        put("name", f.name)
        put("category", f.category)
        put("price", f.price)
        put("quantity", f.quantity)
        put("expiryDate", f.expiryDate)
        put("description", f.description)
    }

    private fun Cursor.toFruit() = Fruit(
        id = long("id"),
        name = str("name"),
        category = str("category"),
        price = double("price"),
        quantity = int("quantity"),
        expiryDate = str("expiryDate"),
        description = str("description")
    )

    private fun Cursor.long(col: String) = getLong(getColumnIndexOrThrow(col))
    private fun Cursor.int(col: String) = getInt(getColumnIndexOrThrow(col))
    private fun Cursor.double(col: String) = getDouble(getColumnIndexOrThrow(col))
    private fun Cursor.str(col: String): String = getString(getColumnIndexOrThrow(col)) ?: ""

    companion object {
        private const val DB_NAME = "fruit_shop.db"
        private const val DB_VERSION = 1

        /** "Order" is an SQL keyword, so the table name is always quoted. */
        const val T_ORDER = "\"Order\""

        @Volatile
        private var instance: FruitShopDb? = null

        fun getInstance(context: Context): FruitShopDb =
            instance ?: synchronized(this) {
                instance ?: FruitShopDb(context.applicationContext).also { instance = it }
            }

        /** Passwords are stored as SHA-256 hashes, never plain text. */
        fun hash(password: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(password.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
