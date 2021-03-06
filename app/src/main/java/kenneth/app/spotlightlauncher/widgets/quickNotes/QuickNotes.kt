package kenneth.app.spotlightlauncher.widgets.quickNotes

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ActivityContext
import kenneth.app.spotlightlauncher.databinding.QuickNotesLayoutBinding
import kenneth.app.spotlightlauncher.databinding.QuickNotesListItemBinding
import kenneth.app.spotlightlauncher.models.Note
import kenneth.app.spotlightlauncher.prefs.notes.NotesPreferenceManager
import kenneth.app.spotlightlauncher.utils.BindingRegister
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter
import javax.inject.Inject

@AndroidEntryPoint
class QuickNotes(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    @Inject
    lateinit var notesPreferenceManager: NotesPreferenceManager

    @Inject
    lateinit var noteListAdapter: NoteListAdapter

    private val binding: QuickNotesLayoutBinding =
        QuickNotesLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        with(binding) {
            addNoteButton.setOnClickListener { addNote() }

            with(addNotesEditText) {
                setOnFocusChangeListener { _, _ -> moveWayForKeyboard() }
                setOnClickListener { moveWayForKeyboard() }

                addTextChangedListener {
                    addNoteButton.isInvisible = it?.isBlank() == true
                }
            }

            val savedNotes = notesPreferenceManager.notes

            noteListAdapter.data = savedNotes

            if (savedNotes.isNotEmpty()) {
                quickNotesList.apply {
                    isVisible = true
                    adapter = noteListAdapter
                    layoutManager = noteListAdapter.layoutManager
                }
            }

            quickNotesWidgetBlurBackground.startBlur()
            notesPreferenceManager.setOnNoteListChangedListener {
                toggleShowAllButtonVisibility()
            }
        }

        toggleShowAllButtonVisibility()
    }

    private fun toggleShowAllButtonVisibility() {
        binding.showAllNotesButton.isVisible = notesPreferenceManager.notes.isNotEmpty()
    }

    private fun moveWayForKeyboard() {
        if (binding.addNotesEditText.hasFocus() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val yPos = intArrayOf(0, 0).run {
                this@QuickNotes.getLocationOnScreen(this)
                this[1]
            }

            BindingRegister.activityMainBinding.pageScrollView
                .moveWayForKeyboard(yPos)
        }
    }

    private fun addNote() {
        binding.addNotesEditText.text?.let {
            if (it.isNotBlank()) {
                val newNote = Note(content = it.toString(), dueDateTimestamp = null)
                noteListAdapter.apply {
                    data = data + newNote
                    notifyItemInserted(data.size)
                }
                notesPreferenceManager.addNote(newNote)
            }
        }
    }
}

class NoteListAdapter @Inject constructor(
    @ActivityContext private val context: Context,
    private val notesPreferenceManager: NotesPreferenceManager,
) : RecyclerViewDataAdapter<Note, NoteListItem>() {
    override val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)

    /**
     * The [RecyclerView] this adapter is attached to.
     */
    lateinit var recyclerView: RecyclerView

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteListItem {
        val binding =
            QuickNotesListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return NoteListItem(this, binding, notesPreferenceManager)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }
}

/**
 * An item in [NoteListAdapter].
 * @param adapter The [NoteListAdapter] this is in.
 * @param binding The view binding of the layout of this item
 * @param notesPreferenceManager The instance of [NotesPreferenceManager] this should use to modify the list of notes
 */
class NoteListItem(
    private val adapter: NoteListAdapter,
    private val binding: QuickNotesListItemBinding,
    private val notesPreferenceManager: NotesPreferenceManager
) :
    RecyclerViewDataAdapter.ViewHolder<Note>(binding.root) {
    private lateinit var note: Note

    override fun bindWith(data: Note) {
        note = data

        with(binding) {
            quickNotesListItemContent.text = data.content
            quickNotesListItemDeleteButton.setOnClickListener {
                deleteNote()
            }
            quickNotesListItemSeparator.isVisible = adapterPosition > 0
        }
    }

    private fun deleteNote() {
        val currentPosition = adapterPosition

        notesPreferenceManager.deleteNote(note)
        adapter.data = adapter.data.filter { it != note }
        adapter.notifyItemRemoved(currentPosition)

        if (currentPosition == 0) {
            redrawSeparators()
        }
    }

    /**
     * When the first item is deleted, the second item will become the first item, but
     * its separator will still be visible. Since that item is now the first item,
     * that separator is no longer needed and needs to be hidden.
     *
     * This method finds the first item in the list and tells it to hide the separator.
     */
    private fun redrawSeparators() {
        val viewOfFirstItem = adapter.recyclerView.getChildAt(1)

        if (viewOfFirstItem != null) {
            val holderOfFirstItem =
                (adapter.recyclerView.getChildViewHolder(viewOfFirstItem) as NoteListItem)

            holderOfFirstItem.binding.quickNotesListItemSeparator.isVisible = false
        }
    }
}
