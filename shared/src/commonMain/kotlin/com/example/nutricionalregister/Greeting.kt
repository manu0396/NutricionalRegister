package com.example.nutricionalregister

public class Greeting {
    private val platform = getPlatform()

    public fun greet(): String = sayHello(platform.name)
}
