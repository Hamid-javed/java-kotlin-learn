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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
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
                    tvTags.text = it?.customTag?.joinToString(", ")
                    if (it.readCount != null) etRadCount.setText(it.readCount!!.toInt().toString())
                    if (it.displayCount != null) etViewedCount.setText(
                        it.displayCount!!.toInt().toString()
                    )

                    tagsList = ArrayList(it.customTag ?: emptyList())

                }
                if(sharedViewModel.notDeletedTags != null) {
                    mBinding.tvTags.text = sharedViewModel.notDeletedTags?.joinToString(", ")
                    sharedViewModel.notDeletedTags = null
                }
                if(mBinding.tvTags.text.isNotBlank()) {
                    mBinding.ivDeleteTags.visibility = View.VISIBLE
                } else {
                    mBinding.ivDeleteTags.visibility = View.GONE
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
                updateCount(mBinding.etRadCount, -1, "read") // only updates EditText
            }
            mBinding.btnViewedPlus.setOnClickListener {
                updateCount(mBinding.etViewedCount, +1, "view")
            }
            mBinding.btnViewedMinus.setOnClickListener {
                updateCount(mBinding.etViewedCount, -1, "view") // only updates EditText
            }



            btnSave.setOnClickListener {
                val wip = etWord.text.toString().trim()
                val meaning = etMeaning.text.toString().trim()
                val sampleSentence = etSampleSentence.text.toString().trim()
                val tags = tvTags.text
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }   // 🔥 CRITICAL

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

                if (!tag.isNullOrEmpty() && !tagsList.contains(tag)) {
                    tagsList.add(tag)
                    tvTags.text = tagsList.joinToString(", ")
                    ivDeleteTags.visibility = View.VISIBLE
                }

                etTag.setText("")
            }


            ivDeleteTags.setOnClickListener {
                if(id != null) {
                    val bundle = Bundle()
                    bundle.putInt("wip_id", id)
                    findNavController().navigate(R.id.deleteTagsFragment, bundle)
                }

            }

        }


    }



    private fun updateCount(editText: TextInputEditText, delta: Int, type: String) {
        val current = editText.text?.toString()?.toIntOrNull() ?: 0
        val newValue = (current + delta).coerceAtLeast(0)
        editText.setText(newValue.toString())

        if (delta > 0) {
            if (type == "view") {
                wipViewModel.incrementDisplayCount(id)
            } else if (type == "read") {
                wipViewModel.incrementReadCount(id)
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