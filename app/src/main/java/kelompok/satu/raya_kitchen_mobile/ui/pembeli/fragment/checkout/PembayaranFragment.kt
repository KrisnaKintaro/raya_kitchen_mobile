package kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.checkout

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import kelompok.satu.raya_kitchen_mobile.databinding.FragmentPembayaranBinding
import kelompok.satu.raya_kitchen_mobile.utils.Constant
import kelompok.satu.raya_kitchen_mobile.utils.FormatHelper
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

private var orderId = ""

class PembayaranFragment : Fragment() {

    private var _binding: FragmentPembayaranBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private var transactionId = ""
    private var totalTagihan = 0

    // Variabel penampung file gambar mentah
    private var imageBytes: ByteArray? = null

    // Launcher untuk Galeri
    private val galeriLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                binding.ivPreviewBukti.setImageURI(it)
                // Konversi Uri ke ByteArray
                val inputStream = requireContext().contentResolver.openInputStream(it)
                imageBytes = inputStream?.readBytes()
                inputStream?.close()
                binding.ivPreviewBukti.imageTintList = null // Hilangkan efek warna abu-abu
            }
        }
    }

    // Launcher minta izin kamera
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Kalau diizinkan, langsung buka kameranya
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            kameraLauncher.launch(intent)
        } else {
            Toast.makeText(requireContext(), "Izin kamera ditolak cuy, ga bisa jepret!", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher untuk Kamera (Ambil Thumbnail/Bitmap)
    private val kameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            bitmap?.let {
                binding.ivPreviewBukti.setImageBitmap(it)
                // Konversi Bitmap ke ByteArray (JPEG)
                val stream = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                imageBytes = stream.toByteArray()
                binding.ivPreviewBukti.imageTintList = null
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPembayaranBinding.inflate(inflater, container, false)

        arguments?.let {
            transactionId = it.getString("EXTRA_TRANS_ID", "")
            orderId = it.getString("EXTRA_ORDER_ID", "")
            totalTagihan = it.getInt("EXTRA_TOTAL", 0)
        }

        binding.tvTotalBayarTF.text = FormatHelper.formatRupiah(totalTagihan)

        // 1. Fitur Salin Rekening
        binding.btnSalinRekening.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val rekNumber = binding.tvNoRekening.text.toString()
            val clip = ClipData.newPlainText("No Rekening", rekNumber)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Nomor rekening disalin!", Toast.LENGTH_SHORT).show()
        }

        // 2. Tombol Buka Galeri
        binding.btnGaleri.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galeriLauncher.launch(intent)
        }

        // 3. Tombol Buka Kamera
        binding.btnKamera.setOnClickListener {
            val cameraPermission = android.Manifest.permission.CAMERA
            val isPermissionGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(),
                cameraPermission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (isPermissionGranted) {
                   val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                kameraLauncher.launch(intent)
            } else {
                requestCameraPermissionLauncher.launch(cameraPermission)
            }
        }

        // 4. Tombol Upload & Update Firestore
        binding.btnUploadBukti.setOnClickListener {
            if (imageBytes == null) {
                Toast.makeText(requireContext(), "Pilih atau foto bukti transfer dulu cuy!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            uploadKeSupabase(imageBytes!!)
        }

        return binding.root
    }

    private fun uploadKeSupabase(bytes: ByteArray) {
        binding.btnUploadBukti.isEnabled = false
        binding.btnUploadBukti.text = "Mengunggah..."

        // Kita jalankan networking di Thread terpisah agar UI tidak freeze
        Thread {
            try {
                val fileName = "${transactionId}.jpg"

                val bucketName = Constant.SUPABASE_BUCKET

                // Endpoint REST API Supabase Storage
                val url = URL("${Constant.SUPABASE_URL}/storage/v1/object/$bucketName/$fileName")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer ${Constant.SUPABASE_KEY}")
                connection.setRequestProperty("apikey", Constant.SUPABASE_KEY)
                connection.setRequestProperty("Content-Type", "image/jpeg")
                connection.doOutput = true

                // Tulis ByteArray ke server
                connection.outputStream.use { it.write(bytes) }

                val responseCode = connection.responseCode
                if (responseCode == 200 || responseCode == 201) {
                    // Kalau berhasil, rakit URL Publik gambarnya
                    val publicUrl = "${Constant.SUPABASE_URL}/storage/v1/object/public/$bucketName/$fileName"

                    // Lanjut update Firestore di Main Thread
                    requireActivity().runOnUiThread {
                        updateFirestoreStatus(publicUrl)
                    }
                } else {
                    requireActivity().runOnUiThread {
                        gagalUpload("Gagal upload: Kode $responseCode")
                    }
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    gagalUpload("Error jaringan: ${e.message}")
                }
            }
        }.start()
    }

    private fun updateFirestoreStatus(imageUrl: String) {
        val batch = db.batch()

        val transRef = db.collection("transactions").document(transactionId)
        batch.update(transRef, mapOf(
            "payment_proof" to imageUrl,
            "payment_status" to "Menunggu Verifikasi"
        ))

        val orderRef = db.collection("orders").document(orderId)
        batch.update(orderRef, "status", "Menunggu Verifikasi")

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Bukti terkirim! Pesanan segera diproses.", Toast.LENGTH_LONG).show()
                requireActivity().supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
            .addOnFailureListener {
                gagalUpload("Gagal update data pesanan")
            }
    }

    private fun gagalUpload(pesan: String) {
        binding.btnUploadBukti.isEnabled = true
        binding.btnUploadBukti.text = "Upload & Konfirmasi"
        Toast.makeText(requireContext(), pesan, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}