package com.rameez.hel.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.rameez.hel.R
import com.rameez.hel.data.model.WIPModel
import com.rameez.hel.databinding.FragmentWIPEditBinding
import com.rameez.hel.viewmodel.SharedViewModel
import com.rameez.hel.viewmodel.WIPViewModel

class WIPEditFragment : Fragment() {

    private lateinit var mBinding: FragmentWIPEditBinding
    private val wipViewModel: WIPViewModel by activityViewModels()
    private var tagsList = arrayListOf<String>()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var readOperator: String? = null
    private var filteredReadCount: Float? = null
    private var allWipWords = listOf<String>()
    private var wipId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = FragmentWIPEditBinding.inflate(layoutInflater, container, false)
        return mBinding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val intent = arguments
        val id = intent?.getInt("wip_id")
        wipId = id

        readOperator = sharedViewModel.readOperator
        readOperatorSetup()

        if (id != null) {
            wipViewModel.getWIPById(id)?.observe(viewLifecycleOwner) {
                mBinding.apply {
                    tvHeading.text = "Edit WIP"
                    etWord.setText(it.wip)
                    etMeaning.setText(it.meaning)
                    etSampleSentence.setText(it.sampleSentence)
                    etCategory.setText((it.category))
                    if (it.readCount != null) etRadCount.setText(it.readCount!!.toInt().toString())
                    if (it.displayCount != null) etViewedCount.setText(
                        it.displayCount!!.toInt().toString()
                    )

                    val initialTags = it.customTag ?: emptyList()
                    mBinding.llTagsContainer.removeAllViews()
                    initialTags.filter { t -> t.isNotBlank() }.forEach { t ->
                        addTagRow(t)
                    }
                }
            }
        } else {
            mBinding.tvHeading.text = "Add WIP"
        }

        wipViewModel.allTags?.observe(viewLifecycleOwner) { tags ->
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                tags
            )
            mBinding.etTag.setAdapter(adapter)
        }

        // #18: Duplicate detection while adding WPI
        wipViewModel.getWIPs()?.observe(viewLifecycleOwner) { allWips ->
            allWipWords = allWips.mapNotNull { it.wip?.trim()?.lowercase() }

            // Set up autocomplete dropdown with existing words
            val wordAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                allWips.mapNotNull { it.wip }
            )
            mBinding.etWord.setAdapter(wordAdapter)
        }

        mBinding.etWord.addTextChangedListener { editable ->
            val typed = editable?.toString()?.trim()?.lowercase() ?: ""
            if (typed.length >= 2 && id == null) { // Only show warning when adding new
                val matches = allWipWords.filter { it.contains(typed) }
                if (matches.isNotEmpty()) {
                    mBinding.tvDuplicateWarning.text = "Possible duplicate: ${matches.size} existing WPI(s) match"
                    mBinding.tvDuplicateWarning.visibility = View.VISIBLE
                } else {
                    mBinding.tvDuplicateWarning.visibility = View.GONE
                }
            } else {
                mBinding.tvDuplicateWarning.visibility = View.GONE
            }
        }

        mBinding.apply {
            imgBack.setOnClickListener {
                findNavController().navigateUp()
            }

            mBinding.etTag.setOnItemClickListener { _, _, _, _ ->
                mBinding.btnAddTag.performClick()
            }

            mBinding.btnEncounterPlus.setOnClickListener {
                updateCount(mBinding.etRadCount, +1, "read")
            }
            mBinding.btnEncounterMinus.setOnClickListener {
                updateCount(mBinding.etRadCount, -1, "read")
            }
            mBinding.btnViewedPlus.setOnClickListener {
                updateCount(mBinding.etViewedCount, +1, "view")
            }
            mBinding.btnViewedMinus.setOnClickListener {
                updateCount(mBinding.etViewedCount, -1, "view")
            }
            mBinding.btnResetViewed.setOnClickListener {
                mBinding.etViewedCount.setText("0")
                wipId?.let { wipViewModel.updateViewedCount(it, 0f) }
            }
            mBinding.btnResetEncountered.setOnClickListener {
                mBinding.etRadCount.setText("0")
                wipId?.let { wipViewModel.updateReadCount(it, 0f) }
            }



            btnSave.setOnClickListener {
                val wip = etWord.text.toString().trim()
                val meaning = etMeaning.text.toString().trim()
                val sampleSentence = etSampleSentence.text.toString().trim()
                
                // Read tags directly from the container
                val tags = mutableListOf<String>()
                for (i in 0 until mBinding.llTagsContainer.childCount) {
                    val row = mBinding.llTagsContainer.getChildAt(i)
                    val et = row.findViewById<TextInputEditText>(R.id.etInlineTag)
                    val tagText = et?.text?.toString()?.trim()
                    if (!tagText.isNullOrEmpty()) {
                        tags.add(tagText)
                    }
                }

                val readCount = etRadCount.text.toString().toFloatOrNull() ?: 0f
                val viewCount = etViewedCount.text.toString().toFloatOrNull() ?: 0f
                val category = etCategory.text.toString()

                if (id != null) {
                    // fetch latest state from repository (synchronously) inside a coroutine
                    lifecycleScope.launch {
                        val prev = wipViewModel.getWIPByIdSync(id)

                        // Only apply manual readCount changes
                        if (prev != null && readCount != (prev.readCount ?: 0f)) {
                            wipViewModel.updateReadCount(id, readCount)
                        }

                        // Only apply manual viewedCount changes
                        if (prev != null && viewCount != (prev.displayCount ?: 0f)) {
                            wipViewModel.updateViewedCount(id, viewCount)
                        }

                        // Update WIP content
                        wipViewModel.updateWIP(
                            id,
                            category,
                            wip,
                            meaning,
                            sampleSentence,
                            tags,
                            readCount,
                            viewCount
                        )

                        findNavController().navigateUp()
                    }

                }
                else {

                    var sr: Float
                    wipViewModel.getWIPs()?.observe(viewLifecycleOwner) {
                        sr = it.size.toFloat() + 1.0f

                        val wipItem = WIPModel(
                            sr = sr,
                            category = category,
                            wip = wip,
                            meaning = meaning,
                            sampleSentence = sampleSentence,
                            customTag = tags,
                            readCount = readCount,
                            displayCount = viewCount
                        )
                        if(wip.isNotBlank() && category.isNotBlank()) {
                            wipViewModel.insertWIP(wipItem)
                            sharedViewModel.isWipAdded = true
                            Toast.makeText(requireContext(), "New WIP added", Toast.LENGTH_SHORT).show()
                            Log.i("WIP",wip + category)
                            findNavController().navigateUp()
                        } else {
                            Toast.makeText(requireContext(), "WIP and category can't be empty", Toast.LENGTH_SHORT).show()
                        }

                    }

                }
            }


            btnAddTag.setOnClickListener {
                val tag = etTag.text?.toString()?.trim()

                if (!tag.isNullOrEmpty()) {
                    addTagRow(tag)
                }

                etTag.setText("")
            }

        }


    }



    private fun addTagRow(tag: String) {
        val inflater = LayoutInflater.from(requireContext())
        val rowView = inflater.inflate(R.layout.item_tag_edit_row, mBinding.llTagsContainer, false)
        
        val etInlineTag = rowView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etInlineTag)
        val btnDeleteTag = rowView.findViewById<android.widget.ImageButton>(R.id.btnDeleteTag)
        
        etInlineTag.setText(tag)
        
        btnDeleteTag.setOnClickListener {
            mBinding.llTagsContainer.removeView(rowView)
        }
        
        mBinding.llTagsContainer.addView(rowView)
    }

    private fun updateCount(editText: TextInputEditText, delta: Int, type: String) {
        val current = editText.text?.toString()?.toIntOrNull() ?: 0
        val newValue = (current + delta).coerceAtLeast(0)
        editText.setText(newValue.toString())

        val currentWipId = wipId ?: return
        if (delta > 0) {
            if (type == "view") {
                wipViewModel.incrementDisplayCount(currentWipId)
            } else if (type == "read") {
                wipViewModel.incrementReadCount(currentWipId)
            }
        }
    }

    private fun readOperatorSetup() {

        val readCountsStr = mutableListOf("Word", "Idiom", "Phrase")
        readCountsStr.add(0, "")
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, readCountsStr)
        mBinding.readSpinner.setSelection(0)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mBinding.readSpinner.adapter = spinnerAdapter
        mBinding.readSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                if (position != 0) {
                    val selectedItem = parent.getItemAtPosition(position)
                    if (selectedItem.toString() != "") {
                        readOperator = selectedItem.toString()
                        mBinding.etCategory.setText(selectedItem.toString())
//                        Log.d("TAG", "read count $filteredReadCount")
                    }
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
    }

}