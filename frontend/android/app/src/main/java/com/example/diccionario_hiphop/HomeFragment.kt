package com.example.diccionario_hiphop

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.diccionario_hiphop.utils.NetworkMonitor
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

class HomeFragment : Fragment(R.layout.fragment_home) {

    // --- UI Components ---
    private lateinit var recyclerView: RecyclerView
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var searchView: SearchView
    private lateinit var tvHeader: TextView
    private lateinit var tvOfflineMessage: TextView
    private lateinit var layoutOffline: View
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvDestacados: TextView
    private lateinit var tvTodas: TextView

    // --- Banner / ViewPager2 ---
    private lateinit var viewPagerBanner: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var bannerContainer: View
    private lateinit var bannerAdapter: BannerAdapter

    // --- Selector de Idioma (CardView con emoji) ---
    private lateinit var cardLanguageSelector: CardView
    private lateinit var tvSelectedLanguage: TextView

    // --- Adapters & Data ---
    private lateinit var adapter: SongAdapter
    private var grammySongs: List<SongItem> = emptyList()
    private lateinit var repository: SongRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var networkMonitor: NetworkMonitor

    private var searchJob: Job? = null
    private var userLanguages: List<LearningLanguage> = emptyList()
    private var selectedLanguage: String = "en"
    private var selectedLevel: String = "B1"

    // --- Auto-Scroll del Banner ---
    private val sliderHandler = Handler(Looper.getMainLooper())
    private val sliderRunnable = Runnable {
        if (::viewPagerBanner.isInitialized && ::bannerAdapter.isInitialized) {
            var nextItem = viewPagerBanner.currentItem + 1
            if (nextItem >= bannerAdapter.itemCount) {
                nextItem = 0
            }
            viewPagerBanner.setCurrentItem(nextItem, true)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = SongRepository(requireContext())
        profileRepository = ProfileRepository(requireContext())
        networkMonitor = NetworkMonitor.getInstance(requireContext())

        initViews(view)
        lifecycleScope.launch {
            val response = repository.getTopGrammy(selectedLanguage)
            if (response.isSuccessful && response.body() != null) {
                grammySongs = response.body()!!
            }
            setupRecyclerView()
        }
        setupBanner()
        setupSearchView()

        lifecycleScope.launch {
            networkMonitor.isConnected.collect { isOnline ->
                if (!isOnline) {
                    showOfflineState()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sliderHandler.postDelayed(sliderRunnable, 4000)

        if (networkMonitor.isConnected.value) {
            loadUserProfile()
        } else {
            showOfflineState()
        }
    }

    override fun onPause() {
        super.onPause()
        sliderHandler.removeCallbacks(sliderRunnable)
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.rvSongs)
        shimmerContainer = view.findViewById(R.id.shimmerViewContainer)
        searchView = view.findViewById(R.id.searchView)
        tvHeader = view.findViewById(R.id.tvHeader)
        tvOfflineMessage = view.findViewById(R.id.tvOfflineMessage)
        layoutOffline = view.findViewById(R.id.layoutOffline)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        tvDestacados = view.findViewById(R.id.tvDestacados)
        tvTodas = view.findViewById(R.id.tvTodas)

        // Banner
        viewPagerBanner = view.findViewById(R.id.viewPagerBanner)
        tabLayout = view.findViewById(R.id.tabLayoutIndicator)
        bannerContainer = view.findViewById(R.id.bannerContainer)

        // Selector de idioma
        cardLanguageSelector = view.findViewById(R.id.cardLanguageSelector)
        tvSelectedLanguage = view.findViewById(R.id.tvSelectedLanguage)

        swipeRefresh.setOnRefreshListener {
            loadUserProfile()
        }
    }

    private fun setupRecyclerView() {
        adapter = SongAdapter(emptyList(), { song ->
            checkLanguageBeforeOpening(song)
        }, selectedLanguage, grammySongs)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.isNestedScrollingEnabled = false
    }

    private fun setupBanner() {
        viewPagerBanner.clipToPadding = false
        viewPagerBanner.clipChildren = false
        viewPagerBanner.offscreenPageLimit = 3
        viewPagerBanner.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer(30))
        compositePageTransformer.addTransformer { page, position ->
            val r = 1 - abs(position)
            page.scaleY = 0.85f + r * 0.15f
        }
        viewPagerBanner.setPageTransformer(compositePageTransformer)

        viewPagerBanner.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                sliderHandler.removeCallbacks(sliderRunnable)
                sliderHandler.postDelayed(sliderRunnable, 4000)
            }
        })
    }

    private fun setupSearchView() {
        searchView.queryHint = "🎧 Busca tu flow..."
        val searchEditText = searchView.findViewById<android.widget.EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText?.apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            isSingleLine = true
            // Cambia el color del texto según el modo (oscuro/blanco, claro/negro)
            val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            if (isDark) {
                setTextColor(resources.getColor(android.R.color.white, null))
                setHintTextColor(resources.getColor(android.R.color.darker_gray, null))
            } else {
                setTextColor(resources.getColor(android.R.color.black, null))
                setHintTextColor(resources.getColor(android.R.color.darker_gray, null))
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchJob?.cancel()
                    performSearch(query, isRecommendation = false)
                    searchView.clearFocus()
                    tvDestacados.visibility = View.GONE
                    tvTodas.visibility = View.GONE
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()

                if (newText.isNullOrEmpty()) {
                    performSearch(getRecommendationQuery(selectedLanguage, selectedLevel), isRecommendation = true)
                    tvDestacados.visibility = View.VISIBLE
                    tvTodas.visibility = View.VISIBLE
                } else if (newText.length >= 2) {
                    searchJob = lifecycleScope.launch {
                        delay(400)
                        performSearch(newText, showShimmer = false, isRecommendation = false)
                        tvDestacados.visibility = View.GONE
                        tvTodas.visibility = View.GONE
                    }
                }
                return true
            }
        })

        searchView.setOnCloseListener {
            performSearch(getRecommendationQuery(selectedLanguage, selectedLevel), isRecommendation = true)
            tvDestacados.visibility = View.VISIBLE
            tvTodas.visibility = View.VISIBLE
            false
        }
    }

    private fun loadUserProfile() {
        searchView.isEnabled = true
        searchView.alpha = 1f
        layoutOffline.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = profileRepository.getProfile()
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    userLanguages = user.learningLanguages?.filter { it.isActive } ?: emptyList()

                    if (userLanguages.isNotEmpty()) {
                        val primaryLang = userLanguages.find { it.language == user.primaryLanguage } ?: userLanguages.first()
                        selectedLanguage = primaryLang.language
                        selectedLevel = primaryLang.level

                        setupLanguageSelector()
                        performSearch(getRecommendationQuery(selectedLanguage, selectedLevel), isRecommendation = true)
                    } else {
                        performSearch("Viral 50 Global", isRecommendation = true)
                    }
                } else {
                    performSearch("Viral 50 Global", isRecommendation = true)
                }
            } catch (e: Exception) {
                performSearch("Viral 50 Global", isRecommendation = true)
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun setupLanguageSelector() {
        val currentLang = userLanguages.find { it.language == selectedLanguage } ?: userLanguages.firstOrNull()

        if (currentLang != null) {
            tvSelectedLanguage.text = getLanguageEmoji(currentLang.language)
            selectedLanguage = currentLang.language
            selectedLevel = currentLang.level
        }

        cardLanguageSelector.setOnClickListener {
            showLanguageBottomSheet()
        }
    }

    private fun showLanguageBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_language_selector, null)
        bottomSheetDialog.setContentView(view)

        val container = view.findViewById<LinearLayout>(R.id.containerLanguages)

        userLanguages.forEach { lang ->
            val itemLayout = LinearLayout(requireContext())
            itemLayout.orientation = LinearLayout.HORIZONTAL
            itemLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            itemLayout.setPadding(30, 40, 30, 40)
            itemLayout.gravity = Gravity.CENTER_VERTICAL

            val outValue = TypedValue()
            requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            itemLayout.setBackgroundResource(outValue.resourceId)

            val textView = TextView(requireContext())
            val emoji = getLanguageEmoji(lang.language)
            val name = getLanguageName(lang.language)
            textView.text = "$emoji  $name • ${lang.level}"
            textView.textSize = 18f
            textView.setTextColor(resources.getColor(R.color.text_primary, null))

            if (lang.language == selectedLanguage) {
                textView.setTypeface(null, Typeface.BOLD)
                textView.setTextColor(resources.getColor(R.color.purple_500, null))
            }

            itemLayout.addView(textView)

            itemLayout.setOnClickListener {
                selectedLanguage = lang.language
                selectedLevel = lang.level

                setupLanguageSelector()
                setupRecyclerView() // Recrear adapter con nuevo idioma para Grammy
                performSearch(getRecommendationQuery(selectedLanguage, selectedLevel), isRecommendation = true)

                bottomSheetDialog.dismiss()
            }

            container.addView(itemLayout)

            val divider = View(requireContext())
            divider.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            divider.setBackgroundColor(resources.getColor(R.color.text_secondary, null))
            divider.alpha = 0.2f
            container.addView(divider)
        }

        bottomSheetDialog.show()
    }

    /**
     * ✅ VALIDACIÓN DE IDIOMAS (nuestra lógica)
     */
    private fun checkLanguageBeforeOpening(song: SongItem) {
        val isLanguageActive = userLanguages.any { it.language == selectedLanguage && it.isActive }

        if (!isLanguageActive) {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Idioma no configurado")
                .setMessage("Esta canción está en ${getLanguageName(selectedLanguage)}, pero no lo tienes como idioma de aprendizaje activo.\n\n¿Quieres continuar? No podrás guardar palabras ni crear flashcards.")
                .setPositiveButton("Continuar de todos modos") { _, _ ->
                    openSongLearningActivity(song, allowSaving = false)
                }
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Configurar idiomas") { _, _ ->
                    val intent = Intent(requireContext(), ManageLanguagesActivity::class.java)
                    startActivity(intent)
                }
                .show()
        } else {
            openSongLearningActivity(song, allowSaving = true)
        }
    }

    /**
     * 🏆 ABRIR CANCIÓN CON GRAMMY CHECK (nuestra lógica)
     */
    private fun openSongLearningActivity(song: SongItem, allowSaving: Boolean) {
        lifecycleScope.launch {
            var isGrammy = false
            try {
                val apiService = RetrofitService.getInstance(requireContext())
                val response = apiService.checkGrammyStatus(song.title, song.artist, selectedLanguage)
                isGrammy = response.isSuccessful && response.body()?.isGrammy == true
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Grammy check error: ${e.message}")
            }

            val intent = Intent(requireContext(), SongLearningActivity::class.java)
            intent.putExtra("song_title", song.title)
            intent.putExtra("song_artist", song.artist)
            intent.putExtra("song_image", song.imageUrl)
            intent.putExtra("song_audio", song.previewUrl)
            intent.putExtra("allow_saving", allowSaving)
            intent.putExtra("language_code", selectedLanguage)
            intent.putExtra("is_grammy_theme", isGrammy)
            startActivity(intent)
        }
    }

    private fun performSearch(query: String, showShimmer: Boolean = true, isRecommendation: Boolean = false) {
        val displayTitle = if (isRecommendation) "Filtrar por idioma" else "Resultados para '$query'"
        tvHeader.text = displayTitle

        // Ocultar/mostrar banner y selector según si es búsqueda o recomendación
        if (isRecommendation) {
            bannerContainer.visibility = View.VISIBLE
            cardLanguageSelector.visibility = View.VISIBLE
        } else {
            bannerContainer.visibility = View.GONE
            cardLanguageSelector.visibility = View.GONE
        }

        if (showShimmer) showLoading(true)

        lifecycleScope.launch {
            try {
                val response = repository.searchSongs(query)
                if (response.isSuccessful && response.body() != null) {
                    val results = response.body()!!

                    // Actualizar lista vertical
                    // Filtrar resultados nulos o vacíos
                    val filteredResults = results.filter {
                        !it.title.isNullOrBlank() && !it.artist.isNullOrBlank()
                    }
                    adapter.updateData(filteredResults)

                    // Actualizar banner (solo en recomendaciones)
                    if (isRecommendation && results.isNotEmpty()) {
                        val bannerData = results.shuffled().take(5)
                        bannerAdapter = BannerAdapter(bannerData) { song -> checkLanguageBeforeOpening(song) }
                        viewPagerBanner.adapter = bannerAdapter
                        TabLayoutMediator(tabLayout, viewPagerBanner) { _, _ -> }.attach()
                    }

                    if (results.isEmpty() && !isRecommendation) {
                        Toast.makeText(requireContext(), "No se encontraron resultados", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                if (networkMonitor.isConnected.value) {
                    Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            shimmerContainer.startShimmer()
            shimmerContainer.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            if (::bannerContainer.isInitialized) bannerContainer.visibility = View.GONE
        } else {
            shimmerContainer.stopShimmer()
            shimmerContainer.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            if (cardLanguageSelector.visibility == View.VISIBLE) {
                bannerContainer.visibility = View.VISIBLE
            } else {
            // Aseguramos que se mantenga oculto si es una búsqueda
            bannerContainer.visibility = View.GONE 
            }
        }
    }

    private fun showOfflineState() {
        showLoading(false)
        adapter.updateData(emptyList())

        searchView.isEnabled = false
        searchView.alpha = 0.5f
        cardLanguageSelector.visibility = View.GONE
        tvHeader.visibility = View.GONE
        bannerContainer.visibility = View.GONE

        layoutOffline.visibility = View.VISIBLE

        swipeRefresh.isRefreshing = false
        swipeRefresh.isEnabled = false
    }

    private fun getLanguageEmoji(code: String): String = when(code) {
        "es" -> "🇪🇸"; "fr" -> "🇫🇷"; "de" -> "🇩🇪"; "pt" -> "🇵🇹"; "it" -> "🇮🇹"
        "ja" -> "🇯🇵"; "ko" -> "🇰🇷"; "zh" -> "🇨🇳"; else -> "🇬🇧"
    }

    private fun getLanguageName(code: String): String = when(code) {
        "es" -> "Español"; "fr" -> "Francés"; "de" -> "Alemán"; "pt" -> "Portugués"
        "it" -> "Italiano"; "ja" -> "Japonés"; "ko" -> "Coreano"; "zh" -> "Chino"; else -> "Inglés"
    }

    private fun getRecommendationQuery(lang: String, level: String): String {
        return when(lang) {
            "es" -> "Top 50 Spain"
            "fr" -> "Top 50 France"
            "de" -> "Top 50 Germany"
            "ja" -> "J-Pop Hits"
            "ko" -> "K-Pop Daebak"
            else -> "Today's Top Hits"
        }
    }
}
