package kenneth.app.spotlightlauncher

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.View
import android.view.animation.PathInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.ColorUtils
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var rootView: ConstraintLayout
    private lateinit var appsGridAdapter: AppsGridAdapter
    private lateinit var searcher: Searcher
    private var statusBarHeight: Int = 0

    private lateinit var searchBox: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // enable edge-to-edge app experience

        rootView = findViewById(R.id.root)

        rootView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        searcher = Searcher(packageManager)

        attachListeners()
    }

    private fun attachListeners() {
        searchBox = findViewById(R.id.search_box)

        searchBox.apply {
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) searcher.refreshAppList()
                onSearchBoxFocusChanged(hasFocus)
            }

            setOnEditorActionListener { _, actionID, _ ->
                if (actionID == EditorInfo.IME_ACTION_DONE) {
                    searchBox.clearFocus()
                    false
                } else false
            }

            addTextChangedListener { text -> handleSearchQuery(text) }
        }

        rootView.setOnApplyWindowInsetsListener { _, insets ->
            statusBarHeight = insets.systemWindowInsetTop
            insets
        }

        searcher.setSearchResultListener {
            runOnUiThread {
                displaySearchResult(it)
            }
        }
    }

    private fun onSearchBoxFocusChanged(hasFocus: Boolean) {
        if (searchBox.text.isBlank()) {
            toggleSearchBoxAnimation(hasFocus)
        }
    }

    private fun toggleSearchBoxAnimation(isActive: Boolean) {
        val inactiveSearchBoxConstraints = ConstraintSet()
        val activeSearchBoxConstraints = ConstraintSet()

        if (isActive) {
            inactiveSearchBoxConstraints.clone(rootView)
            activeSearchBoxConstraints.clone(rootView)
            activeSearchBoxConstraints.clear(R.id.page, ConstraintSet.BOTTOM)

            findViewById<LinearLayout>(R.id.search_box_container)
                .alpha = 1.0f

            findViewById<ConstraintLayout>(R.id.root)
                .setBackgroundColor(
                    ColorUtils.setAlphaComponent(android.R.attr.colorBackground, 128)
                )
        } else {
            inactiveSearchBoxConstraints.clone(rootView)
            activeSearchBoxConstraints.clone(rootView)
            inactiveSearchBoxConstraints.connect(
                R.id.page,
                ConstraintSet.BOTTOM,
                R.id.root,
                ConstraintSet.BOTTOM
            )

            findViewById<LinearLayout>(R.id.search_box_container)
                .alpha = 0.3f

            findViewById<ConstraintLayout>(R.id.root)
                .setBackgroundColor(
                    ColorUtils.setAlphaComponent(android.R.attr.colorBackground, 0)
                )
        }

        val transition = ChangeBounds()

        transition.duration = 500L
        transition.interpolator = PathInterpolator(0.13f, 0.73f, 0.22f, 0.98f)

        TransitionManager.beginDelayedTransition(rootView, transition)

        val constraints =
            if (isActive) activeSearchBoxConstraints
            else inactiveSearchBoxConstraints

        constraints.applyTo(root)
    }

    private fun handleSearchQuery(query: Editable?) {
        if (query == null || query.isBlank()) {
            searcher.cancelPendingSearch()

            findViewById<MaterialCardView>(R.id.apps_section_card)
                .visibility = View.GONE
        } else {
            searcher.requestSearch(query.toString())
        }
    }

    private fun displaySearchResult(result: Searcher.Result) {
        if (!::appsGridAdapter.isInitialized) {
            appsGridAdapter = AppsGridAdapter(packageManager)
        }

        appsGridAdapter.appList = result.apps

        findViewById<RecyclerView>(R.id.apps_grid).apply {
            layoutManager = GridLayoutManager(context, 5)
            adapter = appsGridAdapter
        }

        findViewById<MaterialCardView>(R.id.apps_section_card).visibility = View.VISIBLE
    }
}
