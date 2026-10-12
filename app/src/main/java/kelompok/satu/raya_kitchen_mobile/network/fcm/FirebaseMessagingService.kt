package kelompok.satu.raya_kitchen_mobile.network.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kelompok.satu.raya_kitchen_mobile.R
import kelompok.satu.raya_kitchen_mobile.ui.pembeli.MasterActivity
import kelompok.satu.raya_kitchen_mobile.utils.SessionManager

class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val tipeNotif = message.data["type"] // Bisa "promo" atau "status_order"

        if (tipeNotif == "promo") {
            // Kalau tipe Promo, broadcast data ke Activity buat nampilin Modal Pop-up
            val intent = Intent("EVENT_PROMO_NOTIF")
            intent.putExtra("promo_title", message.data["title"])
            intent.putExtra("promo_body", message.data["body"])
            intent.putExtra("promo_image", message.data["image_url"])

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
        else if (tipeNotif == "status_order" || message.notification != null) {
            // Kalau tipe Status Order, tampilkan notif sistem di bar atas (System Tray)
            val title = message.data["title"] ?: message.notification?.title ?: "Update Pesanan"
            val body = message.data["body"] ?: message.notification?.body ?: "Status pesananmu berubah."
            showSystemNotification(title, body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val userWa = SessionManager(applicationContext).getUserWa()
        if (userWa.isNotEmpty()) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(userWa)
                .set(mapOf("fcm_token" to token), com.google.firebase.firestore.SetOptions.merge())
        }
    }

    private fun showSystemNotification(title: String, message: String) {
        val channelId = "raya_kitchen_channel"
        val intent = Intent(this, MasterActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo_app)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Untuk Android 8.0 (Oreo) ke atas wajib pakai Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Notifikasi Pesanan", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}