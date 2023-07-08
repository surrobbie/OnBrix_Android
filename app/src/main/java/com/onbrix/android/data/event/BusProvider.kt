package com.onbrix.android.data.event

import android.os.Handler
import android.os.Looper
import com.squareup.otto.Bus
import com.squareup.otto.ThreadEnforcer

object BusProvider: Bus() {

    /**
     * Instance of [Bus]
     */
    private var instance: Bus? = null

    /**
     * Get the instance of [Bus]
     *
     * @return
     */
    @Synchronized
    fun get(): Bus? {
        if (instance == null) {
            instance = Bus(ThreadEnforcer.ANY)
        }
        return instance
    }

    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun post(event: Any?) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.post(event)
        } else {
            handler.post(Runnable { super@BusProvider.post(event) })
        }
    }

}