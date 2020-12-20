package kenneth.app.spotlightlauncher.utils

import android.content.res.Resources
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsAnimation
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import kenneth.app.spotlightlauncher.MainActivity
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.views.LauncherScrollView

val Int.dp: Int
    get() = this * Resources.getSystem().displayMetrics.density.toInt()

val WindowInsets.navBarHeight: Int
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            getInsets(WindowInsets.Type.systemBars()).bottom
        else
            systemWindowInsetBottom

@RequiresApi(Build.VERSION_CODES.R)
class KeyboardAnimationCallback(activity: MainActivity) :
    WindowInsetsAnimation.Callback(DISPATCH_MODE_STOP) {
    private var shouldAnimateKeyboard = false
    private val pageScrollView = activity.findViewById<LauncherScrollView>(R.id.page_scroll_view)
    private var paddingBottom = pageScrollView.paddingBottom

    init {
        activity.findViewById<TextView>(R.id.search_box)
            .addTextChangedListener { text ->
                shouldAnimateKeyboard = text?.isNotEmpty() ?: false
                pageScrollView.updatePadding(
                    bottom =
                    if (shouldAnimateKeyboard) pageScrollView.rootWindowInsets
                        .getInsets(WindowInsets.Type.ime())
                        .bottom
                    else 0
                )
            }
    }

    override fun onProgress(
        insets: WindowInsets,
        animations: MutableList<WindowInsetsAnimation>
    ): WindowInsets {
        if (shouldAnimateKeyboard) {
            pageScrollView.updatePadding(bottom = paddingBottom + insets.getInsets(WindowInsets.Type.ime()).bottom)
        }
        return insets
    }
}
