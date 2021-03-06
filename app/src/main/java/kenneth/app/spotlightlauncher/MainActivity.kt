package kenneth.app.spotlightlauncher

import android.Manifest
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import kenneth.app.spotlightlauncher.databinding.ActivityMainBinding
import kenneth.app.spotlightlauncher.prefs.appearance.AppearancePreferenceManager
import kenneth.app.spotlightlauncher.searching.SearchType
import kenneth.app.spotlightlauncher.searching.Searcher
import kenneth.app.spotlightlauncher.searching.ResultAdapter
import kenneth.app.spotlightlauncher.utils.*
import javax.inject.Inject

@Module
@InstallIn(ActivityComponent::class)
object MainActivityModule {
    @Provides
    fun provideMainActivity(@ActivityContext context: Context): MainActivity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is MainActivity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var searcher: Searcher

    @Inject
    lateinit var resultAdapter: ResultAdapter

    @Inject
    lateinit var blurHandler: BlurHandler

    @Inject
    lateinit var wallpaperManager: WallpaperManager

    @Inject
    lateinit var appState: AppState

    @Inject
    lateinit var appearancePreferenceManager: AppearancePreferenceManager

    @Inject
    lateinit var inputMethodManager: InputMethodManager

    private lateinit var binding: ActivityMainBinding

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private var isDarkModeActive = false

    /**
     * Right before requesting a permission, it is stored in this variable, so that when
     * the request result comes back, we know what permission is being requested, and we
     * can hide the corresponding button.
     */
    private var requestedPermission: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateAdaptiveColors()
        setTheme(appState.themeStyleId)

        appState.apply {
            screenWidth = resources.displayMetrics.widthPixels
            screenHeight = resources.displayMetrics.heightPixels
        }

        binding = ActivityMainBinding.inflate(layoutInflater).also {
            BindingRegister.activityMainBinding = it
        }

        setContentView(binding.root)

        appearancePreferenceManager.iconPack?.load()

        // enable edge-to-edge app experience

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            binding.root.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        } else {
            window.setDecorFitsSystemWindows(false)
        }

        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission(),
                ::handlePermissionResult
            )

        isDarkModeActive =
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) resources.configuration.isNightModeActive
            else resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        attachListeners()
        askForReadExternalStoragePermission()
    }

    override fun onBackPressed() {
        when {
            binding.launcherOptionMenu.isActive -> {
                binding.launcherOptionMenu.hide()
            }
            binding.appOptionMenu.isVisible -> {
                binding.appOptionMenu.hide()
            }
            appState.isWidgetPanelExpanded -> {
                binding.pageScrollView.retractWidgetPanel()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        isDarkModeActive =
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) newConfig.isNightModeActive
            else newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        if (appState.isSearchBoxActive) {
            if (isDarkModeActive) {
                disableLightStatusBar()
            } else {
                enableLightStatusBar()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateWallpaper()
    }

    override fun onStop() {
        cleanup()
        super.onStop()
    }

    override fun onPause() {
        cleanup()
        super.onPause()
    }

    private fun cleanup() {
        resultAdapter.cleanup()
    }

    /**
     * a temporary function to ask for READ_EXTERNAL_STORAGE permission
     */
    private fun askForReadExternalStoragePermission() {
        // TODO: a temporary function to ask for READ_EXTERNAL_STORAGE permission
        // TODO: in prod this should be done during setup
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            requestedPermission = Manifest.permission.READ_EXTERNAL_STORAGE
            requestPermissionLauncher.launch(requestedPermission)
        }
    }

    private fun attachListeners() {
        with(binding.root) {
            setOnApplyWindowInsetsListener { _, insets ->
                appState.statusBarHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val systemBarsInset = insets.getInsets(WindowInsets.Type.systemBars())
                    systemBarsInset.top
                } else {
                    insets.systemWindowInsetTop
                }

                insets
            }
        }

        binding.root.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    updateWallpaper()
                }
            }
        )
    }

    /**
     * Update the current adaptive color scheme by finding the dominant color of the current wallpaper.
     */
    private fun updateAdaptiveColors() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            val wallpaperBitmap = wallpaperManager.fastDrawable.toBitmap()
            appState.apply {
                adaptiveBackgroundColor = wallpaperBitmap.calculateDominantColor()

                val white = getColor(android.R.color.white)
                val black = ColorUtils.setAlphaComponent(getColor(android.R.color.black), 0x80)

                adaptiveTextColor =
                    if (ColorUtils.calculateContrast(white, adaptiveBackgroundColor) > 1.5f)
                        white
                    else black
            }

            if (appState.isInitialStart) {
                appState.isInitialStart = false
            } else {
                // restart activity after changing theme
                restartActivity()
            }
        }
    }

    /**
     * Restarts the current activity.
     * Credit to [this gist](https://gist.github.com/alphamu/f2469c28e17b24114fe5)
     */
    private fun restartActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun handlePermissionResult(isGranted: Boolean) {
        if (!isGranted) return

        val query = findViewById<EditText>(R.id.search_box).text.toString()

        when (requestedPermission) {
            Manifest.permission.READ_EXTERNAL_STORAGE -> {
                if (appState.isSearchBoxActive) {
                    searcher.requestSpecificSearch(
                        SearchType.FILES,
                        query
                    )
                }
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun enableLightStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window
                .insetsController
                ?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
        } else if (Build.VERSION.SDK_INT in (Build.VERSION_CODES.M..Build.VERSION_CODES.Q) && !isDarkModeActive) {
            binding.root.systemUiVisibility =
                binding.root.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun disableLightStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window
                .insetsController
                ?.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
        } else if (Build.VERSION.SDK_INT in (Build.VERSION_CODES.M..Build.VERSION_CODES.Q) && !isDarkModeActive) {
            binding.root.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    /**
     * Gets the current wallpaper and renders it to binding.wallpaperImage
     */
    private fun updateWallpaper() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            val wallpaper = wallpaperManager.fastDrawable
            binding.wallpaperImage.setImageDrawable(wallpaper)

            if (binding.wallpaperImage.width > 0 && binding.wallpaperImage.height > 0) {
                blurHandler.changeWallpaper(viewToBitmap(binding.wallpaperImage))
            }
        }
    }
}
