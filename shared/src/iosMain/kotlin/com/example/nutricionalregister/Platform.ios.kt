package com.example.nutricionalregister

import platform.UIKit.UIDevice

public class IOSPlatform : Platform {
    public override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

public actual fun getPlatform(): Platform = IOSPlatform()
