package com.rameez.hel.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.rameez.hel.R
import com.rameez.hel.adapter.CategoryAdapter
import com.rameez.hel.adapter.CustomTagsAdapter
import com.rameez.hel.data.model.WIPModel
import com.rameez.hel.databinding.FragmentWIPFilterBinding
import com.rameez.hel.viewmodel.SharedViewModel
import com.rameez.hel.viewmodel.WIPViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class WIPFilterFragment : Fragment() {

    private lateinit var mBinding: FragmentWIPFilterBinding
    private lateinit var customTagAdapter: CustomTagsAdapter
    private val wipViewModel: WIPViewModel by activityViewModels()
    private val customTags = mutableListOf<String>()
    private lateinit var filteredCategoryList: MutableList<String>
    private lateinit var filteredTagsList: MutableList<String>
    private var filteredReadCount: Float? = null
    private var filteredViewedCount: Float? = null
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var categoryAdapter : CategoryAdapter
    private val categoriesList = arrayListOf<String>()
    private var readOperator: String? = null
    private var viewedOperator: String? = null
    private var isFirstTime = false
    private var filteredWIP: String? = null
    private var filteredMeaning: String? = null
    private var filteredSampleSen: String? = null

    private var createdAtMillis: Long? = null
    private var modifiedAtMillis: Long? = null


    // timestamp filters
    private var filteredCreatedAt: Long? = null
    private var createdOperator: String? = null

    private var filteredModifiedAt: Long? = null
    private var modifiedOperator: String? = null

    private var filteredReadCountTo: Float? = null
    private var filteredViewedCountTo: Float? = null

    private var createdAtMillisTo: Long? = null
    private var modifiedAtMillisTo: Long? = null
    // ===== LAST VIEWED AT =====
    private var filteredLastViewedAt: Long? = null
    private var lastViewedAtMillisTo: Long? = null
    private var lastViewedOperator: String? = null

    // ===== FIRST ENCOUNTERED (FIRST MODIFIED) =====
    private var filteredFirstEncounteredAt: Long? = null
    private var firstEncounteredAtMillisTo: Long? = null
    private var firstEncounteredOperator: String? = null

    private var filteredLastEncounteredAt: Long? = null
    private var lastEncounteredAtMillisTo: Long? = null
    private var lastEncounteredOperator: String? = null

    private var filteredFirstViewedAt: Long? = null
    private var firstViewedAtMillisTo: Long? = null
    private var firstViewedOperator: String? = null

    // PARA FILTER ---
    private var filteredArticleCreatedAt: Long? = null
    private var articleCreatedAtMillisTo: Long? = null
    private var articleCreatedOperator: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        customTagAdapter = CustomTagsAdapter(sharedViewModel.selectedTags)
        categoryAdapter = CategoryAdapter(sharedViewModel.selectedCategories)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (::mBinding.isInitialized.not()) {
            isFirstTime = true
            mBinding = FragmentWIPFilterBinding.inflate(layoutInflater, container, false)
        }
        mBinding.apply {
        }

        return mBinding.root
    }

    private fun formatMillisToDate(millis: Long?): String {
        if (millis == null) return ""
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) // Match your picker format
        return sdf.format(Date(millis))
    }


    private fun clearAllFilters() {
        mBinding.apply {
            etWIP.text?.clear()
            etMeaning.text?.clear()
            etSampleSen.text?.clear()
            etReadCount.text?.clear()
            etReadCountTo.text?.clear()
            etViewedCount.text?.clear()
            etViewedCountTo.text?.clear()
            etLimit.text?.clear()
            etCreatedAt.text?.clear()
            etCreatedAtTo.text?.clear()
            etModifiedAt.text?.clear()
            sharedViewModel.selectedTags.clear()
            sharedViewModel.selectedCategories.clear()
            filteredTagsList.clear()
            filteredCategoryList.clear()
            etModifiedAtTo.text?.clear()
            etLastViewedAt.text?.clear()
            etLastViewedAtTo.text?.clear()
            etLastEncounteredAt.text?.clear()
            etLastEncounteredAtTo.text?.clear()
            etFirstViewedAt.text?.clear()
            etFirstViewedAtTo.text?.clear()
            etFirstEncounteredAt.text?.clear()
            etFirstEncounteredAtTo.text?.clear()
            etArticleCreatedAt.text?.clear()
            etArticleCreatedAtTo.text?.clear()
            customTagAdapter.notifyDataSetChanged()
            categoryAdapter.notifyDataSetChanged()
        }

        // --- Reset spinner selections ---
        mBinding.apply {
            readSpinner.setSelection(0)
            viewedSpinner.setSelection(0)
            spinnerCreatedOp.setSelection(0)
            spinnerModifiedOp.setSelection(0)
            spinnerLastEncounteredOp.setSelection(0)
            spinnerLastViewedOp.setSelection(0)
            spinnerFirstViewedOp.setSelection(0)
            spinnerFirstEncounteredOp.setSelection(0)
            spinnerArticleCreatedOp.setSelection(0)
        }

        // --- Reset checkboxes ---
        mBinding.apply {
            cbWord.isChecked = false
            cbMeaning.isChecked = false
            cbSentence.isChecked = false
        }

        // --- Reset switches ---
        mBinding.apply {
            switchAutoScroll.isChecked = false
            switchMaterial.isChecked = false
            etScrollInterval.isEnabled = false
            cbUpdateViewCount.isChecked = true
            cbUpdateTimestamps.isChecked = true
            cbWhiteNoise.isChecked = false
            spinnerSortBy.setSelection(0)
            btnSortOrder.text = "DESC \u2193"
        }


        // READ
        filteredReadCount = null
        filteredReadCountTo = null
        readOperator = null
        sharedViewModel.readCount = null
        sharedViewModel.readOperator = null

// VIEWED
        filteredViewedCount = null
        filteredViewedCountTo = null
        viewedOperator = null
        sharedViewModel.viewedCount = null
        sharedViewModel.viewedOperator = null


        filteredReadCount = null
        filteredReadCountTo = null
        filteredViewedCount = null
        filteredViewedCountTo = null
        filteredWIP = null
        filteredMeaning = null
        filteredSampleSen = null
        filteredCreatedAt = null
        createdAtMillis = null
        createdAtMillisTo = null
        filteredModifiedAt = null
        modifiedAtMillis = null
        modifiedAtMillisTo = null
        readOperator = null
        viewedOperator = null
        createdOperator = null
        modifiedOperator = null
        filteredLastViewedAt = null
        lastViewedAtMillisTo = null
        lastViewedOperator = null
        filteredLastEncounteredAt = null
        lastEncounteredAtMillisTo = null
        lastEncounteredOperator = null

        filteredFirstViewedAt = null
        firstViewedAtMillisTo = null
        firstViewedOperator = null

        filteredFirstEncounteredAt = null
        firstEncounteredAtMillisTo = null
        firstEncounteredOperator = null

        filteredArticleCreatedAt = null
        articleCreatedAtMillisTo = null
        articleCreatedOperator = null


        sharedViewModel.apply {
            filteredWord = null
            filteredMeaning = null
            filteredSampleSen = null
            readCount = null
            viewedCount = null
            readOperator = null
            viewedOperator = null
            createdOperator = null
            modifiedOperator = null
            lastEncounteredAt = null
            lastEncounteredAtTo = null
            lastEncounteredOperator = null
            createdAt = null
            createdAtTo = null
            modifiedAt = null
            modifiedAtTo = null

            lastEncounteredAt = null
            lastEncounteredAtTo = null
            lastEncounteredOperator = null
            firstViewedAt = null
            firstViewedAtTo = null
            firstViewedOperator = null

            lastViewedAt = null
            lastViewedAtTo = null
            lastViewedOperator = null

            firstEncounteredAt = null
            firstEncounteredAtTo = null
            firstEncounteredOperator = null

            articleCreatedAt = null
            articleCreatedAtTo = null
            articleCreatedOperator = null

            isAutoScrollEnabled = false
            autoScrollIntervalSecs = 5
            isAutoScrollPaused = false
            isReadAloud = false
            ttsOptions.clear()
            updateViewCountDuringFlashcard = true
            updateTimestampsDuringFlashcard = true
            isWhiteNoiseEnabled = false
            sortBy = null
            sortAscending = false
            isFilterApplied = false
            isShowingFilteredResults = false
            filteredWipsList.clear()
        }


        sharedViewModel.selectedHours = 0
        sharedViewModel.selectedMins = 0
        sharedViewModel.selectedSecs = 0
        updateTimerText()


        customTagAdapter.submitList(customTags) // refresh list if checkboxes were checked
        categoryAdapter.submitList(categoriesList)

        Toast.makeText(requireContext(), "All filters are cleared", Toast.LENGTH_SHORT).show()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        filteredCategoryList = sharedViewModel.categoryList
        filteredTagsList = sharedViewModel.tagsList
        filteredReadCount = sharedViewModel.readCount
        filteredViewedCount = sharedViewModel.viewedCount
        readOperator = sharedViewModel.readOperator
        viewedOperator = sharedViewModel.viewedOperator
        filteredWIP = sharedViewModel.filteredWord
        filteredMeaning = sharedViewModel.filteredMeaning
        filteredSampleSen = sharedViewModel.filteredSampleSen
        filteredLastViewedAt = sharedViewModel.lastViewedAt
        lastViewedAtMillisTo = sharedViewModel.lastViewedAtTo
        lastViewedOperator = sharedViewModel.lastViewedOperator

        filteredFirstEncounteredAt = sharedViewModel.firstEncounteredAt
        firstEncounteredAtMillisTo = sharedViewModel.firstEncounteredAtTo
        filteredLastEncounteredAt = sharedViewModel.lastEncounteredAt
        lastEncounteredAtMillisTo = sharedViewModel.lastEncounteredAtTo
        lastEncounteredOperator = sharedViewModel.lastEncounteredOperator

        filteredFirstViewedAt = sharedViewModel.firstViewedAt
        firstViewedAtMillisTo = sharedViewModel.firstViewedAtTo
        firstViewedOperator = sharedViewModel.firstViewedOperator

        filteredCreatedAt = sharedViewModel.createdAt
        createdAtMillis = sharedViewModel.createdAt
        createdAtMillisTo = sharedViewModel.createdAtTo

        filteredModifiedAt = sharedViewModel.modifiedAt
        modifiedAtMillis = sharedViewModel.modifiedAt
        modifiedAtMillisTo = sharedViewModel.modifiedAtTo

        filteredArticleCreatedAt = sharedViewModel.articleCreatedAt
        articleCreatedAtMillisTo = sharedViewModel.articleCreatedAtTo
        articleCreatedOperator = sharedViewModel.articleCreatedOperator

        mBinding.etCreatedAt.setText(formatMillisToDate(filteredCreatedAt))
        mBinding.etCreatedAtTo.setText(formatMillisToDate(createdAtMillisTo))
        mBinding.etModifiedAt.setText(formatMillisToDate(filteredModifiedAt))
        mBinding.etModifiedAtTo.setText(formatMillisToDate(modifiedAtMillisTo))
        mBinding.etArticleCreatedAt.setText(formatMillisToDate(filteredArticleCreatedAt))
        mBinding.etArticleCreatedAtTo.setText(formatMillisToDate(articleCreatedAtMillisTo))

        updateTimerText()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 1. Clear the filters
                clearAllFilters()

                this.isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        })

        mBinding.switchAutoScroll.setOnCheckedChangeListener { _, isChecked ->
            mBinding.etScrollInterval.isEnabled = isChecked
            if (!isChecked) {
                mBinding.etScrollInterval.text = null
            }
        }

        // #13: Flashcard options checkboxes
        mBinding.cbUpdateViewCount.isChecked = sharedViewModel.updateViewCountDuringFlashcard
        mBinding.cbUpdateTimestamps.isChecked = sharedViewModel.updateTimestampsDuringFlashcard
        mBinding.cbWhiteNoise.isChecked = sharedViewModel.isWhiteNoiseEnabled
        mBinding.cbUpdateViewCount.setOnCheckedChangeListener { _, isChecked ->
            sharedViewModel.updateViewCountDuringFlashcard = isChecked
        }
        mBinding.cbUpdateTimestamps.setOnCheckedChangeListener { _, isChecked ->
            sharedViewModel.updateTimestampsDuringFlashcard = isChecked
        }
        mBinding.cbWhiteNoise.setOnCheckedChangeListener { _, isChecked ->
            sharedViewModel.isWhiteNoiseEnabled = isChecked
        }

        // #14: Sort spinner setup
        sortSpinnerSetup()

        Log.d("TAG", "onCreate: ")

        setUpRecyclerView()
        readOperatorSetup()
        viewedOperatorSetup()
        createdOperatorSetup()
        modifiedOperatorSetup()
        lastViewedOperatorSetup()
        firstEncounteredOperatorSetup()
        lastEncounteredOperatorSetup()
        firstViewedOperatorSetup()
        articleCreatedOperatorSetup()



        restoreSpinnerSelection(mBinding.readSpinner, sharedViewModel.readOperator)
        restoreSpinnerSelection(mBinding.viewedSpinner, sharedViewModel.viewedOperator)
        restoreSpinnerSelection(mBinding.spinnerCreatedOp, sharedViewModel.createdOperator)
        restoreSpinnerSelection(mBinding.spinnerModifiedOp, sharedViewModel.modifiedOperator)
        restoreSpinnerSelection(mBinding.spinnerLastViewedOp, sharedViewModel.lastViewedOperator)
        restoreSpinnerSelection(mBinding.spinnerFirstEncounteredOp, sharedViewModel.firstEncounteredOperator)
        restoreSpinnerSelection( mBinding.spinnerLastEncounteredOp,sharedViewModel.lastEncounteredOperator)
        restoreSpinnerSelection(mBinding.spinnerFirstViewedOp,sharedViewModel.firstViewedOperator)
        restoreSpinnerSelection(mBinding.spinnerArticleCreatedOp, sharedViewModel.articleCreatedOperator)

        Log.d("TAG", "onViewCreated")

        wipViewModel.getWIPs()?.observe(viewLifecycleOwner) { wips ->
            categoriesList.clear()
            customTags.clear()

            wips.forEach { wipItem ->
                val lowerCaseCategory = wipItem.category?.lowercase(Locale.ROOT)
                if (wipItem.category !in categoriesList && lowerCaseCategory !in categoriesList.map { it.lowercase(Locale.ROOT) }) {
                    wipItem.category?.let { categoriesList.add(it) }
                }

                wipItem.customTag?.filterNot { it.isEmpty() }?.forEach { customTag ->
                    val lowerCaseCustomTag = customTag.lowercase(Locale.ROOT)
                    if (customTag !in customTags && lowerCaseCustomTag !in customTags.map { it.lowercase() }) {
                        customTags.add(customTag)
                    }
                }
            }

            categoryAdapter.submitList(categoriesList.toList())
            customTagAdapter.submitList(customTags.toList())
        }




        mBinding.apply {

            etReadCount.addTextChangedListener {
                if (it.isNullOrBlank()) {
                    filteredReadCount = null
                    filteredReadCountTo = null
                    readOperator = null
                    readSpinner.setSelection(0)
                    etReadCountTo.text = null
                }
            }

            etViewedCount.addTextChangedListener {
                if (it.isNullOrBlank()) {
                    filteredViewedCount = null
                    filteredViewedCountTo = null
                    viewedOperator = null
                    viewedSpinner.setSelection(0)
                    etViewedCountTo.text = null
                }
            }



            etFirstViewedAt.setOnClickListener {
                showDateTimePicker { text, millis ->
                    etFirstViewedAt.setText(text)
                    filteredFirstViewedAt = millis
                }
            }

            etFirstViewedAtTo.setOnClickListener {
                showDateTimePicker { text, millis ->
                    etFirstViewedAtTo.setText(text)
                    firstViewedAtMillisTo = millis
                }
            }


            etLastEncounteredAt.setOnClickListener {
                showDateTimePicker { text, millis ->
                    etLastEncounteredAt.setText(text)
                    filteredLastEncounteredAt = millis
                }
            }

            etLastEncounteredAtTo.setOnClickListener {
                showDateTimePicker { text, millis ->
                    etLastEncounteredAtTo.setText(text)
                    lastEncounteredAtMillisTo = millis
                }
            }


            etLastViewedAt.setOnClickListener {
                showDateTimePicker { text, millis ->
                    etLastViewedAt.setText(text)
                    filteredLastViewedAt = millis
                }
            }

            etLastViewedAtTo.setOnClickListener {
                showDateTimePicker { text, millis ->
                    etLastViewedAtTo.setText(text)
                    lastViewedAtMillisTo = millis
                }
            }

            etLastViewedAt.setOnLongClickListener {
                etLastViewedAt.text = null
                filteredLastViewedAt = null
                spinnerLastViewedOp.setSelection(0)
                true
            }

            etLastViewedAtTo.setOnLongClickListener {
                etLastViewedAtTo.text = null
                lastViewedAtMillisTo = null
                true
            }
            etLastEncounteredAt.setOnLongClickListener {
                etLastEncounteredAt.text = null
                filteredLastEncounteredAt = null
                spinnerLastEncounteredOp.setSelection(0)
                true
            }

            etFirstViewedAt.setOnLongClickListener {
                etFirstViewedAt.text = null
                filteredFirstViewedAt = null
                spinnerFirstViewedOp.setSelection(0)
                true
            }

            etFirstViewedAtTo.setOnLongClickListener {
                etFirstViewedAtTo.text = null
                firstViewedAtMillisTo = null
                true
            }


            etFirstEncounteredAt.setOnClickListener {
                showDateTimePicker { text, millis ->
                    etFirstEncounteredAt.setText(text)
                    filteredFirstEncounteredAt = millis
                }
            }

            etFirstEncounteredAtTo.setOnClickListener {
                showDateTimePicker { text, millis ->
                    etFirstEncounteredAtTo.setText(text)
                    firstEncounteredAtMillisTo = millis
                }
            }

            etFirstEncounteredAt.setOnLongClickListener {
                etFirstEncounteredAt.text = null
                filteredFirstEncounteredAt = null
                spinnerFirstEncounteredOp.setSelection(0)
                true
            }

            etFirstEncounteredAtTo.setOnLongClickListener {
                etFirstEncounteredAtTo.text = null
                firstEncounteredAtMillisTo = null
                true
            }



            etCreatedAtTo.setOnClickListener {
                showDateTimePicker { text, millis ->
                    etCreatedAtTo.setText(text)
                    createdAtMillisTo = millis
                }
            }

            etModifiedAtTo.setOnClickListener {
                showDateTimePicker { text, millis ->
                    etModifiedAtTo.setText(text)
                    modifiedAtMillisTo = millis
                }
            }


            etCreatedAt.setOnClickListener {
                showDateTimePicker { text, millis ->
                    etCreatedAt.setText(text)
                    createdAtMillis = millis
                }
            }


            etModifiedAt.setOnClickListener {
                showDateTimePicker { text, millis ->
                    etModifiedAt.setText(text)
                    modifiedAtMillis = millis
                }
            }

            // allow long-press to clear the createdAt field
            etCreatedAt.setOnLongClickListener {
                etCreatedAt.setText("")
                createdAtMillis = null
                sharedViewModel.createdAt = null
                mBinding.spinnerCreatedOp.setSelection(0)
                true
            }

            mBinding.etTimer.setOnLongClickListener {
                mBinding.etTimer.setText("")
                sharedViewModel.selectedHours = 0
                sharedViewModel.selectedMins = 0
                sharedViewModel.selectedSecs = 0
                updateTimerText()
                true
            }


// allow long-press to clear the modifiedAt field
            etModifiedAt.setOnLongClickListener {
                etModifiedAt.setText("")
                modifiedAtMillis = null
                mBinding.spinnerModifiedOp.setSelection(0)
                sharedViewModel.modifiedAt = null
                true
            }
            // allow long-press to clear the createdAtTo field
            etCreatedAtTo.setOnLongClickListener {
                etCreatedAtTo.setText("")
                createdAtMillisTo = null
                sharedViewModel.createdAtTo = null
                true
            }

// allow long-press to clear the modifiedAtTo field
            etModifiedAtTo.setOnLongClickListener {
                etModifiedAtTo.setText("")
                modifiedAtMillisTo = null
                sharedViewModel.modifiedAtTo = null
                true
            }



            etLastEncounteredAtTo.setOnLongClickListener {
                etLastEncounteredAtTo.setText("")
                lastEncounteredAtMillisTo = null
                true
            }

            etArticleCreatedAt.setOnClickListener {
                showDateTimePicker { text, millis ->
                    etArticleCreatedAt.setText(text)
                    filteredArticleCreatedAt = millis
                }
            }

            etArticleCreatedAtTo.setOnClickListener {
                showDateTimePicker { text, millis ->
                    etArticleCreatedAtTo.setText(text)
                    articleCreatedAtMillisTo = millis
                }
            }

            etArticleCreatedAt.setOnLongClickListener {
                etArticleCreatedAt.setText("")
                filteredArticleCreatedAt = null
                mBinding.spinnerArticleCreatedOp.setSelection(0)
                true
            }

            etArticleCreatedAtTo.setOnLongClickListener {
                etArticleCreatedAtTo.setText("")
                articleCreatedAtMillisTo = null
                true
            }


            btnTimer.setOnClickListener {
                val timePicker = com.rameez.hel.utils.TimePicker()
                timePicker.setTitle("Select time")
                //timePicker.includeHours = false
                timePicker.setOnTimeSetOption("Set time") { hour, minute, second ->
                    if (hour != 0 || minute != 0 || second != 0) {
                        sharedViewModel.selectedHours = hour
                        sharedViewModel.selectedMins = minute
                        sharedViewModel.selectedSecs = second
                        updateTimerText()
                    } else {
                        Toast.makeText(requireContext(), "Time can't be zero", Toast.LENGTH_SHORT).show()
                    }
                }


                /* To show the dialog you have to supply the "fragment manager"
                    and a tag (whatever you want)
                 */
                timePicker.show(requireActivity().supportFragmentManager, "time_picker")
//                showTimePickerDialog()
            }


            btnClearFilters.setOnClickListener {
                clearAllFilters()
            }

            // #17: Show Results as list (navigates back to WIPListFragment with filter applied)
            btnShowResults.setOnClickListener {
                applyFiltersToViewModel()
            }






//            cbWord.setOnCheckedChangeListener { _, isChecked ->
//                if (isChecked) {
//                    filteredCategoryList.add(cbWord.text.toString())
//                } else {
//                    filteredCategoryList.remove(cbWord.text.toString())
//                }
//            }
//
//            cbPhrases.setOnCheckedChangeListener { _, isChecked ->
//                if (isChecked) {
//                    filteredCategoryList.add(cbPhrases.text.toString())
//                } else {
//                    filteredCategoryList.remove(cbPhrases.text.toString())
//                }
//            }
//
//            cbIdioms.setOnCheckedChangeListener { _, isChecked ->
//                if (isChecked) {
//                    filteredCategoryList.add(cbIdioms.text.toString())
//                } else {
//                    filteredCategoryList.remove(cbIdioms.text.toString())
//                }
//            }



            btnApplyFilter.setOnClickListener {
                filteredReadCount = null
                filteredReadCountTo = null
                filteredViewedCount = null
                filteredViewedCountTo = null

                val autoScrollOn = mBinding.switchAutoScroll.isChecked
                val intervalText = mBinding.etScrollInterval.text.toString()

                filteredCreatedAt = createdAtMillis
                filteredModifiedAt = modifiedAtMillis

                sharedViewModel.readOperator = readOperator
                sharedViewModel.viewedOperator = viewedOperator
                sharedViewModel.createdOperator = createdOperator
                sharedViewModel.modifiedOperator = modifiedOperator
                sharedViewModel.lastViewedAt = filteredLastViewedAt
                sharedViewModel.lastViewedAtTo = lastViewedAtMillisTo
                sharedViewModel.lastViewedOperator = lastViewedOperator

                sharedViewModel.firstEncounteredAt = filteredFirstEncounteredAt
                sharedViewModel.firstEncounteredAtTo = firstEncounteredAtMillisTo
                sharedViewModel.firstEncounteredOperator = firstEncounteredOperator
                sharedViewModel.lastEncounteredAt = filteredLastEncounteredAt
                sharedViewModel.lastEncounteredAtTo = lastEncounteredAtMillisTo
                sharedViewModel.lastEncounteredOperator = lastEncounteredOperator

                sharedViewModel.firstViewedAt = filteredFirstViewedAt
                sharedViewModel.firstViewedAtTo = firstViewedAtMillisTo
                sharedViewModel.firstViewedOperator = firstViewedOperator

                sharedViewModel.createdAt = filteredCreatedAt
                sharedViewModel.createdAtTo = createdAtMillisTo
                sharedViewModel.modifiedAt = filteredModifiedAt
                sharedViewModel.modifiedAtTo = modifiedAtMillisTo

                sharedViewModel.articleCreatedAt = filteredArticleCreatedAt
                sharedViewModel.articleCreatedAtTo = articleCreatedAtMillisTo
                sharedViewModel.articleCreatedOperator = articleCreatedOperator


                if (!etReadCount.text.isNullOrBlank()) {
                    filteredReadCount = etReadCount.text.toString().trim().toFloat()
                }

                if (!etViewedCount.text.isNullOrBlank()) {
                    filteredViewedCount = etViewedCount.text.toString().trim().toFloat()
                }


                if (etWIP.text?.isNotBlank() == true) {
                    filteredWIP = etWIP.text?.toString()?.trim()
                }

                if (etMeaning.text?.isNotBlank() == true) {
                    filteredMeaning = etMeaning.text?.toString()?.trim()
                }

                if (etSampleSen.text?.isNotBlank() == true) {
                    filteredSampleSen = etSampleSen.text?.toString()?.trim()
                }



                // #12: Allow any positive auto scroll interval
                if (autoScrollOn) {
                    if (intervalText.isNotBlank()) {
                        val seconds = intervalText.toIntOrNull()
                        if (seconds != null && seconds > 0) {
                            sharedViewModel.isAutoScrollEnabled = true
                            sharedViewModel.autoScrollIntervalSecs = seconds
                        } else {
                            Toast.makeText(requireContext(), "Interval must be a positive number", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                    } else {
                        Toast.makeText(requireContext(), "Please enter scroll time", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                } else {
                    sharedViewModel.isAutoScrollEnabled = false
                }


                createdOperator = if (mBinding.spinnerCreatedOp.selectedItemPosition != 0)
                    mBinding.spinnerCreatedOp.selectedItem?.toString() else null

                modifiedOperator = if (mBinding.spinnerModifiedOp.selectedItemPosition != 0)
                    mBinding.spinnerModifiedOp.selectedItem?.toString() else null







                if ((lastViewedOperator == "<>" || lastViewedOperator == "!<>") &&
                    (filteredLastViewedAt == null || lastViewedAtMillisTo == null)) {
                    Toast.makeText(requireContext(), "Enter Last Viewed date range", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if ((lastViewedOperator == "<>" || lastViewedOperator == "!<>") &&
                    filteredLastViewedAt!! > lastViewedAtMillisTo!!) {
                    Toast.makeText(requireContext(), "'From' must be before 'To'", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }




                if ((lastEncounteredOperator == "<>" || lastEncounteredOperator == "!<>") &&
                    (filteredLastEncounteredAt == null || lastEncounteredAtMillisTo == null)) {
                    Toast.makeText(requireContext(), "Enter Last Encountered range", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }


                if ((firstViewedOperator == "<>" || firstViewedOperator == "!<>") &&
                    (filteredFirstViewedAt == null || firstViewedAtMillisTo == null)) {
                    Toast.makeText(requireContext(), "Enter First Viewed range", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }



                if ((firstEncounteredOperator == "<>" || firstEncounteredOperator == "!<>") &&
                    (filteredFirstEncounteredAt == null || firstEncounteredAtMillisTo == null)) {
                    Toast.makeText(requireContext(), "Enter First Encountered date range", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if ((firstEncounteredOperator == "<>" || firstEncounteredOperator == "!<>") &&
                    filteredFirstEncounteredAt!! > firstEncounteredAtMillisTo!!) {
                    Toast.makeText(requireContext(), "'From' must be before 'To'", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // ARTICLE CREATED range sanity check
                if ((articleCreatedOperator == "<>" || articleCreatedOperator == "!<>") &&
                    (filteredArticleCreatedAt == null || articleCreatedAtMillisTo == null)) {
                    Toast.makeText(requireContext(), "Enter Article Created range", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if ((articleCreatedOperator == "<>" || articleCreatedOperator == "!<>") &&
                    filteredArticleCreatedAt!! > articleCreatedAtMillisTo!!) {
                    Toast.makeText(requireContext(), "'From' must be before 'To'", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }


                // READ
                if (readOperator == "<>" || readOperator == "!<>") {
                    if (etReadCount.text.isNullOrBlank() || etReadCountTo.text.isNullOrBlank()) {
                        Toast.makeText(requireContext(), "Enter Read count range", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    filteredReadCountTo = etReadCountTo.text.toString().trim().toFloat()
                }

// VIEWED
                if (viewedOperator == "<>" || viewedOperator == "!<>") {
                    if (etViewedCount.text.isNullOrBlank() || etViewedCountTo.text.isNullOrBlank()) {
                        Toast.makeText(requireContext(), "Enter Viewed count range", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    filteredViewedCountTo = etViewedCountTo.text.toString().trim().toFloat()
                }

                // CREATED
                if (createdOperator == "<>" || createdOperator == "!<>") {
                    if (etCreatedAt.text.isNullOrBlank() || etCreatedAtTo.text.isNullOrBlank() ||
                        createdAtMillis == null || createdAtMillisTo == null) {
                        Toast.makeText(requireContext(), "Enter Created date range", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }

// MODIFIED
                if (modifiedOperator == "<>" || modifiedOperator == "!<>") {
                    if (etModifiedAt.text.isNullOrBlank() || etModifiedAtTo.text.isNullOrBlank() ||
                        modifiedAtMillis == null || modifiedAtMillisTo == null) {
                        Toast.makeText(requireContext(), "Enter Modified date range", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }


// READ range sanity check
                if ((readOperator == "<>" || readOperator == "!<>") && filteredReadCount != null && filteredReadCountTo != null) {
                    if (filteredReadCount!! > filteredReadCountTo!!) {
                        Toast.makeText(requireContext(), "Read count 'From' must be less than or equal to 'To'", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }

// VIEWED range sanity check
                if ((viewedOperator == "<>" || viewedOperator == "!<>") && filteredViewedCount != null && filteredViewedCountTo != null) {
                    if (filteredViewedCount!! > filteredViewedCountTo!!) {
                        Toast.makeText(requireContext(), "Viewed count 'From' must be less than or equal to 'To'", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }

// CREATED range sanity check
                if ((createdOperator == "<>" || createdOperator == "!<>") && filteredCreatedAt != null && createdAtMillisTo != null) {
                    if (filteredCreatedAt!! > createdAtMillisTo!!) {
                        Toast.makeText(requireContext(), "Created 'From' must be before 'To'", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }

// MODIFIED range sanity check
                if ((modifiedOperator == "<>" || modifiedOperator == "!<>") && filteredModifiedAt != null && modifiedAtMillisTo != null) {
                    if (filteredModifiedAt!! > modifiedAtMillisTo!!) {
                        Toast.makeText(requireContext(), "Modified 'From' must be before 'To'", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
                if (readOperator == null) {
                    filteredReadCount = null
                    filteredReadCountTo = null
                }
                if (viewedOperator == null) {
                    filteredViewedCount = null
                    filteredViewedCountTo = null
                }
                if (createdOperator == null) {
                    filteredCreatedAt = null
                    createdAtMillisTo = null
                }
                if (modifiedOperator == null) {
                    filteredModifiedAt = null
                    modifiedAtMillisTo = null
                }

                if (lastViewedOperator == null) {
                    filteredLastViewedAt = null
                    lastViewedAtMillisTo = null
                }

                if (firstViewedOperator == null) {
                    filteredFirstViewedAt = null
                    firstViewedAtMillisTo = null
                }

                if (firstEncounteredOperator == null) {
                    filteredFirstEncounteredAt = null
                    firstEncounteredAtMillisTo = null
                }

                if (lastEncounteredOperator == null) {
                    filteredLastEncounteredAt = null
                    lastEncounteredAtMillisTo = null
                }

                if (articleCreatedOperator == null) {
                    filteredArticleCreatedAt = null
                    articleCreatedAtMillisTo = null
                }



                wipViewModel.getWIPs()?.observe(viewLifecycleOwner) { data ->


                    if (
                    // ✅ Use ViewModel Sets here
                        sharedViewModel.selectedCategories.isNotEmpty() ||
                        sharedViewModel.selectedTags.isNotEmpty() ||
                        filteredReadCount != null ||
                        filteredViewedCount != null ||
                        filteredWIP != null ||
                        filteredMeaning != null ||
                        filteredSampleSen != null ||
                        filteredCreatedAt != null ||
                        filteredModifiedAt != null ||
                        filteredLastViewedAt != null ||
                        filteredFirstEncounteredAt != null ||
                        filteredLastEncounteredAt != null ||
                        filteredFirstViewedAt != null ||
                        filteredArticleCreatedAt != null ||
                        readOperator == "null" || readOperator == "!null" ||
                        viewedOperator == "null" || viewedOperator == "!null" ||
                        createdOperator == "null" || createdOperator == "!null" ||
                        modifiedOperator == "null" || modifiedOperator == "!null" ||
                        lastViewedOperator == "null" || lastViewedOperator == "!null" ||
                        firstEncounteredOperator == "null" || firstEncounteredOperator == "!null" ||
                        lastEncounteredOperator == "null" || lastEncounteredOperator == "!null" ||
                        firstViewedOperator == "null" || firstViewedOperator == "!null" ||
                        articleCreatedOperator == "null" || articleCreatedOperator == "!null"
                    ) {

                        var filteredData = data

                        if (readOperator != "<>" && readOperator != "!<>") {
                            etReadCountTo.text = null
                            filteredReadCountTo = null
                        }

                        if (viewedOperator != "<>" && viewedOperator != "!<>") {
                            etViewedCountTo.text = null
                            filteredViewedCountTo = null
                        }

                        if (createdOperator != "<>" && createdOperator != "!<>") {
                            etCreatedAtTo.text = null
                            createdAtMillisTo = null
                        }

                        if (modifiedOperator != "<>" && modifiedOperator != "!<>") {
                            etModifiedAtTo.text = null
                            modifiedAtMillisTo = null
                        }

                        if (articleCreatedOperator != "<>" && articleCreatedOperator != "!<>") {
                            etArticleCreatedAtTo.text = null
                            articleCreatedAtMillisTo = null
                        }
                        sharedViewModel.isReadAloud = switchMaterial.isChecked

                        CoroutineScope(Dispatchers.IO).launch {

                            if (filteredArticleCreatedAt != null || articleCreatedOperator == "null" || articleCreatedOperator == "!null") {
                                filteredData = filteredData.filter { wip ->
                                    matchDateOperator(
                                        ts = wip.lastParaCreatedAt,
                                        operator = articleCreatedOperator,
                                        from = filteredArticleCreatedAt ?: 0L,
                                        to = articleCreatedAtMillisTo
                                    )
                                }
                            }

                            if (filteredWIP != null) {
                                filteredData = filteredData.filter { wipItem ->
                                    wipItem.wip?.contains(filteredWIP ?: "", ignoreCase = true) == true
                                }
                            }

                            if (filteredMeaning != null && filteredData.isNotEmpty()) {
                                filteredData = filteredData.filter { wipItem ->
                                    wipItem.meaning?.contains(filteredMeaning ?:  "", true) == true
                                }
                            }

                            if (filteredSampleSen != null && filteredData.isNotEmpty()) {
                                filteredData = filteredData.filter { wipItem ->
                                    wipItem.sampleSentence?.contains(filteredSampleSen ?: "", true) == true
                                }
                            }




                            if (filteredData.isNotEmpty() && sharedViewModel.selectedCategories.isNotEmpty()) {
                                val selectedCatsLower = sharedViewModel.selectedCategories.map { it.lowercase(Locale.ROOT) }
                                filteredData = filteredData.filter { wipItem ->
                                    wipItem.category?.lowercase(Locale.ROOT) in selectedCatsLower
                                }
                            }

                            if (filteredData.isNotEmpty() && sharedViewModel.selectedTags.isNotEmpty()) {
                                val selectedTagsLower = sharedViewModel.selectedTags.map { it.lowercase(Locale.ROOT) }
                                filteredData = filteredData.filter { wipItem ->
                                    wipItem.customTag?.any { tag ->
                                        tag.lowercase(Locale.ROOT) in selectedTagsLower
                                    } ?: false
                                }
                            }


                            if (filteredData.isNotEmpty() && (filteredReadCount != null || readOperator == "null" || readOperator == "!null")) {
                                val from = filteredReadCount ?: 0f
                                val to = filteredReadCountTo

                                filteredData = filteredData.filter { wipItem ->
                                    val rc = wipItem.readCount
                                    when (readOperator) {
                                        "null" -> rc == null || rc == 0f
                                        "!null" -> rc != null && rc != 0f
                                        "="  -> rc != null && rc == from
                                        "!=" -> rc == null || rc != from
                                        ">"  -> rc != null && rc > from
                                        "<"  -> rc != null && rc < from
                                        ">=" -> rc != null && rc >= from
                                        "<=" -> rc != null && rc <= from
                                        "<>" -> rc != null && to != null && rc in from..to
                                        "!<>" -> to != null && (rc == null || rc !in from..to)
                                        else -> false
                                    }
                                }
                            }



                            if (filteredData.isNotEmpty() && (filteredLastViewedAt != null || lastViewedOperator == "null" || lastViewedOperator == "!null")) {
                                val from = filteredLastViewedAt ?: 0L
                                val to = lastViewedAtMillisTo

                                filteredData = filteredData.filter { wip ->
                                    matchDateOperator(
                                        ts = wip.displayCountUpdatedAt,
                                        operator = lastViewedOperator,
                                        from = from,
                                        to = to
                                    )
                                }
                            }



                            if (filteredData.isNotEmpty() && (filteredViewedCount != null || viewedOperator == "null" || viewedOperator == "!null")) {
                                val vcFilter = filteredViewedCount ?: 0f
                                val to = filteredViewedCountTo
                                filteredData = filteredData.filter { wipItem ->
                                    val vc = wipItem.displayCount
                                    when (viewedOperator) {
                                        "null" -> vc == null || vc == 0f
                                        "!null" -> vc != null && vc != 0f
                                        "="  -> vc != null && vc == vcFilter
                                        "!=" -> vc == null || vc != vcFilter
                                        ">"  -> vc != null && vc > vcFilter
                                        "<"  -> vc != null && vc < vcFilter
                                        ">=" -> vc != null && vc >= vcFilter
                                        "<=" -> vc != null && vc <= vcFilter
                                        "<>" -> vc != null && to != null && vc in vcFilter..to
                                        "!<>" -> to != null && (vc == null || vc !in vcFilter..to)
                                        else -> false
                                    }
                                }
                            }

                            if (filteredData.isNotEmpty() && (filteredFirstEncounteredAt != null || firstEncounteredOperator == "null" || firstEncounteredOperator == "!null")) {
                                val from = filteredFirstEncounteredAt ?: 0L
                                val to = firstEncounteredAtMillisTo

                                filteredData = filteredData.filter { wip ->
                                    matchDateOperator(
                                        ts = wip.firstEncounteredAt,
                                        operator = firstEncounteredOperator,
                                        from = from,
                                        to = to
                                    )
                                }
                            }


                            if (filteredData.isNotEmpty() && (filteredCreatedAt != null || createdOperator == "null" || createdOperator == "!null")) {
                                val from = filteredCreatedAt ?: 0L
                                val to = createdAtMillisTo

                                filteredData = filteredData.filter { wip ->
                                    matchDateOperator(
                                        ts = wip.createdAt,
                                        operator = createdOperator,
                                        from = from,
                                        to = to
                                    )
                                }
                            }


                            if (filteredData.isNotEmpty() && (filteredModifiedAt != null || modifiedOperator == "null" || modifiedOperator == "!null")) {
                                val from = filteredModifiedAt ?: 0L
                                val to = modifiedAtMillisTo

                                filteredData = filteredData.filter { wip ->
                                    matchDateOperator(
                                        ts = wip.modifiedAt,
                                        operator = modifiedOperator,
                                        from = from,
                                        to = to
                                    )
                                }
                            }

                            if (filteredData.isNotEmpty() && (filteredLastEncounteredAt != null || lastEncounteredOperator == "null" || lastEncounteredOperator == "!null")) {
                                val from = filteredLastEncounteredAt ?: 0L
                                val to = lastEncounteredAtMillisTo

                                filteredData = filteredData.filter { wip ->
                                    matchDateOperator(
                                        ts = wip.readCountUpdatedAt,
                                        operator = lastEncounteredOperator,
                                        from = from,
                                        to = to
                                    )
                                }
                            }

                            if (filteredData.isNotEmpty() && (filteredFirstViewedAt != null || firstViewedOperator == "null" || firstViewedOperator == "!null")) {
                                val from = filteredFirstViewedAt ?: 0L
                                val to = firstViewedAtMillisTo

                                filteredData = filteredData.filter { wip ->
                                    matchDateOperator(
                                        ts = wip.firstViewedAt,
                                        operator = firstViewedOperator,
                                        from = from,
                                        to = to
                                    )
                                }
                            }





                            // #14: Sort filtered results
                            filteredData = sortFilteredList(filteredData)

                            var limit = 0
                            if (etLimit.text.toString().isNotBlank()) {
                                limit = mBinding.etLimit.text.toString().toInt()
                            }
                            if (etTimer.text.toString().isNotBlank()) {
                                sharedViewModel.filteredWipsList = filteredData.toMutableList()
                            } else if (etTimer.text.toString().isBlank() && etLimit.text.toString().isBlank()) {
                                sharedViewModel.filteredWipsList = filteredData.toMutableList()
                            } else {
                                if (limit > 0) {
                                    sharedViewModel.filteredWipsList = filteredData.take(limit).toMutableList()
                                }
                            }


                            filteredData.forEach {
                                Log.d("TAG", "filteredWips $it")
                            }
                        }
                    }
                    if (mBinding.etLimit.text.toString()
                            .isNotBlank() && mBinding.etLimit.text.toString().toInt() == 0
                    ) {
                        Toast.makeText(requireContext(), "Limit can't be zero", Toast.LENGTH_SHORT)
                            .show()
                    } else {

                        // Use the ViewModel sets here as well
                        if (sharedViewModel.selectedCategories.isEmpty() &&
                            sharedViewModel.selectedTags.isEmpty() &&
                            filteredReadCount == null &&
                            filteredViewedCount == null &&
                            filteredWIP == null &&
                            filteredCreatedAt == null &&
                            filteredModifiedAt == null &&
                            filteredLastViewedAt == null &&
                            filteredFirstEncounteredAt == null &&
                            filteredLastEncounteredAt == null &&
                            filteredFirstViewedAt == null &&
                            filteredArticleCreatedAt == null &&
                            readOperator != "null" && readOperator != "!null" &&
                            viewedOperator != "null" && viewedOperator != "!null" &&
                            createdOperator != "null" && createdOperator != "!null" &&
                            modifiedOperator != "null" && modifiedOperator != "!null" &&
                            lastViewedOperator != "null" && lastViewedOperator != "!null" &&
                            firstEncounteredOperator != "null" && firstEncounteredOperator != "!null" &&
                            lastEncounteredOperator != "null" && lastEncounteredOperator != "!null" &&
                            firstViewedOperator != "null" && firstViewedOperator != "!null" &&
                            articleCreatedOperator != "null" && articleCreatedOperator != "!null") {
                            Toast.makeText(requireContext(), "Nothing to filter", Toast.LENGTH_SHORT).show()
                        }
                        else {
                            sharedViewModel.isFilterApplied = true
                            findNavController().navigate(R.id.carouselFragment)

                        }
                    }
                }

            }


            switchMaterial.setOnCheckedChangeListener { _, isChecked ->
                layoutTtsOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
                sharedViewModel.isReadAloud = isChecked

                if (!isChecked) {
                    // Clear TTS options when switch is off
                    sharedViewModel.ttsOptions.clear()
                    cbWord.isChecked = false
                    cbMeaning.isChecked = false
                    cbSentence.isChecked = false
                } else {
                    // If ttsOptions is empty, set "Word" by default
                    if (sharedViewModel.ttsOptions.isEmpty()) {
                        sharedViewModel.ttsOptions.add("Word")
                    }

                    // Restore previously selected options
                    cbWord.isChecked = sharedViewModel.ttsOptions.contains("Word")
                    cbMeaning.isChecked = sharedViewModel.ttsOptions.contains("Meaning")
                    cbSentence.isChecked = sharedViewModel.ttsOptions.contains("Sentence")
                }
            }

// Update sharedViewModel whenever a checkbox is clicked
            val checkBoxes = listOf(
                cbWord to "Word",
                cbMeaning to "Meaning",
                cbSentence to "Sentence"
            )
            checkBoxes.forEach { (cb, option) ->
                cb.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        if (!sharedViewModel.ttsOptions.contains(option)) {
                            sharedViewModel.ttsOptions.add(option)
                        }
                    } else {
                        sharedViewModel.ttsOptions.remove(option)
                    }
                }
            }

        }



    }


    private fun sortSpinnerSetup() {
        val sortOptions = listOf(
            "", "Last Viewed", "First Viewed", "Last Encountered", "First Encountered", "Created", "Modified", "Para Created", "Viewed Count", "Encountered Count"
        )
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mBinding.spinnerSortBy.adapter = adapter

        // Restore previous selection
        val previousSort = sharedViewModel.sortBy
        if (previousSort != null) {
            val idx = sortOptions.indexOf(previousSort)
            if (idx >= 0) mBinding.spinnerSortBy.setSelection(idx)
        }

        mBinding.spinnerSortBy.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                sharedViewModel.sortBy = if (position > 0) parent.getItemAtPosition(position).toString() else null
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Sort order toggle button
        updateSortOrderButton()
        mBinding.btnSortOrder.setOnClickListener {
            sharedViewModel.sortAscending = !sharedViewModel.sortAscending
            updateSortOrderButton()
        }
    }

    private fun updateSortOrderButton() {
        if (sharedViewModel.sortAscending) {
            mBinding.btnSortOrder.text = "ASC \u2191"  // up arrow
        } else {
            mBinding.btnSortOrder.text = "DESC \u2193" // down arrow
        }
    }

    private fun sortFilteredList(list: List<WIPModel>): List<WIPModel> {
        val asc = sharedViewModel.sortAscending
        return when (sharedViewModel.sortBy) {
            "Last Viewed" -> if (asc) list.sortedBy { it.displayCountUpdatedAt } else list.sortedByDescending { it.displayCountUpdatedAt }
            "First Viewed" -> if (asc) list.sortedBy { it.firstViewedAt } else list.sortedByDescending { it.firstViewedAt }
            "Last Encountered" -> if (asc) list.sortedBy { it.readCountUpdatedAt } else list.sortedByDescending { it.readCountUpdatedAt }
            "First Encountered" -> if (asc) list.sortedBy { it.firstEncounteredAt } else list.sortedByDescending { it.firstEncounteredAt }
            "Created" -> if (asc) list.sortedBy { it.createdAt } else list.sortedByDescending { it.createdAt }
            "Modified" -> if (asc) list.sortedBy { it.modifiedAt } else list.sortedByDescending { it.modifiedAt }
            "Para Created" -> if (asc) list.sortedBy { it.lastParaCreatedAt } else list.sortedByDescending { it.lastParaCreatedAt }
            "Viewed Count" -> if (asc) list.sortedBy { it.displayCount ?: 0f } else list.sortedByDescending { it.displayCount ?: 0f }
            "Encountered Count" -> if (asc) list.sortedBy { it.readCount ?: 0f } else list.sortedByDescending { it.readCount ?: 0f }
            else -> list.shuffled()
        }
    }

    private fun readCurrentFilterValues() {
        // Text filters
        filteredWIP = if (mBinding.etWIP.text?.isNotBlank() == true) mBinding.etWIP.text.toString().trim() else null
        filteredMeaning = if (mBinding.etMeaning.text?.isNotBlank() == true) mBinding.etMeaning.text.toString().trim() else null
        filteredSampleSen = if (mBinding.etSampleSen.text?.isNotBlank() == true) mBinding.etSampleSen.text.toString().trim() else null

        // Re-read ALL operators directly from spinners to avoid stale listener values
        readOperator = if (mBinding.readSpinner.selectedItemPosition != 0) mBinding.readSpinner.selectedItem?.toString() else null
        viewedOperator = if (mBinding.viewedSpinner.selectedItemPosition != 0) mBinding.viewedSpinner.selectedItem?.toString() else null
        createdOperator = if (mBinding.spinnerCreatedOp.selectedItemPosition != 0) mBinding.spinnerCreatedOp.selectedItem?.toString() else null
        modifiedOperator = if (mBinding.spinnerModifiedOp.selectedItemPosition != 0) mBinding.spinnerModifiedOp.selectedItem?.toString() else null
        lastViewedOperator = if (mBinding.spinnerLastViewedOp.selectedItemPosition != 0) mBinding.spinnerLastViewedOp.selectedItem?.toString() else null
        firstEncounteredOperator = if (mBinding.spinnerFirstEncounteredOp.selectedItemPosition != 0) mBinding.spinnerFirstEncounteredOp.selectedItem?.toString() else null
        lastEncounteredOperator = if (mBinding.spinnerLastEncounteredOp.selectedItemPosition != 0) mBinding.spinnerLastEncounteredOp.selectedItem?.toString() else null
        firstViewedOperator = if (mBinding.spinnerFirstViewedOp.selectedItemPosition != 0) mBinding.spinnerFirstViewedOp.selectedItem?.toString() else null
        articleCreatedOperator = if (mBinding.spinnerArticleCreatedOp.selectedItemPosition != 0) mBinding.spinnerArticleCreatedOp.selectedItem?.toString() else null

        // Count filters
        filteredReadCount = if (!mBinding.etReadCount.text.isNullOrBlank()) mBinding.etReadCount.text.toString().trim().toFloatOrNull() else null
        filteredViewedCount = if (!mBinding.etViewedCount.text.isNullOrBlank()) mBinding.etViewedCount.text.toString().trim().toFloatOrNull() else null

        if (readOperator == "<>" || readOperator == "!<>") {
            filteredReadCountTo = if (!mBinding.etReadCountTo.text.isNullOrBlank()) mBinding.etReadCountTo.text.toString().trim().toFloatOrNull() else null
        } else {
            filteredReadCountTo = null
        }

        if (viewedOperator == "<>" || viewedOperator == "!<>") {
            filteredViewedCountTo = if (!mBinding.etViewedCountTo.text.isNullOrBlank()) mBinding.etViewedCountTo.text.toString().trim().toFloatOrNull() else null
        } else {
            filteredViewedCountTo = null
        }

        // Timestamp filters
        filteredCreatedAt = createdAtMillis
        filteredModifiedAt = modifiedAtMillis

        // Save operators to sharedViewModel
        sharedViewModel.readOperator = readOperator
        sharedViewModel.viewedOperator = viewedOperator
        sharedViewModel.createdOperator = createdOperator
        sharedViewModel.modifiedOperator = modifiedOperator
        sharedViewModel.lastViewedAt = filteredLastViewedAt
        sharedViewModel.lastViewedAtTo = lastViewedAtMillisTo
        sharedViewModel.lastViewedOperator = lastViewedOperator
        sharedViewModel.firstEncounteredAt = filteredFirstEncounteredAt
        sharedViewModel.firstEncounteredAtTo = firstEncounteredAtMillisTo
        sharedViewModel.firstEncounteredOperator = firstEncounteredOperator
        sharedViewModel.lastEncounteredAt = filteredLastEncounteredAt
        sharedViewModel.lastEncounteredAtTo = lastEncounteredAtMillisTo
        sharedViewModel.lastEncounteredOperator = lastEncounteredOperator
        sharedViewModel.firstViewedAt = filteredFirstViewedAt
        sharedViewModel.firstViewedAtTo = firstViewedAtMillisTo
        sharedViewModel.firstViewedOperator = firstViewedOperator
        sharedViewModel.createdAt = filteredCreatedAt
        sharedViewModel.createdAtTo = createdAtMillisTo
        sharedViewModel.modifiedAt = filteredModifiedAt
        sharedViewModel.modifiedAtTo = modifiedAtMillisTo
        sharedViewModel.articleCreatedAt = filteredArticleCreatedAt
        sharedViewModel.articleCreatedAtTo = articleCreatedAtMillisTo
        sharedViewModel.articleCreatedOperator = articleCreatedOperator

        // Null out filter values when operator is not set
        if (readOperator == null) { filteredReadCount = null; filteredReadCountTo = null }
        if (viewedOperator == null) { filteredViewedCount = null; filteredViewedCountTo = null }
        if (createdOperator == null) { filteredCreatedAt = null; createdAtMillisTo = null }
        if (modifiedOperator == null) { filteredModifiedAt = null; modifiedAtMillisTo = null }
        if (lastViewedOperator == null) { filteredLastViewedAt = null; lastViewedAtMillisTo = null }
        if (firstViewedOperator == null) { filteredFirstViewedAt = null; firstViewedAtMillisTo = null }
        if (firstEncounteredOperator == null) { filteredFirstEncounteredAt = null; firstEncounteredAtMillisTo = null }
        if (lastEncounteredOperator == null) { filteredLastEncounteredAt = null; lastEncounteredAtMillisTo = null }
        if (articleCreatedOperator == null) { filteredArticleCreatedAt = null; articleCreatedAtMillisTo = null }
    }

    private fun applyFiltersToViewModel() {
        readCurrentFilterValues()

        // Check if any filter is actually set (same guard as flashcard button)
        val hasAnyFilter = sharedViewModel.selectedCategories.isNotEmpty() ||
                sharedViewModel.selectedTags.isNotEmpty() ||
                filteredReadCount != null ||
                filteredViewedCount != null ||
                filteredWIP != null ||
                filteredMeaning != null ||
                filteredSampleSen != null ||
                filteredCreatedAt != null ||
                filteredModifiedAt != null ||
                filteredLastViewedAt != null ||
                filteredFirstEncounteredAt != null ||
                filteredLastEncounteredAt != null ||
                filteredFirstViewedAt != null ||
                filteredArticleCreatedAt != null ||
                readOperator == "null" || readOperator == "!null" ||
                viewedOperator == "null" || viewedOperator == "!null" ||
                createdOperator == "null" || createdOperator == "!null" ||
                modifiedOperator == "null" || modifiedOperator == "!null" ||
                lastViewedOperator == "null" || lastViewedOperator == "!null" ||
                firstEncounteredOperator == "null" || firstEncounteredOperator == "!null" ||
                lastEncounteredOperator == "null" || lastEncounteredOperator == "!null" ||
                firstViewedOperator == "null" || firstViewedOperator == "!null" ||
                articleCreatedOperator == "null" || articleCreatedOperator == "!null"

        if (!hasAnyFilter) {
            Toast.makeText(requireContext(), "Nothing to filter", Toast.LENGTH_SHORT).show()
            return
        }

        // Limit validation
        if (mBinding.etLimit.text.toString().isNotBlank() && mBinding.etLimit.text.toString().toIntOrNull() == 0) {
            Toast.makeText(requireContext(), "Limit can't be zero", Toast.LENGTH_SHORT).show()
            return
        }

        // Use lifecycleScope with suspend getWIPs2() to avoid LiveData observer issues
        viewLifecycleOwner.lifecycleScope.launch {
            val data = wipViewModel.getWIPs2() ?: emptyList()

            // Apply all filters
            var filteredData = data

            if (filteredWIP != null) {
                filteredData = filteredData.filter { wipItem ->
                    wipItem.wip?.contains(filteredWIP ?: "", ignoreCase = true) == true
                }
            }

            if (filteredMeaning != null) {
                filteredData = filteredData.filter { wipItem ->
                    wipItem.meaning?.contains(filteredMeaning ?: "", true) == true
                }
            }

            if (filteredSampleSen != null) {
                filteredData = filteredData.filter { wipItem ->
                    wipItem.sampleSentence?.contains(filteredSampleSen ?: "", true) == true
                }
            }

            if (sharedViewModel.selectedCategories.isNotEmpty()) {
                val selectedCatsLower = sharedViewModel.selectedCategories.map { it.lowercase(Locale.ROOT) }
                filteredData = filteredData.filter { wipItem ->
                    wipItem.category?.lowercase(Locale.ROOT) in selectedCatsLower
                }
            }

            if (sharedViewModel.selectedTags.isNotEmpty()) {
                val selectedTagsLower = sharedViewModel.selectedTags.map { it.lowercase(Locale.ROOT) }
                filteredData = filteredData.filter { wipItem ->
                    wipItem.customTag?.any { tag ->
                        tag.lowercase(Locale.ROOT) in selectedTagsLower
                    } ?: false
                }
            }

            if (filteredReadCount != null || readOperator == "null" || readOperator == "!null") {
                val from = filteredReadCount ?: 0f
                val to = filteredReadCountTo
                filteredData = filteredData.filter { wipItem ->
                    val rc = wipItem.readCount
                    when (readOperator) {
                        "null" -> rc == null || rc == 0f
                        "!null" -> rc != null && rc != 0f
                        "="  -> rc != null && rc == from
                        "!=" -> rc == null || rc != from
                        ">"  -> rc != null && rc > from
                        "<"  -> rc != null && rc < from
                        ">=" -> rc != null && rc >= from
                        "<=" -> rc != null && rc <= from
                        "<>" -> rc != null && to != null && rc in from..to
                        "!<>" -> to != null && (rc == null || rc !in from..to)
                        else -> false
                    }
                }
            }

            if (filteredViewedCount != null || viewedOperator == "null" || viewedOperator == "!null") {
                val vcFilter = filteredViewedCount ?: 0f
                val to = filteredViewedCountTo
                filteredData = filteredData.filter { wipItem ->
                    val vc = wipItem.displayCount
                    when (viewedOperator) {
                        "null" -> vc == null || vc == 0f
                        "!null" -> vc != null && vc != 0f
                        "="  -> vc != null && vc == vcFilter
                        "!=" -> vc == null || vc != vcFilter
                        ">"  -> vc != null && vc > vcFilter
                        "<"  -> vc != null && vc < vcFilter
                        ">=" -> vc != null && vc >= vcFilter
                        "<=" -> vc != null && vc <= vcFilter
                        "<>" -> vc != null && to != null && vc in vcFilter..to
                        "!<>" -> to != null && (vc == null || vc !in vcFilter..to)
                        else -> false
                    }
                }
            }

            if (filteredLastViewedAt != null || lastViewedOperator == "null" || lastViewedOperator == "!null") {
                filteredData = filteredData.filter { wip ->
                    matchDateOperator(ts = wip.displayCountUpdatedAt, operator = lastViewedOperator, from = filteredLastViewedAt ?: 0L, to = lastViewedAtMillisTo)
                }
            }

            if (filteredFirstEncounteredAt != null || firstEncounteredOperator == "null" || firstEncounteredOperator == "!null") {
                filteredData = filteredData.filter { wip ->
                    matchDateOperator(ts = wip.firstEncounteredAt, operator = firstEncounteredOperator, from = filteredFirstEncounteredAt ?: 0L, to = firstEncounteredAtMillisTo)
                }
            }

            if (filteredCreatedAt != null || createdOperator == "null" || createdOperator == "!null") {
                filteredData = filteredData.filter { wip ->
                    matchDateOperator(ts = wip.createdAt, operator = createdOperator, from = filteredCreatedAt ?: 0L, to = createdAtMillisTo)
                }
            }

            if (filteredModifiedAt != null || modifiedOperator == "null" || modifiedOperator == "!null") {
                filteredData = filteredData.filter { wip ->
                    matchDateOperator(ts = wip.modifiedAt, operator = modifiedOperator, from = filteredModifiedAt ?: 0L, to = modifiedAtMillisTo)
                }
            }

            if (filteredLastEncounteredAt != null || lastEncounteredOperator == "null" || lastEncounteredOperator == "!null") {
                filteredData = filteredData.filter { wip ->
                    matchDateOperator(ts = wip.readCountUpdatedAt, operator = lastEncounteredOperator, from = filteredLastEncounteredAt ?: 0L, to = lastEncounteredAtMillisTo)
                }
            }

            if (filteredFirstViewedAt != null || firstViewedOperator == "null" || firstViewedOperator == "!null") {
                filteredData = filteredData.filter { wip ->
                    matchDateOperator(ts = wip.firstViewedAt, operator = firstViewedOperator, from = filteredFirstViewedAt ?: 0L, to = firstViewedAtMillisTo)
                }
            }

            if (filteredArticleCreatedAt != null || articleCreatedOperator == "null" || articleCreatedOperator == "!null") {
                filteredData = filteredData.filter { wip ->
                    matchDateOperator(ts = wip.lastParaCreatedAt, operator = articleCreatedOperator, from = filteredArticleCreatedAt ?: 0L, to = articleCreatedAtMillisTo)
                }
            }

            // Sort results
            filteredData = sortFilteredList(filteredData)

            // Apply limit if set
            val limitText = mBinding.etLimit.text.toString()
            if (limitText.isNotBlank()) {
                val limit = limitText.toIntOrNull()
                if (limit != null && limit > 0) {
                    filteredData = filteredData.take(limit)
                }
            }

            sharedViewModel.filteredWipsList = filteredData.toMutableList()
            sharedViewModel.isFilterApplied = true
            sharedViewModel.isShowingFilteredResults = true

            Log.d("ShowList", "Filtered: ${filteredData.size} / ${data.size} items, categories=${sharedViewModel.selectedCategories}, tags=${sharedViewModel.selectedTags}")
            Toast.makeText(requireContext(), "Filtered: ${filteredData.size} of ${data.size}", Toast.LENGTH_SHORT).show()

            // Navigate back to WIPListFragment
            findNavController().popBackStack(R.id.WIPListFragment, false)
        }
    }


    private fun updateTimerText() {
        val h = sharedViewModel.selectedHours ?: 0
        val m = sharedViewModel.selectedMins ?: 0
        val s = sharedViewModel.selectedSecs ?: 0
        if (h != 0 || m != 0 || s != 0) {
            mBinding.etTimer.setText(String.format("%02d:%02d:%02d", h, m, s))
        } else {
            mBinding.etTimer.setText("")
        }
    }

    private fun restoreSpinnerSelection(spinner: Spinner, value: String?) {
        if (value == null) return
        val adapter = spinner.adapter ?: return
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString() == value) {
                spinner.setSelection(i)
                break
            }
        }
    }


    private fun setUpRecyclerView() {
        mBinding.apply {
            rvList.layoutManager = GridLayoutManager(requireContext(), 3)
            rvList.adapter = customTagAdapter // initialized in onCreate with sharedViewModel.selectedTags

            categoryRv.layoutManager = GridLayoutManager(requireContext(), 3)
            categoryRv.adapter = categoryAdapter // initialized in onCreate with sharedViewModel.selectedCategories
        }
    }


    private fun readOperatorSetup() {
        // include all operators, keep empty default at index 0
        val readCountsStr = mutableListOf("=", "<", ">", "<=", ">=", "<>", "!<>", "!=", "null", "!null")
        readCountsStr.add(0, "")
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, readCountsStr)
        mBinding.readSpinner.setSelection(0)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mBinding.readSpinner.adapter = spinnerAdapter
        mBinding.readSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position != 0) {
                    val selectedItem = parent.getItemAtPosition(position)
                    if (selectedItem.toString() != "") {
                        readOperator = selectedItem.toString()
                        Log.d("TAG", "readOperator set to $readOperator")

                        mBinding.etReadCountTo.visibility =
                            if (readOperator == "<>" || readOperator == "!<>") View.VISIBLE else View.GONE
                    }
                } else {
                    readOperator = null
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
    }


    private fun viewedOperatorSetup() {
        val viewedCountsStr = mutableListOf("=", "<", ">", "<=", ">=", "<>", "!<>", "!=", "null", "!null")
        viewedCountsStr.add(0, "")
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, viewedCountsStr)
        mBinding.viewedSpinner.setSelection(0)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mBinding.viewedSpinner.adapter = spinnerAdapter
        mBinding.viewedSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    if (position != 0) {
                        val selectedItem = parent.getItemAtPosition(position)
                        if (selectedItem.toString() != "") {
                            viewedOperator = selectedItem.toString()
                            Log.d("TAG", "viewedOperator set to $viewedOperator")

                            mBinding.etViewedCountTo.visibility =
                                if (viewedOperator == "<>" || viewedOperator == "!<>") View.VISIBLE else View.GONE
                        }
                    } else {
                        viewedOperator = null
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                }
            }
    }



    private fun createdOperatorSetup() {
        val ops = mutableListOf("", "=", "<", ">", "<=", ">=", "<>", "!<>", "!=", "null", "!null")
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, ops)
        mBinding.spinnerCreatedOp.setSelection(0)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mBinding.spinnerCreatedOp.adapter = spinnerAdapter
        mBinding.spinnerCreatedOp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position != 0) {
                    val selectedItem = parent.getItemAtPosition(position)
                    if (selectedItem.toString() != "") {
                        createdOperator = selectedItem.toString()
                        mBinding.etCreatedAtTo.visibility =
                            if (createdOperator == "<>" || createdOperator == "!<>") View.VISIBLE else View.GONE
                    }
                } else {
                    createdOperator = null
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun modifiedOperatorSetup() {
        val ops = mutableListOf("", "=", "<", ">", "<=", ">=", "<>", "!<>", "!=", "null", "!null")
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, ops)
        mBinding.spinnerModifiedOp.setSelection(0)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mBinding.spinnerModifiedOp.adapter = spinnerAdapter
        mBinding.spinnerModifiedOp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                if (position != 0) {
                    val selectedItem = parent.getItemAtPosition(position)
                    if (selectedItem.toString() != "") {
                        modifiedOperator = selectedItem.toString()

                        mBinding.etModifiedAtTo.visibility =
                            if (modifiedOperator == "<>" || modifiedOperator == "!<>") View.VISIBLE else View.GONE

                    }
                } else {
                    modifiedOperator = null
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }


    private fun lastViewedOperatorSetup() {
        val ops = mutableListOf("", "=", "<", ">", "<=", ">=", "<>", "!<>", "!=", "null", "!null")
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, ops)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mBinding.spinnerLastViewedOp.adapter = adapter


        mBinding.spinnerLastViewedOp.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                    lastViewedOperator = p.getItemAtPosition(pos).toString().takeIf { it.isNotBlank() }
                    mBinding.etLastViewedAtTo.visibility =
                        if (lastViewedOperator == "<>" || lastViewedOperator == "!<>") View.VISIBLE else View.GONE
                }
                override fun onNothingSelected(p: AdapterView<*>) {}
            }
    }

    private fun firstEncounteredOperatorSetup() {
        val ops = mutableListOf("", "=", "<", ">", "<=", ">=", "<>", "!<>", "!=", "null", "!null")
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, ops)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mBinding.spinnerFirstEncounteredOp.adapter = adapter

        mBinding.spinnerFirstEncounteredOp.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                    firstEncounteredOperator = p.getItemAtPosition(pos).toString().takeIf { it.isNotBlank() }
                    mBinding.etFirstEncounteredAtTo.visibility =
                        if (firstEncounteredOperator == "<>" || firstEncounteredOperator == "!<>") View.VISIBLE else View.GONE
                }
                override fun onNothingSelected(p: AdapterView<*>) {}
            }
    }

    private fun lastEncounteredOperatorSetup() {
        val ops = mutableListOf("", "=", "<", ">", "<=", ">=", "<>", "!<>", "!=", "null", "!null")
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, ops)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mBinding.spinnerLastEncounteredOp.adapter = adapter

        mBinding.spinnerLastEncounteredOp.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                    lastEncounteredOperator =
                        p.getItemAtPosition(pos).toString().takeIf { it.isNotBlank() }
                    mBinding.etLastEncounteredAtTo.visibility =
                        if (lastEncounteredOperator == "<>" || lastEncounteredOperator == "!<>") View.VISIBLE else View.GONE
                }
                override fun onNothingSelected(p: AdapterView<*>) {}
            }
    }


    private fun firstViewedOperatorSetup() {
        val ops = mutableListOf("", "=", "<", ">", "<=", ">=", "<>", "!<>", "!=", "null", "!null")
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, ops)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mBinding.spinnerFirstViewedOp.adapter = adapter

        mBinding.spinnerFirstViewedOp.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                    firstViewedOperator =
                        p.getItemAtPosition(pos).toString().takeIf { it.isNotBlank() }
                    mBinding.etFirstViewedAtTo.visibility =
                        if (firstViewedOperator == "<>" || firstViewedOperator == "!<>") View.VISIBLE else View.GONE
                }
                override fun onNothingSelected(p: AdapterView<*>) {}
            }
    }

    private fun articleCreatedOperatorSetup() {
        val ops = mutableListOf("", "=", "<", ">", "<=", ">=", "<>", "!<>", "!=", "null", "!null")
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, ops)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mBinding.spinnerArticleCreatedOp.adapter = adapter

        mBinding.spinnerArticleCreatedOp.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                    articleCreatedOperator =
                        p.getItemAtPosition(pos).toString().takeIf { it.isNotBlank() }
                    mBinding.etArticleCreatedAtTo.visibility =
                        if (articleCreatedOperator == "<>" || articleCreatedOperator == "!<>") View.VISIBLE else View.GONE
                }
                override fun onNothingSelected(p: AdapterView<*>) {}
            }
    }


    private fun matchDateOperator(
        ts: Long?,
        operator: String?,
        from: Long,
        to: Long? = null
    ): Boolean {

        // #16: null/!null operators
        if (operator == "null") return ts == null || ts == 0L
        if (operator == "!null") return ts != null && ts != 0L

        // For negation operators, null/0 timestamps should match
        // (null is "not equal to" any date, null is "not between" any range)
        if (operator == "!=" || operator == "!<>") {
            if (ts == null || ts == 0L) return true
        }

        // exclude empty / never-used timestamps for other operators
        if (ts == null || ts == 0L) return false

        return when (operator) {

            "=" -> {
                // same minute match
                val minuteStart = from - (from % 60_000)
                val minuteEnd = minuteStart + 59_999
                ts in minuteStart..minuteEnd
            }

            // #15: NOT equal
            "!=" -> {
                val minuteStart = from - (from % 60_000)
                val minuteEnd = minuteStart + 59_999
                ts !in minuteStart..minuteEnd
            }

            "<"  -> ts < from
            "<=" -> ts <= from
            ">"  -> ts > from
            ">=" -> ts >= from

            "<>" -> {
                if (to == null) false
                else ts in from..to
            }

            "!<>" -> {
                if (to == null) false
                else ts !in from..to
            }

            else -> false
        }
    }



    //helpers

    private fun showDateTimePicker(
        onDateTimeSelected: (displayText: String, millis: Long) -> Unit
    ) {
        val calendar = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)

                val timePicker = TimePickerDialog(
                    requireContext(),
                    { _, hour, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)

                        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        onDateTimeSelected(fmt.format(calendar.time), calendar.timeInMillis)

                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                )

                timePicker.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.show()
    }


}
