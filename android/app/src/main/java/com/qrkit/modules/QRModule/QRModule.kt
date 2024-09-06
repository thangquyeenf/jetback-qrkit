package com.qrkit.modules.QRModule

import android.app.Activity
import android.content.Intent
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.qrkit.activities.QRScannerActivity

class QRModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), ActivityEventListener {

    private var promise: Promise? = null

    init {
        reactContext.addActivityEventListener(this)
    }

    override fun getName(): String {
        return "QRModule"
    }

    @ReactMethod
    fun openQRScanner(promise: Promise) {
        this.promise = promise
        val activity = currentActivity
        if (activity != null) {
            val intent = Intent(activity, QRScannerActivity::class.java)
            activity.startActivityForResult(intent, REQUEST_CODE_QR_SCAN)
        } else {
            promise.reject("Activity doesn't exist")
        }
    }

    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_QR_SCAN) {
            if (resultCode == Activity.RESULT_OK) {
                val qrCode = data?.getStringExtra("qrCode")
                promise?.resolve(qrCode)
            } else {
                promise?.reject("QR scan failed")
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        // Not needed for this module
    }

    companion object {
        private const val REQUEST_CODE_QR_SCAN = 101
    }
}
