package com.lockin

import android.app.Application
import com.lockin.app.LockinContainer

class LockinApp : Application() {
    val container: LockinContainer by lazy {
        LockinContainer(this)
    }
}
