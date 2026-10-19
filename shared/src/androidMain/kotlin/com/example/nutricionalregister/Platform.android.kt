package com.example.nutricionalregister

import android.os.Build

public class AndroidPlatform : Platform {
    public override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

public actual fun getPlatform(): Platform = AndroidPlatform()
