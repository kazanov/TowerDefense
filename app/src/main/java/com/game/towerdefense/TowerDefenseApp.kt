package com.game.towerdefense

import android.app.Application
import com.game.towerdefense.di.AppContainer

class TowerDefenseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }
}
