package com.vastobb.alpmegateway.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vastobb.alpmegateway.AlpmeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val app = context.applicationContext as? AlpmeApp ?: run {
            pending.finish()
            return
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = app.container.settings.first()
                if (settings.startOnBoot) {
                    ProxyService.start(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
