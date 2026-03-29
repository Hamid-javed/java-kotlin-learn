package com.rameez.hel.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rameez.hel.R
import com.rameez.hel.SharedPref
import com.rameez.hel.databinding.FragmentWIPDetailBinding
import com.rameez.hel.viewmodel.SharedViewModel
import com.rameez.hel.viewmodel.WIPViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale


class WIPDetailFragment : Fragment() {

    private lateinit var mBinding: FragmentWIPDetailBinding
    private val wipViewModel: WIPViewModel by activityViewModels()
    private lateinit var textToSpeech:  TextToSpeech
    private var word: String = ""
    private var id: Int = 0
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
        mBinding.txtSampleSentence.movementMethod = LinkMovementMethod.getInstance()



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


            mBinding.apply {
                word = wip.wip.orEmpty()
                txtWord.text = wip.wip
                txtMeaning.text = wip.meaning
                setSentenceWithCitation(
                    mBinding.txtSampleSentence,
                    wip.sampleSentence.orEmpty()
                )

                txtCategory.text = wip.category
                tvTags.text = wip.customTag?.joinToString(", ")
                txtReadCount.text = "${wip.readCount?.toInt() ?: 0} times"
                txtViewCount.text = "${wip.displayCount?.toInt() ?: 0} times"




                    val dateFormatter = java.text.DateFormat.getDateTimeInstance()
                    tvCreatedAt.text = "Created at: " + (if (wip.createdAt > 0L) dateFormatter.format(java.util.Date(wip.createdAt)) else "-")
                    tvModifiedAt.text = "Modified at: " + (if (wip.modifiedAt > 0L) dateFormatter.format(java.util.Date(wip.modifiedAt)) else "-")
                    tvLastViewedAt.text = "Last viewed: " + (if (wip.displayCountUpdatedAt > 0L) dateFormatter.format(java.util.Date(wip.displayCountUpdatedAt)) else "-")
                    tvLastEncounteredAt.text = "Last encountered: " + (if (wip.readCountUpdatedAt > 0L) dateFormatter.format(java.util.Date(wip.readCountUpdatedAt)) else "-")
                tvFirstViewedAt.text = "First viewed: " +
                        if (wip.firstViewedAt > 0L) dateFormatter.format(Date(wip.firstViewedAt)) else "-"
                    tvFirstEncounteredAt.text = "First encountered: " +
                            if (wip.firstEncounteredAt > 0L) dateFormatter.format(Date(wip.firstEncounteredAt)) else "-"
                
                tvParaCreatedAt.text = if (wip.lastParaCreatedAt > 0L) dateFormatter.format(Date(wip.lastParaCreatedAt)) else "-"
                }
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

        mBinding.btnEdit.setOnClickListener {
            val bundle = Bundle()
            bundle.putInt("wip_id", id)
            findNavController().navigate(R.id.WIPEditFragment, bundle)
        }

        mBinding.tvDeleteWIP.setOnClickListener {
            wipViewModel.deleteWIPById(id)
            lifecycleScope.launch {
//                delay(500)
                sharedViewModel.isWIPDeleted = true
                findNavController().navigateUp()
            }
        }

        mBinding.dEncountered.setOnClickListener {
            wipViewModel.resetEncountered(id)
            mBinding.txtReadCount.text = "0"
        }

        mBinding.dViewed.setOnClickListener {
            wipViewModel.resetViewed(id)
            mBinding.txtViewCount.text = "0"
        }

        mBinding.dParaCreatedAt.setOnClickListener {
            wipViewModel.resetParaCreatedAt(id)
            mBinding.tvParaCreatedAt.text = "-"
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


    private fun setSentenceWithCitation(textView: TextView, sentence: String) {
        if (sentence.isBlank()) {
            textView.text = ""
            return
        }

        // 1. First, find a valid source URL BEFORE stripping everything out.
        // We look for any URL that isn't a proxy.
        val matcher = Patterns.WEB_URL.matcher(sentence)
        var finalSourceUrl: String? = null
        while (matcher.find()) {
            val url = matcher.group()
            if (!url.contains("vertexsearch.cloud.google.com", ignoreCase = true) &&
                !url.contains("vertexaisearch.cloud.google.com", ignoreCase = true) &&
                !url.contains("google.com/search", ignoreCase = true)) {
                finalSourceUrl = url
                break
            }
        }

        // 2. Clean the text for display.
        // We remove HTML comments, any Source: label lines, and finally ALL raw URLs.
        var displayText = sentence
            .replace(Regex("<!--[\\s\\S]*?-->"), "") // Remove comments
            .replace(Regex("Source:\\s*https?://\\S+", RegexOption.IGNORE_CASE), "") // Remove specific Source lines
        
        // Use a matcher to remove ALL URLs found by the pattern
        val urlMatcher = Patterns.WEB_URL.matcher(displayText)
        displayText = urlMatcher.replaceAll("").trim()

        // Remove any leftover JSON-like brackets at the end if they exist
        displayText = displayText.replace(Regex("\\{.*\\}$"), "").trim()

        val spannable = SpannableStringBuilder(displayText)

        // 3. Append "Go To Source" link if we captured a valid URL in step 1.
        if (!finalSourceUrl.isNullOrBlank()) {
            val linkLabel = "Go To Source"
            if (displayText.isNotEmpty()) {
                spannable.append("\n\n")
            }
            
            val startOfLink = spannable.length
            spannable.append(linkLabel)

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    try {
                        val normalizedUrl = if (finalSourceUrl!!.contains("://")) finalSourceUrl!! else "http://$finalSourceUrl"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl))
                        widget.context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(widget.context, "Could not open link", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = Color.BLUE
                    ds.isUnderlineText = true
                }
            }
            spannable.setSpan(clickableSpan, startOfLink, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        textView.text = spannable
    }


    override fun onDestroyView() {
        super.onDestroyView()
        textToSpeech.shutdown()
    }

}
