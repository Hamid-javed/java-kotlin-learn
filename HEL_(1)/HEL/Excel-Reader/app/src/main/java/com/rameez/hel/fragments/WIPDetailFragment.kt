package com.rameez.hel.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rameez.hel.SharedPref
import com.rameez.hel.R
import com.rameez.hel.databinding.FragmentWIPDetailBinding
import com.rameez.hel.viewmodel.SharedViewModel
import com.rameez.hel.viewmodel.WIPViewModel
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale


class WIPDetailFragment : Fragment() {

    private lateinit var mBinding: FragmentWIPDetailBinding
    private val wipViewModel: WIPViewModel by activityViewModels()
    private lateinit var textToSpeech:  TextToSpeech
    private var word: String = ""
    private var id: Int = 0
    private var currentReadCount: Float = 0f
    private var currentDisplayCount: Float = 0f
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = FragmentWIPDetailBinding.inflate(layoutInflater, container, false)
        return mBinding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val intent = arguments
        id = intent?.getInt("wip_id", 0) ?: 0


        mBinding.tvGenerateSentence.setOnClickListener {
            if (word.isNotBlank()) {
                val savedSources = SharedPref.getSources(requireContext()).filter { it.isChecked }.map { it.name }
                if (savedSources.isEmpty()) {
                    val sourceSelectionBS = SourceSelectionBottomSheetFragment.newInstance()
                    sourceSelectionBS.onConfirmListener = { selectedSources ->
                        if (selectedSources.isNotEmpty()) {
                            wipViewModel.generateAndSaveSentence(requireContext(), id, word)
                        } else {
                            Toast.makeText(requireContext(), "Please select at least one source", Toast.LENGTH_SHORT).show()
                        }
                    }
                    sourceSelectionBS.show(childFragmentManager, "SourceSelectionBS")
                } else {
                    wipViewModel.generateAndSaveSentence(requireContext(), id, word)
                }
            } else {
                Toast.makeText(requireContext(), "Word cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }



        wipViewModel.isGenerating.observe(viewLifecycleOwner) { generating ->
            if (generating && ::textToSpeech.isInitialized) {
                textToSpeech.stop()
                mBinding.btnStopTTS.visibility = View.GONE
            }
            mBinding.tvGenerateSentence.apply {
                isEnabled = !generating
                text = if (generating) "Generating…" else "Generate AI sentence"
            }
            mBinding.generationBarrier.visibility = if (generating) View.VISIBLE else View.GONE
        }


        wipViewModel.getWIPById(id)?.observe(viewLifecycleOwner) { wip ->
            wip ?: return@observe
            mBinding.txtReadCount.text = "${wip.readCount?.toInt() ?: 0} times"
            mBinding.txtViewCount.text = "${wip.displayCount?.toInt() ?: 0} times"


            currentReadCount = wip.readCount ?: 0f
            currentDisplayCount = wip.displayCount ?: 0f

            mBinding.apply {
                word = wip.wip.orEmpty()
                txtWord.setText(wip.wip)
                txtMeaning.setText(wip.meaning)
                txtSampleSentence.setText(wip.sampleSentence.orEmpty())

                txtCategory.setText(wip.category)
                mBinding.llTagsContainer.removeAllViews()
                wip.customTag?.filter { t -> t.isNotBlank() }?.forEach { tag ->
                    addTagRow(tag)
                }
                txtReadCount.text = "${wip.readCount?.toInt() ?: 0} times"
                txtViewCount.text = "${wip.displayCount?.toInt() ?: 0} times"




                    val dateFormatter = java.text.DateFormat.getDateTimeInstance()
                    tvCreatedAt.text = "Created at: " + (if (wip.createdAt != 0L) dateFormatter.format(java.util.Date(wip.createdAt)) else "-")
                    tvModifiedAt.text = "Modified at: " + (if (wip.modifiedAt != 0L) dateFormatter.format(java.util.Date(wip.modifiedAt)) else "-")
                    tvLastViewedAt.text = "Last viewed: " + (if (wip.displayCountUpdatedAt != 0L) dateFormatter.format(java.util.Date(wip.displayCountUpdatedAt)) else "-")
                    tvLastEncounteredAt.text = "Last encountered: " + (if (wip.readCountUpdatedAt != 0L) dateFormatter.format(java.util.Date(wip.readCountUpdatedAt)) else "-")
                tvFirstViewedAt.text = "First viewed: " +
                        if (wip.firstViewedAt != 0L) dateFormatter.format(Date(wip.firstViewedAt)) else "-"
                    tvFirstEncounteredAt.text = "First encountered: " +
                            if (wip.firstEncounteredAt != 0L) dateFormatter.format(Date(wip.firstEncounteredAt)) else "-"

                tvParaCreatedAt.text = if (wip.lastParaCreatedAt != 0L) dateFormatter.format(Date(wip.lastParaCreatedAt)) else "-"
                }
            }



        wipViewModel.allTags?.observe(viewLifecycleOwner) { tags ->
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                tags
            )
            mBinding.etTag.setAdapter(adapter)
        }

        mBinding.etTag.setOnItemClickListener { _, _, _, _ ->
            mBinding.btnAddTag.performClick()
        }

        val viewCount = intent?.getFloat("view_count", 0f)
        if (viewCount != null) {
            if(SharedPref.isAppLaunched(requireContext())) {
//                updateViewedCount(viewCount)
            }
        }

        mBinding.imgBack.setOnClickListener {
//            updateReadCount()
            findNavController().navigateUp()
        }
        
        mBinding.btnAddTag.setOnClickListener {
            val tag = mBinding.etTag.text?.toString()?.trim()
            if (!tag.isNullOrEmpty()) {
                addTagRow(tag)
            }
            mBinding.etTag.setText("")
        }

        mBinding.btnEdit.setOnClickListener {
            val newWord = mBinding.txtWord.text.toString().trim()
            val newMeaning = mBinding.txtMeaning.text.toString().trim()
            val newSentence = mBinding.txtSampleSentence.text.toString().trim()
            val newCategory = mBinding.txtCategory.text.toString().trim()
            
            // Collect directly from dynamic row layout
            val newTags = mutableListOf<String>()
            for (i in 0 until mBinding.llTagsContainer.childCount) {
                val row = mBinding.llTagsContainer.getChildAt(i)
                val et = row.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etInlineTag)
                val tagText = et?.text?.toString()?.trim()
                if (!tagText.isNullOrEmpty()) {
                    newTags.add(tagText)
                }
            }

            if (newWord.isEmpty()) {
                Toast.makeText(requireContext(), "Word cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            wipViewModel.updateWIP(
                id,
                newCategory,
                newWord,
                newMeaning,
                newSentence,
                newTags,
                currentReadCount,
                currentDisplayCount
            )
            word = newWord
            Toast.makeText(requireContext(), "WIP saved", Toast.LENGTH_SHORT).show()
        }

        mBinding.tvDeleteWIP.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete WPI")
                .setMessage("Are you sure you want to delete this WPI?")
                .setPositiveButton("Delete") { _, _ ->
                    wipViewModel.deleteWIPById(id)
                    lifecycleScope.launch {
                        sharedViewModel.isWIPDeleted = true
                        findNavController().navigateUp()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        mBinding.btnViewedPlus.setOnClickListener {
            currentDisplayCount += 1
            wipViewModel.updateViewedCount(id, currentDisplayCount)
            mBinding.txtViewCount.text = "${currentDisplayCount.toInt()} times"
        }

        mBinding.btnViewedMinus.setOnClickListener {
            if (currentDisplayCount > 0) {
                currentDisplayCount -= 1
                wipViewModel.updateViewedCount(id, currentDisplayCount)
                mBinding.txtViewCount.text = "${currentDisplayCount.toInt()} times"
            }
        }

        mBinding.btnEncounterPlus.setOnClickListener {
            currentReadCount += 1
            wipViewModel.updateReadCount(id, currentReadCount)
            mBinding.txtReadCount.text = "${currentReadCount.toInt()} times"
        }

        mBinding.btnEncounterMinus.setOnClickListener {
            if (currentReadCount > 0) {
                currentReadCount -= 1
                wipViewModel.updateReadCount(id, currentReadCount)
                mBinding.txtReadCount.text = "${currentReadCount.toInt()} times"
            }
        }

        mBinding.dEncountered.setOnClickListener {
            wipViewModel.resetEncountered(id)
            currentReadCount = 0f
            mBinding.txtReadCount.text = "0 times"
        }

        mBinding.dViewed.setOnClickListener {
            wipViewModel.resetViewed(id)
            currentDisplayCount = 0f
            mBinding.txtViewCount.text = "0 times"
        }

        mBinding.dParaCreatedAt.setOnClickListener {
            wipViewModel.resetParaCreatedAt(id)
            mBinding.tvParaCreatedAt.text = "-"
        }

        mBinding.btnEditParaCreatedAt.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                TimePickerDialog(requireContext(), { _, hour, minute ->
                    cal.set(year, month, day, hour, minute, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    val ts = cal.timeInMillis
                    wipViewModel.setParaCreatedAt(id, ts)
                    val dateFormatter = java.text.DateFormat.getDateTimeInstance()
                    mBinding.tvParaCreatedAt.text = dateFormatter.format(Date(ts))
                }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), true).show()
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS){
                Log.d("TAG", "Initialization Success")
                textToSpeech.language = Locale.US
                textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        activity?.runOnUiThread { mBinding.btnStopTTS.visibility = View.VISIBLE }
                    }

                    override fun onDone(utteranceId: String?) {
                        activity?.runOnUiThread { mBinding.btnStopTTS.visibility = View.GONE }
                    }

                    override fun onError(utteranceId: String?) {
                        activity?.runOnUiThread { mBinding.btnStopTTS.visibility = View.GONE }
                    }
                })
            }else{
                Log.d("TAG", "Initialization Failed")
            }
        }

        mBinding.btnStopTTS.setOnClickListener {
            if (::textToSpeech.isInitialized) {
                textToSpeech.stop()
                mBinding.btnStopTTS.visibility = View.GONE
            }
        }
        mBinding.ivSpeaker.setOnClickListener {
            textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, "WordID")
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (wipViewModel.isGenerating.value == true) {
                    Toast.makeText(requireContext(), "AI is working, please wait...", Toast.LENGTH_SHORT).show()
                } else {
                    isEnabled = false // Disable this callback
                    requireActivity().onBackPressedDispatcher.onBackPressed() // Actually go back
                }
            }
        })

        mBinding.ivSpeakerSentence.setOnClickListener {

            val fullText = mBinding.txtSampleSentence.text.toString()

            // Remove the "Go To Source" part before speaking
            val spokenText = fullText
                .replace("Go To Source", "", ignoreCase = true)
                .trim()

            if (spokenText.isNotBlank()) {
                textToSpeech.speak(
                    spokenText,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "SentenceID"
                )
            } else {
                Toast.makeText(requireContext(), "No sentence to read", Toast.LENGTH_SHORT).show()
            }
        }


        mBinding.ivSpeakerMeaning.setOnClickListener {
            val meaning = mBinding.txtMeaning.text.toString()
            if (meaning.isNotBlank()) {
                // Use QUEUE_FLUSH to stop any current speech and start the meaning immediately
                textToSpeech.speak(meaning, TextToSpeech.QUEUE_FLUSH, null, "MeaningID")
            }
            else {
                Toast.makeText(requireContext(), "No meaning found to read", Toast.LENGTH_SHORT).show()
            }
        }

//        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                updateReadCount()
//                findNavController().navigateUp()
//            }
//
//        })
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

    override fun onDestroyView() {
        super.onDestroyView()
        textToSpeech.shutdown()
    }

}
