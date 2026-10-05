package com.game.towerdefense.data.analytics

import android.util.Log
import com.game.towerdefense.domain.service.Analytics

class LogcatAnalytics : Analytics {
    override fun logEvent(name: String, params: Map<String, Any?>) {
        Log.d("Analytics", if (params.isEmpty()) name else "$name $params")
    }
}
