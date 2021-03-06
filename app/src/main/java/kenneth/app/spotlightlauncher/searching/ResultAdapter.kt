package kenneth.app.spotlightlauncher.searching

import androidx.core.view.isVisible
import dagger.hilt.android.scopes.ActivityScoped
import kenneth.app.spotlightlauncher.utils.BindingRegister
import javax.inject.Inject

/**
 * ResultAdapter is used to adapt search results into views. It combines numerous RecyclerView
 * adapters into one class.
 */
class ResultAdapter @Inject constructor() {
    fun displayResult(result: Searcher.Result, type: SearchType) {
        with(BindingRegister.activityMainBinding) {
            widgetList.isVisible = false
            searchResultContainer.isVisible = true
        }

        with(BindingRegister.searchResultViewBinding) {
            when (type) {
                SearchType.ALL -> {
                    appsSectionCard.display(result.apps)
                    filesSectionCard.display(result.files)
                    suggesedResultCard.display(result.suggested)
                }
                SearchType.FILES -> {
                    filesSectionCard.display(result.files)
                }
                SearchType.APPS -> {
                    appsSectionCard.display(result.apps)
                }
                SearchType.SUGGESTED -> {
                    suggesedResultCard.display(result.suggested)
                }
            }
        }
    }

    fun hideResult() {
        with(BindingRegister.searchResultViewBinding) {
            appsSectionCard.hide()
            filesSectionCard.hide()
            webResultCard.hide()
            suggesedResultCard.hide()
        }

        BindingRegister.activityMainBinding.searchResultContainer.isVisible = false
    }

    fun displayWebResult(result: SmartSearcher.WebResult) {
        BindingRegister.searchResultViewBinding.webResultCard.display(result)
    }

    /**
     * Perform cleanup work on adapters
     */
    fun cleanup() {

    }
}