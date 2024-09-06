package com.qrkit.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.qrkit.views.QRScannerView

class QRScannerActivity: ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QRScannerView { qrCode ->
                val resultIntent = Intent().apply {
                    putExtra("qrCode", qrCode)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }
}