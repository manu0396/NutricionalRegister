package com.example.nutricionalregister.search

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.nutricionalregister.domain.product.ConnectivityStatus
import com.example.nutricionalregister.domain.product.ConnectivityStatusProvider

public class AndroidConnectivityStatusProvider(
    context: Context,
) : ConnectivityStatusProvider {
    private val connectivityManager: ConnectivityManager =
        requireNotNull(context.getSystemService(ConnectivityManager::class.java))

    override suspend fun currentStatus(): ConnectivityStatus {
        val network = connectivityManager.activeNetwork ?: return ConnectivityStatus.OFFLINE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectivityStatus.OFFLINE
        return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            ConnectivityStatus.ONLINE
        } else {
            ConnectivityStatus.OFFLINE
        }
    }
}
