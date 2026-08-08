package com.docuvio.app.ui.order.schedulecomponents


import android.app.Activity
import android.util.Log
import com.docuvio.app.BuildConfig
import com.docuvio.app.MainActivity
import com.razorpay.Checkout
import org.json.JSONObject

fun startRazorpayPayment(
    activity: Activity,
    razorpayOrderId: String,
    amount: Int,
    onError: (String) -> Unit
) {
    if (activity is MainActivity) {
        activity.prepareWindowForRazorpay {
            try {
                val checkout = Checkout()
                checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID)

                val options = JSONObject().apply {
                    put("name", "Docuvio")
                    put("description", "Print Order Payment")
                    put("order_id", razorpayOrderId)
                    put("currency", "INR")
                    put("amount", amount * 100)
                    put("theme.color", "#FFFBF5E7")
                }

                checkout.open(activity, options)

            } catch (e: Exception) {
                Log.e("RAZORPAY_DEBUG", "Error opening Razorpay", e)
                activity.enableEdgeToEdgeSafe()
                onError(e.message ?: "Payment initialization failed")
            }
        }
    } else {
        try {
            val checkout = Checkout()
            checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID)

            val options = JSONObject().apply {
                put("name", "Docuvio")
                put("description", "Print Order Payment")
                put("order_id", razorpayOrderId)
                put("currency", "INR")
                put("amount", amount * 100)
                put("theme.color", "#FFFBF5E7")
            }

            checkout.open(activity, options)

        } catch (e: Exception) {
            Log.e("RAZORPAY_DEBUG", "Error opening Razorpay", e)
            onError(e.message ?: "Payment initialization failed")
        }
    }
}