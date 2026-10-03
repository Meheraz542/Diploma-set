package com.example

import android.app.Application
import com.example.data.MarketplaceRepository

class MarketplaceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MarketplaceRepository.initialize(this)
    }
}
