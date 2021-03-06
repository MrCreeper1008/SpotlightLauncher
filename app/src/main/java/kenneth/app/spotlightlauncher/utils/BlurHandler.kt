package kenneth.app.spotlightlauncher.utils

import android.app.WallpaperManager
import android.content.Context
import android.graphics.*
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import android.widget.ImageView
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.spotlightlauncher.AppState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Defines how much a bitmap should be scaled before being blurred
 */
private const val BLUR_SCALE = 0.2f

@Module
@InstallIn(ApplicationComponent::class)
object BlurHandlerModule {
    @Provides
    fun provideWallpaperManager(@ApplicationContext context: Context): WallpaperManager =
        WallpaperManager.getInstance(context)

    @Provides
    fun provideRenderScript(@ApplicationContext context: Context): RenderScript =
        RenderScript.create(context)
}

/**
 * Handles blur background of cards in the home screen
 */
@Singleton
class BlurHandler @Inject constructor(
    private val renderScript: RenderScript,
    private val appState: AppState
) {
    /**
     * The wallpaper of the home screen. This wallpaper will be used to create blur backgrounds
     * for cards in the home screen.
     */
    private var wallpaper: Bitmap? = null

    /**
     * Determines if wallpaper should be blurred. This should be true when
     * the blurred version of has not yet been created.
     */
    private var shouldBlurWallpaper = true

    /**
     * The blurred version of the wallpaper
     */
    private var blurredWallpaper: Bitmap? = null

    /**
     * Change the wallpaper to be blurred.
     * @param newWallpaperBitmap [Bitmap] representation of the new wallpaper.
     */
    fun changeWallpaper(newWallpaperBitmap: Bitmap) {
        wallpaper = newWallpaperBitmap
        shouldBlurWallpaper = true
    }

    /**
     * Sets the blurred wallpaper as the background of the given [ImageView].
     * @param dest The [ImageView] this function should set the blurred wallpaper for.
     * @param blurAmount Amount of blur
     */
    fun blurView(dest: ImageView, blurAmount: Int) {
        if (shouldBlurWallpaper) {
            Log.i("", "blur")
            cacheBlurredWallpaper(blurAmount.toFloat())
            shouldBlurWallpaper = false
        }

        blurredWallpaper?.let {
            if (dest.width <= 0 || dest.height <= 0) return

            val viewCoordinates = IntArray(2)
            dest.getLocationOnScreen(viewCoordinates)
            val (viewX, viewY) = viewCoordinates

            val bitmapX = it.width * max(viewX, 0) / appState.screenWidth
            val bitmapY =
                it.height * max(min(viewY, appState.screenHeight), 0) / appState.screenHeight

            if (bitmapY >= it.height || bitmapX > it.width) return

            val bgWidth = min(it.width * dest.width / appState.screenWidth, appState.screenWidth)
            val scaledHeight = it.height * dest.height / appState.screenHeight
            val bgHeight =
                if (bitmapY + scaledHeight > it.height)
                    it.height - bitmapY
                else
                    scaledHeight

            if (bgWidth > 0 && bgHeight > 0 && bitmapX + bgWidth <= it.width && bitmapY + bgHeight <= it.height) {
                val bitmap = Bitmap.createBitmap(
                    it,
                    bitmapX,
                    bitmapY,
                    bgWidth,
                    bgHeight,
                )
                val bgAspectRatio = bgWidth.toFloat() / bgHeight
                val destWidthFloat = dest.width.toFloat()

                dest.imageMatrix = Matrix().apply {
                    setTranslate(0f, if (viewY < 0) -viewY.toFloat() else 0f)
                    preScale(
                        destWidthFloat / bgWidth,
                        destWidthFloat / bgAspectRatio / bgHeight,
                    )
                }

                dest.setImageBitmap(bitmap)
            }
        }
    }

    /**
     * Tries to cache the current wallpaper to this.wallpaperDrawable.
     *
     * If READ_EXTERNAL_STORAGE is not granted, nothing will be cached
     */
    private fun cacheBlurredWallpaper(blurRadius: Float) {
        wallpaper?.let {
            blurredWallpaper = createBlurredBitmap(
                it,
                blurRadius,
            )
        }
    }

    /**
     * Creates a blurred version of the wallpaper with renderscript.
     *
     * Credit to [this StackOverflow post](https://stackoverflow.com/questions/6795483/create-blurry-transparent-background-effect/21052060#21052060)
     */
    private fun createBlurredBitmap(bitmap: Bitmap, blurRadius: Float): Bitmap {
        val scaledWidth = (bitmap.width * BLUR_SCALE).roundToInt()
        val scaledHeight = (bitmap.height * BLUR_SCALE).roundToInt()

        val scaledBitmap =
            Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false)
        val blurredBitmap = Bitmap.createBitmap(scaledBitmap)

        val allocationIn = Allocation.createFromBitmap(renderScript, scaledBitmap)
        val allocationOut = Allocation.createFromBitmap(renderScript, blurredBitmap)

        with(ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))) {
            setRadius(blurRadius)
            setInput(allocationIn)
            forEach(allocationOut)
        }

        allocationOut.copyTo(blurredBitmap)
        allocationOut.destroy()
        allocationIn.destroy()
        scaledBitmap.recycle()

        return blurredBitmap
    }
}
