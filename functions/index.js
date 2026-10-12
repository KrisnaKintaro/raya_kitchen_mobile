const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.kirimNotifStatusPesanan = functions.firestore
    .document('orders/{orderId}')
    .onUpdate(async (change, context) => {
        const dataBaru = change.after.data();
        const dataLama = change.before.data();

        // Cek kalau statusnya beneran berubah
        if (dataBaru.status !== dataLama.status) {
            
            const userWa = dataBaru.user_wa;
            
            // 1. Tarik Token FCM dari koleksi users
            const userDoc = await admin.firestore().collection('users').doc(userWa).get();

            if (!userDoc.exists || !userDoc.data().fcm_token) {
                console.log(`Batal kirim: Token FCM tidak ditemukan untuk WA ${userWa}`);
                return null; 
            }

            const tokenAsli = userDoc.data().fcm_token;
            
            // 2. Rakit Pesan Notifikasi (Data Payload)
            const payload = {
                data: {
                    type: "status_order",
                    title: `Pesanan ${dataBaru.status}!`,
                    body: `Hai ${dataBaru.recipient_name}, status pesananmu sekarang: ${dataBaru.status}.`
                },
                token: tokenAsli
            };

            // 3. Tembak Notifikasi ke HP User
            try {
                const response = await admin.messaging().send(payload);
                console.log(`Notif sukses dikirim ke ${userWa}:`, response);
                return response;
            } catch (error) {
                console.error(`Error kirim notif ke ${userWa}:`, error);
                return null;
            }
        }
        
        return null;
    });