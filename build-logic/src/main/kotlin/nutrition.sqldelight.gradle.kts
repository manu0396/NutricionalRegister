plugins {
    id("app.cash.sqldelight")
}

sqldelight {
    databases {
        create("ProductDatabase") {
            packageName.set("com.example.nutricionalregister.data")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}
