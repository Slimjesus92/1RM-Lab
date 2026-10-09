package lab.onerm.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class OneRmWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val prefs = context.getSharedPreferences("last_estimate", Context.MODE_PRIVATE)
        val label = prefs.getString("label", "Open to calculate") ?: "Open to calculate"
        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.widget)
            views.setTextViewText(R.id.widget_value, label)
            val intent = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.widget_value, pending)
            manager.updateAppWidget(id, views)
        }
    }
}