package com.qrkit.modules.SimManageModule

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.euicc.EuiccManager
import android.telephony.euicc.EuiccInfo
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ActivityEventListener
import android.util.Log

class SimManageModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), ActivityEventListener {

    private var promise: Promise? = null
    private val TAG = "SimManageModule"
    private val REQUEST_CODE = 1
    private val euiccManager: EuiccManager by lazy {
        reactContext.getSystemService(Context.EUICC_SERVICE) as EuiccManager
    }

    private val ACTIVATION_DOWNLOAD_SUBSCRIPTION = 'download_subscription';
    private val LPA_DECLARE_PERMISSION = 'com.qrkit.permission.BROADCAST'

    init {
        reactContext.addActivityEventListener(this)
    }

    override fun getName(): String {
        return "SimManageModule"
    }

    val receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent?.action != ACTION_DOWNLOAD_SUBSCRIPTION) {
                    promise.reject("3", "Can't set up eSIM due to wrong intent: ${intent?.action}")
                    return
            }
            when (resultCode) {
                    EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_RESOLVABLE_ERROR -> {
                        try {
                            val callbackIntent = PendingIntent.getBroadcast(
                                mReactContext,
                                3,
                                Intent(ACTION_DOWNLOAD_SUBSCRIPTION),
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                            )
                            mgr?.startResolutionActivity(mReactContext.currentActivity, 3, intent, callbackIntent)
                        } catch (e: Exception) {
                            promise.reject("3", "Can't set up eSIM due to Activity error: ${e.localizedMessage}")
                        }
                    }

                    EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK -> {
                        promise.resolve(true)
                    }

                    EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_ERROR -> {
                        promise.reject("2", "EMBEDDED_SUBSCRIPTION_RESULT_ERROR - Can't add an eSIM subscription")
                    }

                    else -> {
                        promise.reject("3", "Unknown error, result code: $resultCode")
                    }
                }
            }
        }
    }

    private fun handResolvableError(intent: Intent) {
        try {
            val explicitIntent = Intent(ACTION_DOWNLOAD_SUBSCRIPTION)
            explicitIntent.
        }
        catch(Exception e) {
            
        }
    }

    @ReactMethod
    fun isEsimSupported(promise: Promise) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            promise.resolve(euiccManager.isEnabled)
        } else {
            promise.reject("UNSUPPORTED", "eSIM is not supported on this device.")
        }
    }

    @ReactMethod
    fun manageEmbeddedSubcriptions(promise: Promise) {
        this.promise = promise
        val activity = currentActivity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && activity != null) {
            val intent = Intent(EuiccManager.ACTION_MANAGE_EMBEDDED_SUBSCRIPTIONS)
            activity.startActivityForResult(intent, REQUEST_CODE)
            Log.d(TAG, "Done Intent")

        } else {
            promise.reject("UNSUPPORTED", "eSIM setup requires Android P or above.")
        }
    }


    @ReactMethod
    fun setupEsimFromQr(qrCode: String, promise: Promise) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val intent = Intent(EuiccManager.ACTION_MANAGE_EMBEDDED_SUBSCRIPTIONS)
            currentActivity?.startActivityForResult(intent, REQUEST_CODE)
            promise.resolve("eSIM provisioning started")
        } else {
            promise.reject("UNSUPPORTED", "eSIM setup requires Android P or above.")
        }
    }
    
    // Handle Activity Result to check the outcome of the provisioning process
    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                promise?.resolve(true)
            } else {
                promise?.reject("SYSTEM ERROR")
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        // Not needed for this module
    }
}
