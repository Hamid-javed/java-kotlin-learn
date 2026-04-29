package com.rameez.hel.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rameez.hel.R
import com.rameez.hel.databinding.FragmentBulkActionBinding
import com.rameez.hel.viewmodel.WIPViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BulkActionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentBulkActionBinding? = null
    private val binding get() = _binding!!

    private lateinit var wipViewModel: WIPViewModel
    private var selectedIds: List<Int> = emptyList()

    /** Fired after a successful bulk apply — receives the ids that were edited. */
    var onBulkActionApplied: ((List<Int>) -> Unit)? = null

    /**
     * Fired whenever the sheet is dismissed — whether the user applied changes, cancelled,
     * swiped away, or tapped outside. The host uses this to re-fetch the list so the UI
     * never ends up showing a stale snapshot after the sheet closes.
     */
    var onBulkActionDismissed: (() -> Unit)? = null

    private val allCheckboxes = mutableListOf<CheckBox>()

    // Selected timestamps per field (null = clear to 0). Only read when the paired checkbox is checked.
    private val pickedTimestamps = mutableMapOf<Int, Long?>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBulkActionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wipViewModel = ViewModelProvider(requireActivity())[WIPViewModel::class.java]
        selectedIds = arguments?.getIntegerArrayList(ARG_SELECTED_IDS)?.toList() ?: emptyList()

        binding.tvBulkEditHeader.text = "Bulk Edit — ${selectedIds.size} selected"

        setupCheckboxes()
        setupTagAutocomplete()
        setupTimestampPickers()
        setupApplyButton()
        observeBulkActionComplete()

        binding.ivClose.setOnClickListener { dismiss() }
    }

    private fun observeBulkActionComplete() {
        wipViewModel.bulkActionComplete.observe(viewLifecycleOwner) { complete ->
            if (complete == true) {
                wipViewModel.resetBulkActionComplete()
                onBulkActionApplied?.invoke(selectedIds)
                dismiss()
            }
        }
    }

    private fun setupCheckboxes() {
        allCheckboxes.addAll(
            listOf(
                binding.cbSetEncountered,
                binding.cbSetViewed,
                binding.cbRemoveAllTags,
                binding.cbAddTag,
                binding.cbRemoveTag,
                binding.cbSetCreatedAt,
                binding.cbSetModifiedAt,
                binding.cbSetFirstViewedAt,
                binding.cbSetFirstEncounteredAt,
                binding.cbSetLastViewedAt,
                binding.cbSetLastEncounteredAt,
                binding.cbSetLastParaCreatedAt
            )
        )

        allCheckboxes.forEach { cb -> cb.setOnClickListener { updateApplyButton() } }

        // Inputs stay always-enabled so the user can type/pick values before deciding to tick
        // the checkbox. The checkbox only gates whether the entered value is actually applied.
    }

    private fun setupTagAutocomplete() {
        // Populate Add/Remove Tag AutoCompleteTextView with existing tags (case-insensitive match,
        // shown only while typing to avoid a huge initial dropdown).
        wipViewModel.allTags?.observe(viewLifecycleOwner) { tags ->
            val safe = tags ?: emptyList()
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, safe)
            binding.etAddTag.setAdapter(adapter)
            binding.etRemoveTag.setAdapter(adapter)
            binding.etAddTag.threshold = 1
            binding.etRemoveTag.threshold = 1
        }

        listOf<AutoCompleteTextView>(binding.etAddTag, binding.etRemoveTag).forEach { ac ->
            ac.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus && ac.isEnabled && ac.adapter?.count ?: 0 > 0) ac.showDropDown()
            }
        }
    }

    private fun setupTimestampPickers() {
        val tsRows = listOf(
            binding.etSetCreatedAt,
            binding.etSetModifiedAt,
            binding.etSetFirstViewedAt,
            binding.etSetFirstEncounteredAt,
            binding.etSetLastViewedAt,
            binding.etSetLastEncounteredAt,
            binding.etSetLastParaCreatedAt
        )
        tsRows.forEach { et ->
            et.setOnClickListener { showDateTimePicker(et) }
        }
    }

    private fun showDateTimePicker(et: EditText) {
        val cal = Calendar.getInstance()
        val existing = pickedTimestamps[et.id]
        if (existing != null) cal.timeInMillis = existing

        DatePickerDialog(requireContext(), { _, year, month, day ->
            TimePickerDialog(requireContext(), { _, hour, minute ->
                cal.set(year, month, day, hour, minute, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val ts = cal.timeInMillis
                pickedTimestamps[et.id] = ts
                et.setText(dateFormat.format(Date(ts)))
                updateApplyButton()
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateApplyButton() {
        val checkedCount = allCheckboxes.count { it.isChecked }
        binding.btnApplyBulk.text = "Apply Changes ($checkedCount)"
        binding.btnApplyBulk.isEnabled = checkedCount > 0
    }

    private fun getSelectedActionNames(): List<String> {
        val actions = mutableListOf<String>()
        if (binding.cbSetEncountered.isChecked) {
            val v = binding.etSetEncountered.text.toString().trim()
            actions.add("Set Encountered Count → ${if (v.isEmpty()) "0" else v}")
        }
        if (binding.cbSetViewed.isChecked) {
            val v = binding.etSetViewed.text.toString().trim()
            actions.add("Set Viewed Count → ${if (v.isEmpty()) "0" else v}")
        }
        if (binding.cbRemoveAllTags.isChecked) actions.add("Remove All Tags")
        if (binding.cbAddTag.isChecked) {
            val tag = binding.etAddTag.text.toString().trim()
            if (tag.isNotEmpty()) actions.add("Add Tag: \"$tag\"")
        }
        if (binding.cbRemoveTag.isChecked) {
            val tag = binding.etRemoveTag.text.toString().trim()
            if (tag.isNotEmpty()) actions.add("Remove Tag: \"$tag\"")
        }
        addTimestampAction(actions, binding.cbSetCreatedAt, binding.etSetCreatedAt, "Created At")
        addTimestampAction(actions, binding.cbSetModifiedAt, binding.etSetModifiedAt, "Modified At")
        addTimestampAction(actions, binding.cbSetFirstViewedAt, binding.etSetFirstViewedAt, "First Viewed At")
        addTimestampAction(actions, binding.cbSetFirstEncounteredAt, binding.etSetFirstEncounteredAt, "First Encountered At")
        addTimestampAction(actions, binding.cbSetLastViewedAt, binding.etSetLastViewedAt, "Last Viewed At")
        addTimestampAction(actions, binding.cbSetLastEncounteredAt, binding.etSetLastEncounteredAt, "Last Encountered At")
        addTimestampAction(actions, binding.cbSetLastParaCreatedAt, binding.etSetLastParaCreatedAt, "Last Article Created At")
        return actions
    }

    private fun addTimestampAction(out: MutableList<String>, cb: CheckBox, et: EditText, label: String) {
        if (!cb.isChecked) return
        val display = et.text.toString().trim()
        out.add("Set $label → ${if (display.isEmpty()) "cleared" else display}")
    }

    private fun parseFloatOrZero(raw: String): Float {
        if (raw.isEmpty()) return 0f
        return raw.toFloatOrNull() ?: 0f
    }

    private fun setupApplyButton() {
        binding.btnApplyBulk.setOnClickListener {
            // Validate text inputs for checked rows
            if (binding.cbAddTag.isChecked && binding.etAddTag.text.toString().trim().isEmpty()) {
                binding.cbAddTag.isChecked = false
                updateApplyButton()
                Toast.makeText(requireContext(), "Add Tag unchecked — no tag entered", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (binding.cbRemoveTag.isChecked && binding.etRemoveTag.text.toString().trim().isEmpty()) {
                binding.cbRemoveTag.isChecked = false
                updateApplyButton()
                Toast.makeText(requireContext(), "Remove Tag unchecked — no tag entered", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val actionNames = getSelectedActionNames()
            if (actionNames.isEmpty()) return@setOnClickListener

            val message = "Apply ${actionNames.size} action(s) to ${selectedIds.size} WPIs?\n\n" +
                    actionNames.joinToString("\n") { "• $it" }

            val confirmDialog = AlertDialog.Builder(requireContext())
                .setTitle("Confirm Bulk Edit")
                .setMessage(message)
                .setPositiveButton("Apply") { _, _ -> executeBulkActions() }
                .setNegativeButton("Cancel", null)
                .show()
            confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(android.graphics.Color.parseColor("#E65100"))
        }
    }

    private fun executeBulkActions() {
        val setEncountered: Float? = if (binding.cbSetEncountered.isChecked)
            parseFloatOrZero(binding.etSetEncountered.text.toString().trim()) else null
        val setViewed: Float? = if (binding.cbSetViewed.isChecked)
            parseFloatOrZero(binding.etSetViewed.text.toString().trim()) else null

        val addTag = if (binding.cbAddTag.isChecked) binding.etAddTag.text.toString().trim() else null
        val removeTag = if (binding.cbRemoveTag.isChecked) binding.etRemoveTag.text.toString().trim() else null

        val setCreatedAt = timestampValue(binding.cbSetCreatedAt, binding.etSetCreatedAt)
        val setModifiedAt = timestampValue(binding.cbSetModifiedAt, binding.etSetModifiedAt)
        val setFirstViewedAt = timestampValue(binding.cbSetFirstViewedAt, binding.etSetFirstViewedAt)
        val setFirstEncounteredAt = timestampValue(binding.cbSetFirstEncounteredAt, binding.etSetFirstEncounteredAt)
        val setLastViewedAt = timestampValue(binding.cbSetLastViewedAt, binding.etSetLastViewedAt)
        val setLastEncounteredAt = timestampValue(binding.cbSetLastEncounteredAt, binding.etSetLastEncounteredAt)
        val setLastParaCreatedAt = timestampValue(binding.cbSetLastParaCreatedAt, binding.etSetLastParaCreatedAt)

        wipViewModel.executeBulkActions(
            ids = selectedIds,
            setEncountered = setEncountered,
            setViewed = setViewed,
            removeAllTags = binding.cbRemoveAllTags.isChecked,
            addTag = addTag,
            removeTag = removeTag,
            setCreatedAt = setCreatedAt,
            setModifiedAt = setModifiedAt,
            setFirstViewedAt = setFirstViewedAt,
            setFirstEncounteredAt = setFirstEncounteredAt,
            setLastViewedAt = setLastViewedAt,
            setLastEncounteredAt = setLastEncounteredAt,
            setLastParaCreatedAt = setLastParaCreatedAt
        )
    }

    /** null = field not modified; non-null = set to this value (0 when user left it blank → clear). */
    private fun timestampValue(cb: CheckBox, et: EditText): Long? {
        if (!cb.isChecked) return null
        return pickedTimestamps[et.id] ?: 0L
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onBulkActionDismissed?.invoke()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SELECTED_IDS = "selected_ids"

        fun newInstance(selectedIds: List<Int>): BulkActionBottomSheet {
            val fragment = BulkActionBottomSheet()
            val args = Bundle()
            args.putIntegerArrayList(ARG_SELECTED_IDS, ArrayList(selectedIds))
            fragment.arguments = args
            return fragment
        }
    }
}
