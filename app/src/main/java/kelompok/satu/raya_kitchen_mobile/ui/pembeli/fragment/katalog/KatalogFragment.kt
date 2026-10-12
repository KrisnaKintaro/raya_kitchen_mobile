package kelompok.satu.raya_kitchen_mobile.ui.pembeli.fragment.katalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import kelompok.satu.raya_kitchen_mobile.databinding.FragmentKatalogBinding
import kelompok.satu.raya_kitchen_mobile.model.Product
import kelompok.satu.raya_kitchen_mobile.model.ProductVariant
import kelompok.satu.raya_kitchen_mobile.network.api.VolleySingleton
import kelompok.satu.raya_kitchen_mobile.ui.adapter.pembeli.katalog.ProductAdapter
import kelompok.satu.raya_kitchen_mobile.ui.adapter.pembeli.katalog.ProductVariantAdapter
import kelompok.satu.raya_kitchen_mobile.utils.Constant
import kelompok.satu.raya_kitchen_mobile.utils.SessionManager
import org.json.JSONObject

class KatalogFragment : Fragment() {

    private var _binding: FragmentKatalogBinding? = null
    private val binding get() = _binding!!

    // Simpan data Model mentah untuk filter
    private var allVariants = mutableListOf<ProductVariant>()
    private var variantAdapter: ProductVariantAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKatalogBinding.inflate(inflater, container, false)

        setupRecyclerViews()
        reqKatalogApi()

        binding.swipeRefresh.setOnRefreshListener {
            reqKatalogApi()
        }

        return binding.root
    }

    private fun setupRecyclerViews() {
        binding.rvKategori.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvProduk.layoutManager = GridLayoutManager(requireContext(), 2)
    }

    private fun reqKatalogApi() {
        binding.swipeRefresh.isRefreshing = true

        val url = Constant.BASE_URL + "pembeli/katalog"
        val sessionManager = SessionManager(requireContext())
        val token = sessionManager.getToken() ?: ""

        val stringRequest = object : StringRequest(Method.GET, url,
            Response.Listener { response ->
                if (_binding == null) return@Listener
                binding.swipeRefresh.isRefreshing = false
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getString("status") == "success") {
                        val dataObj = jsonObject.getJSONObject("data")

                        // 1. Ekstrak Kategori jadi List<Product>
                        val categoriesArray = dataObj.getJSONArray("categories")
                        val categoryList = mutableListOf<Product>()

                        // Tambahkan "Semua" sebagai default filter pertama
                        categoryList.add(Product(id = 0, name = "Semua"))

                        for (i in 0 until categoriesArray.length()) {
                            val catObj = categoriesArray.getJSONObject(i)
                            categoryList.add(Product(
                                id = catObj.getInt("id"),
                                name = catObj.getString("name")
                            ))
                        }

                        // 2. Ekstrak Produk jadi List<ProductVariant>
                        val variantsArray = dataObj.getJSONArray("variants")
                        allVariants.clear()
                        for (i in 0 until variantsArray.length()) {
                            val varObj = variantsArray.getJSONObject(i)
                            allVariants.add(ProductVariant(
                                id = varObj.getInt("id"),
                                productId = varObj.getInt("product_id"),
                                variantName = varObj.getString("variant_name"),
                                price = varObj.getInt("price"),
                                imageUrl = varObj.getString("image_url"),
                                categoryName = varObj.getString("category_name")
                            ))
                        }

                        // 3. Pasang Adapter Kategori (ProductAdapter)
                        val productAdapter = ProductAdapter(categoryList) { selectedCategory ->
                            filterVariants(selectedCategory)
                        }
                        binding.rvKategori.adapter = productAdapter

                        // 4. Pasang Adapter Varian (ProductVariantAdapter)
                        variantAdapter = ProductVariantAdapter(allVariants)
                        binding.rvProduk.adapter = variantAdapter
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Gagal parsing data", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                if (_binding == null) return@ErrorListener
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), "Koneksi bermasalah", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["ngrok-skip-browser-warning"] = "69420"
                headers["Accept"] = "application/json"
                headers["Authorization"] = "Bearer $token"
                return headers
            }
        }

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(stringRequest)
    }

    private fun filterVariants(category: Product) {
        if (category.name == "Semua") {
            variantAdapter?.updateData(allVariants)
        } else {
            // Saring berdasarkan categoryName yang ada di dalam Model
            val filteredList = allVariants.filter {
                it.categoryName == category.name
            }
            variantAdapter?.updateData(filteredList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}