package com.rameez.hel.fragments

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rameez.hel.R
import com.rameez.hel.SharedPref
import com.rameez.hel.data.model.SourceModel

class SourceSelectionBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var rvSources: RecyclerView
    private lateinit var etSourceName: EditText
    private lateinit var btnAddSource: Button
    private lateinit var btnConfirm: Button
    private lateinit var cbSelectAllSources: CheckBox
    
    private val sourcesList = mutableListOf<SourceModel>()
    private lateinit var adapter: SourceAdapter

    var onConfirmListener: ((List<String>) -> Unit)? = null
    var isSettingsMode: Boolean = false

    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialogTheme
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_source_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        rvSources = view.findViewById(R.id.rvSources)
        etSourceName = view.findViewById(R.id.etSourceName)
        btnAddSource = view.findViewById(R.id.btnAddSource)
        btnConfirm = view.findViewById(R.id.btnConfirm)
        cbSelectAllSources = view.findViewById(R.id.cbSelectAllSources)

        if (isSettingsMode) {
            btnConfirm.text = "Save & Close"
        }

        // Input Filter: No spaces, newlines, or full stops
        val filter = InputFilter { source, start, end, dest, dstart, dend ->
            for (i in start until end) {
                val char = source[i]
                //if (char == ' ' || char == '\n' || char == '.') {
				if (char == ' ' || char == '\n') {
                    return@InputFilter ""
                }
            }
            null
        }
        etSourceName.filters = arrayOf(filter)

        sourcesList.clear()
        sourcesList.addAll(SharedPref.getSources(requireContext()))
        
        adapter = SourceAdapter(sourcesList, 
            onStateChanged = { updatedList ->
                SharedPref.saveSources(requireContext(), updatedList)
                updateSelectAllState()
            },
            onDelete = { position ->
                sourcesList.removeAt(position)
                adapter.notifyItemRemoved(position)
                adapter.notifyItemRangeChanged(position, sourcesList.size)
                SharedPref.saveSources(requireContext(), sourcesList)
                updateSelectAllState()
            },
            onEdit = { position, newName ->
                sourcesList[position] = sourcesList[position].copy(name = newName)
                adapter.notifyItemChanged(position)
                SharedPref.saveSources(requireContext(), sourcesList)
            }
        )
        
        rvSources.layoutManager = LinearLayoutManager(requireContext())
        rvSources.adapter = adapter

        updateSelectAllState()

        cbSelectAllSources.setOnCheckedChangeListener { _, isChecked ->
            if (cbSelectAllSources.isPressed) {
                sourcesList.forEach { it.isChecked = isChecked }
                adapter.notifyDataSetChanged()
                SharedPref.saveSources(requireContext(), sourcesList)
            }
        }

        btnAddSource.setOnClickListener {
            val name = etSourceName.text.toString().trim()
            if (name.isNotEmpty()) {
                if (sourcesList.any { it.name.equals(name, ignoreCase = true) }) {
                    Toast.makeText(requireContext(), "Source already exists", Toast.LENGTH_SHORT).show()
                } else {
                    val newSource = SourceModel(name, true)
                    sourcesList.add(newSource)
                    adapter.notifyItemInserted(sourcesList.size - 1)
                    SharedPref.saveSources(requireContext(), sourcesList)
                    etSourceName.text.clear()
                    updateSelectAllState()
                }
            } else {
                Toast.makeText(requireContext(), "Please enter a source name", Toast.LENGTH_SHORT).show()
            }
        }

        btnConfirm.setOnClickListener {
            val selectedSources = sourcesList.filter { it.isChecked }.map { it.name }
            if (!isSettingsMode) {
                onConfirmListener?.invoke(selectedSources)
            }
            dismiss()
        }
    }

    private fun updateSelectAllState() {
        cbSelectAllSources.isChecked = sourcesList.isNotEmpty() && sourcesList.all { it.isChecked }
    }

    private class SourceAdapter(
        private val list: List<SourceModel>,
        private val onStateChanged: (List<SourceModel>) -> Unit,
        private val onDelete: (Int) -> Unit,
        private val onEdit: (Int, String) -> Unit
    ) : RecyclerView.Adapter<SourceAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val cbSource: CheckBox = view.findViewById(R.id.cbSource)
            val ivEdit: ImageView = view.findViewById(R.id.ivEditSource)
            val ivDelete: ImageView = view.findViewById(R.id.ivDeleteSource)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_source_selection, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.cbSource.text = item.name
            holder.cbSource.setOnCheckedChangeListener(null)
            holder.cbSource.isChecked = item.isChecked
            holder.cbSource.setOnCheckedChangeListener { _, isChecked ->
                item.isChecked = isChecked
                onStateChanged(list)
            }

            holder.ivDelete.setOnClickListener {
                onDelete(position)
            }

            holder.ivEdit.setOnClickListener {
                val context = holder.itemView.context
                val editText = EditText(context)
                editText.setText(item.name)
                
                // Reuse same filter for editing (allow dots for URLs)
                val filter = InputFilter { source, start, end, dest, dstart, dend ->
                    for (i in start until end) {
                        val char = source[i]
                        if (char == ' ' || char == '\n') {
                            return@InputFilter ""
                        }
                    }
                    null
                }
                editText.filters = arrayOf(filter)

                androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Edit Source")
                    .setView(editText)
                    .setPositiveButton("Save") { _, _ ->
                        val newName = editText.text.toString().trim()
                        if (newName.isNotEmpty()) {
                            onEdit(position, newName)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        override fun getItemCount(): Int = list.size
    }

    companion object {
        fun newInstance(isSettings: Boolean = false): SourceSelectionBottomSheetFragment {
            val fragment = SourceSelectionBottomSheetFragment()
            fragment.isSettingsMode = isSettings
            return fragment
        }
    }
}
