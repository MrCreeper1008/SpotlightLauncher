package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.Gravity
import androidx.core.view.setPadding
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.prefs.SettingsActivity
import kenneth.app.spotlightlauncher.utils.dp

/**
 * Renders an option menu for configuring the launcher.
 * Can be triggered by long pressing the background, or by swiping up when scrolled to bottom.
 */
class LauncherOptionMenu(context: Context, attrs: AttributeSet) : BottomOptionMenu(context, attrs) {
    private val launcherSettingsItem: Item

    init {
        inflate(context, R.layout.launcher_option_menu, this)

        launcherSettingsItem = findViewById(R.id.launcher_settings_item)

        attachListeners()
    }

    private fun attachListeners() {
        launcherSettingsItem.setOnClickListener { openSettings() }
    }

    private fun openSettings() {
        val settingsIntent = Intent(context, SettingsActivity::class.java)
        context.startActivity(settingsIntent)
        hide()
    }
}