package kenneth.app.spotlightlauncher.utils

import android.app.Activity
import android.view.View
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.spotlightlauncher.MainActivity

/**
 * An abstraction over RecyclerView.Adapter that delegates data binding (onBindViewHolder)
 * to RecyclerViewDataAdapter.ViewHolder, so view logic of individual [ViewHolder] can be
 * separated from the adapter.
 */
abstract class RecyclerViewDataAdapter<T, VH : RecyclerViewDataAdapter.ViewHolder<T>> :
    RecyclerView.Adapter<VH>() {
    /**
     * The LayoutManager that RecyclerView should use.
     */
    abstract val layoutManager: RecyclerView.LayoutManager

    /**
     * The data to be displayed by this adapter. Exposed to allow access to the data this adapter
     * is holding.
     */
    open lateinit var data: List<T>

    /**
     * This is responsible for holding views for this adapter. View logic of individual
     * items in the RecyclerView should be handled in this class.
     */
    abstract class ViewHolder<T>(
        view: View,
//        protected val activity: Activity
    ) : RecyclerView.ViewHolder(view) {
        /**
         * Binds this ViewHolder with the given data, and also displays the data.
         */
        abstract fun bindWith(data: T)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = data[position]
        holder.bindWith(item)
    }

    override fun getItemCount() = data.size
}
