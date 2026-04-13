package com.rameez.hel.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
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

class BulkActionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentBulkActionBinding? = null
    private val binding get() = _binding!!

    private lateinit var wipViewModel: WIPViewModel
    private var selectedIds: List<Int> = emptyList()

    var onBulkActionApplied: (() -> Unit)? = null

    private val allCheckboxes = mutableListOf<CheckBox>()

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
        setupApplyButton()
        observeBulkActionComplete()

        binding.ivClose.setOnClickListener { dismiss() }
    }

    private fun observeBulkActionComplete() {
        wipViewModel.bulkActionComplete.observe(viewLifecycleOwner) { complete ->
            if (complete) {
                wipViewModel.resetBulkActionComplete()
                Toast.makeText(
                    requireContext(),
                    "Bulk changes applied to ${selectedIds.size} WPIs",
                    Toast.LENGTH_SHORT
                ).show()
                onBulkActionApplied?.invoke()
                dismiss()
            }
        }
    }

    private fun setupCheckboxes() {
        allCheckboxes.addAll(
            listOf(
                binding.cbResetEncountered,
                binding.cbResetViewed,
                binding.cbRemoveAllTags,
                binding.cbAddTag,
                binding.cbResetCreatedAt,
                binding.cbResetModifiedAt,
                binding.cbResetFirstViewedAt,
                binding.cbResetFirstEncounteredAt,
                binding.cbResetLastViewedAt,
                binding.cbResetLastEncounteredAt,
                binding.cbResetLastParaCreatedAt
            )
        )

        val listener = View.OnClickListener { updateApplyButton() }
        allCheckboxes.forEach { it.setOnClickListener(listener) }

        // Enable/disable tag EditText based on checkbox
        binding.cbAddTag.setOnClickListener {
            binding.etAddTag.isEnabled = binding.cbAddTag.isChecked
            if (!binding.cbAddTag.isChecked) {
                binding.etAddTag.text.clear()
            }
            updateApplyButton()
        }
    }

    private fun updateApplyButton() {
        val checkedCount = getCheckedCount()
        binding.btnApplyBulk.text = "Apply Changes ($checkedCount)"
        binding.btnApplyBulk.isEnabled = checkedCount > 0
    }

    private fun getCheckedCount(): Int {
        return allCheckboxes.count { it.isChecked }
    }

    private fun getSelectedActionNames(): List<String> {
        val actions = mutableListOf<String>()
        if (binding.cbResetEncountered.isChecked) actions.add("Reset Encountered Count")
        if (binding.cbResetViewed.isChecked) actions.add("Reset Viewed Count")
        if (binding.cbRemoveAllTags.isChecked) actions.add("Remove All Tags")
        if (binding.cbAddTag.isChecked) {
            val tag = binding.etAddTag.text.toString().trim()
            if (tag.isNotEmpty()) actions.add("Add Tag: \"$tag\"")
        }
        if (binding.cbResetCreatedAt.isChecked) actions.add("Reset Created At")
        if (binding.cbResetModifiedAt.isChecked) actions.add("Reset Modified At")
        if (binding.cbResetFirstViewedAt.isChecked) actions.add("Reset First Viewed At")
        if (binding.cbResetFirstEncounteredAt.isChecked) actions.add("Reset First Encountered At")
        if (binding.cbResetLastViewedAt.isChecked) actions.add("Reset Last Viewed At")
        if (binding.cbResetLastEncounteredAt.isChecked) actions.add("Reset Last Encountered At")
        if (binding.cbResetLastParaCreatedAt.isChecked) actions.add("Reset Last Article Created At")
        return actions
    }

    private fun setupApplyButton() {
        binding.btnApplyBulk.setOnClickListener {
            // Validate Add Tag
            if (binding.cbAddTag.isChecked && binding.etAddTag.text.toString().trim().isEmpty()) {
                binding.cbAddTag.isChecked = false
                binding.etAddTag.isEnabled = false
                updateApplyButton()
                Toast.makeText(requireContext(), "Add Tag unchecked — no tag entered", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val actionNames = getSelectedActionNames()
            if (actionNames.isEmpty()) return@setOnClickListener

            val message = "Apply ${actionNames.size} action(s) to ${selectedIds.size} WPIs?\n\n" +
                    actionNames.joinToString("\n") { "• $it" }

            AlertDialog.Builder(requireContext())
                .setTitle("Confirm Bulk Edit")
                .setMessage(message)
                .setPositiveButton("Apply") { _, _ -> executeBulkActions() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun executeBulkActions() {
        val addTag = if (binding.cbAddTag.isChecked) binding.etAddTag.text.toString().trim() else null

        wipViewModel.executeBulkActions(
            ids = selectedIds,
            resetEncountered = binding.cbResetEncountered.isChecked,
            resetViewed = binding.cbResetViewed.isChecked,
            removeAllTags = binding.cbRemoveAllTags.isChecked,
            addTag = addTag,
            resetCreatedAt = binding.cbResetCreatedAt.isChecked,
            resetModifiedAt = binding.cbResetModifiedAt.isChecked,
            resetFirstViewedAt = binding.cbResetFirstViewedAt.isChecked,
            resetFirstEncounteredAt = binding.cbResetFirstEncounteredAt.isChecked,
            resetLastViewedAt = binding.cbResetLastViewedAt.isChecked,
            resetLastEncounteredAt = binding.cbResetLastEncounteredAt.isChecked,
            resetLastParaCreatedAt = binding.cbResetLastParaCreatedAt.isChecked
        )
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
