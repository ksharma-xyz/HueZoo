package xyz.ksharma.huezoo.platform

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Koin singleton that holds a weak reference to the current foreground [Activity].
 * [MainActivity] calls [set] in `onResume` and [clear] in `onPause`.
 * Android-only billing and ad clients inject this to obtain the Activity they need.
 */
class AndroidActivityProvider {
    private var ref: WeakReference<Activity>? = null

    fun set(activity: Activity) {
        ref = WeakReference(activity)
    }

    fun clear() {
        ref = null
    }

    fun get(): Activity? = ref?.get()
}
