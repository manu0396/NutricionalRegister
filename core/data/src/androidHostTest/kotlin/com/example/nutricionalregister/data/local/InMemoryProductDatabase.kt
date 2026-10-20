package com.example.nutricionalregister.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.nutricionalregister.data.ProductDatabase
import java.io.File

internal class InMemoryProductDatabase private constructor(
    val driver: JdbcSqliteDriver,
    val file: File?,
    initializeSchema: Boolean,
) : AutoCloseable {
    constructor() : this(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY), null, true)

    val database: ProductDatabase

    init {
        if (initializeSchema) ProductDatabase.Schema.create(driver).value
        database = ProductDatabase(driver)
    }

    override fun close() {
        driver.close()
    }

    internal companion object {
        fun createPersistent(): InMemoryProductDatabase {
            val file = File.createTempFile("product-search-", ".db")
            return InMemoryProductDatabase(
                driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}"),
                file = file,
                initializeSchema = true,
            )
        }

        fun reopenPersistent(file: File): InMemoryProductDatabase =
            InMemoryProductDatabase(
                driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}"),
                file = file,
                initializeSchema = false,
            )
    }
}
