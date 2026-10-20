plugins {
    id("app.cash.sqldelight")
}

sqldelight {
    databases {
        create("ProductDatabase") {
            packageName.set("com.example.nutricionalregister.data")
        }
    }
}
