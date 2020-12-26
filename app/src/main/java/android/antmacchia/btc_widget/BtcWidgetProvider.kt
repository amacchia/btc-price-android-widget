package android.antmacchia.btc_widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.NumberFormat


class BtcWidgetProvider : AppWidgetProvider() {

    private var price = 0.0

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        Log.i(TAG, "onUpdate: Updating BTC widget")
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(KRAKEN_URL)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i(TAG, "onUpdate: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.i(TAG, "onResponse: Response status code ${response.code}")
                val body = response.body?.string()
                val jsonResponse = JSONObject(body ?: "")
                price = jsonResponse
                    .getJSONObject("result")
                    .getJSONObject("XXBTZUSD")
                    .getJSONArray("c")
                    .getDouble(0)

                appWidgetIds?.forEach { appWidgetId ->
                    val views: RemoteViews = RemoteViews(
                        context?.packageName,
                        R.layout.btc_widget_layout
                    ).apply {
                        val intent = Intent(context, BtcWidgetProvider::class.java).apply {
                            action = ACTION_REFRESH_PRICE
                        }
                        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
                        setOnClickPendingIntent(R.id.price_ticker, pendingIntent)
                        setTextViewText(R.id.price_ticker, formatPrice(price))
                    }

                    appWidgetManager?.updateAppWidget(appWidgetId, views)
                }
            }
        })
    }

    private fun formatPrice(price: Double) =
        NumberFormat.getCurrencyInstance().format(price)

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        Log.i(TAG, "onReceive: Received Intent ${intent?.action}")
        if (intent?.action == ACTION_REFRESH_PRICE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisAppWidgetComponentName = ComponentName(context!!.packageName, javaClass.name)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        private val TAG = BtcWidgetProvider::class.simpleName
        private const val KRAKEN_URL = "https://api.kraken.com/0/public/Ticker?pair=xbtusd"
        private const val ACTION_REFRESH_PRICE = "REFRESH_BTC_PRICE"
    }

}