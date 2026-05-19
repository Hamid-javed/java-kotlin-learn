package com.rameez.hel.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rameez.hel.R
import com.rameez.hel.SharedPref
import com.rameez.hel.viewmodel.WIPViewModel
import java.util.*

class ArticleBottomSheetFragment : BottomSheetDialogFragment() {

    private var content: String? = null
    private var createdAt: Long = 0L
    private var wipIds: ArrayList<Int> = arrayListOf()
    private var usedWords: ArrayList<String> = arrayListOf()

    private lateinit var textToSpeech: TextToSpeech
    private var isSpeaking = false
    private val wipViewModel: WIPViewModel by activityViewModels()

    private var isShowingInfo = false
    private val wordCheckBoxes = mutableListOf<CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        content = arguments?.getString(ARG_CONTENT)
        createdAt = arguments?.getLong(ARG_TIMESTAMP) ?: 0L
        wipIds = arguments?.getIntegerArrayList(ARG_WIP_IDS) ?: arrayListOf()
        usedWords = arguments?.getStringArrayList(ARG_USED_WORDS) ?: arrayListOf()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_article, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tvContent = view.findViewById<TextView>(R.id.tvArticleContent)
        val tvTimestamp = view.findViewById<TextView>(R.id.tvTimestamp)
        val ivSpeaker = view.findViewById<ImageView>(R.id.ivSpeaker)
        val ivInfo = view.findViewById<ImageView>(R.id.ivInfo)
        val layoutContent = view.findViewById<LinearLayout>(R.id.layoutContent)
        val layoutInfo = view.findViewById<LinearLayout>(R.id.layoutInfo)
        val layoutUsedWords = view.findViewById<LinearLayout>(R.id.layoutUsedWords)
        val btnRegenerate = view.findViewById<Button>(R.id.btnRegenerate)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val cbSelectAllWips = view.findViewById<CheckBox>(R.id.cbSelectAllWips)

        // Set consistent tint for Select All checkbox
        cbSelectAllWips.buttonTintList = ColorStateList.valueOf(Color.BLACK)

        // Clean and set content - we keep the Markdown cleaning but let the URL flow naturally
        val cleanedContent = cleanMarkdown(content ?: "")

        // Format and set the text
        setArticleTextWithBoldWips(tvContent, cleanedContent, usedWords)

        // movementMethod is required for clickable spans
        tvContent.movementMethod = LinkMovementMethod.getInstance()

        // Populate used words with checkboxes
        layoutUsedWords.removeAllViews()
        wordCheckBoxes.clear()

        var isInternalChange = false

        fun updateSelectAllState() {
            if (isInternalChange) return
            isInternalChange = true
            cbSelectAllWips.isChecked = wordCheckBoxes.all { it.isChecked }
            isInternalChange = false
        }

        cbSelectAllWips.setOnCheckedChangeListener { _, isChecked ->
            if (isInternalChange) return@setOnCheckedChangeListener
            isInternalChange = true
            wordCheckBoxes.forEach { it.isChecked = isChecked }
            isInternalChange = false
        }

        usedWords.forEach { word ->
            val checkBox = CheckBox(requireContext()).apply {
                text = word
                isChecked = true
                setTextColor(Color.BLACK)
                buttonTintList = ColorStateList.valueOf(Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnCheckedChangeListener { _, _ ->
                    updateSelectAllState()
                }
            }
            wordCheckBoxes.add(checkBox)
            layoutUsedWords.addView(checkBox)
        }

        updateSelectAllState()

        val dateFormatter = java.text.DateFormat.getDateTimeInstance()
        tvTimestamp.text = "Created at: " + (if (createdAt != 0L) dateFormatter.format(Date(createdAt)) else "-")

        initTTS(ivSpeaker)

        ivSpeaker.setOnClickListener {
            if (isSpeaking) {
                stopTTS(ivSpeaker)
            } else {
                startTTS(ivSpeaker)
            }
        }

        ivInfo.setOnClickListener {
            isShowingInfo = !isShowingInfo
            if (isShowingInfo) {
                layoutContent.visibility = View.GONE
                layoutInfo.visibility = View.VISIBLE
                tvTitle.text = "Words Used"
                ivInfo.setImageResource(R.drawable.baseline_article_24)
            } else {
                layoutContent.visibility = View.VISIBLE
                layoutInfo.visibility = View.GONE
                tvTitle.text = "Generated Article"
                ivInfo.setImageResource(R.drawable.baseline_info_24)
            }
        }

        btnRegenerate.setOnClickListener {
            val selectedWords = wordCheckBoxes.filter { it.isChecked }.map { it.text.toString() }
            if (selectedWords.size < 2) {
                Toast.makeText(requireContext(), "Please select at least 2 WIPs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val savedSources = SharedPref.getSources(requireContext()).filter { it.isChecked }.map { it.name }

            if (savedSources.isEmpty()) {
                val sourceSelectionBS = SourceSelectionBottomSheetFragment.newInstance()
                sourceSelectionBS.onConfirmListener = { selectedSources ->
                    regenerateArticle(selectedWords, selectedSources)
                    dismiss()
                }
                sourceSelectionBS.show(childFragmentManager, "SourceSelectionBS")
            } else {
                regenerateArticle(selectedWords, savedSources)
                dismiss()
            }
        }
    }

    private fun regenerateArticle(selectedWords: List<String>, selectedSources: List<String>) {
        val filteredWipIds = mutableListOf<Int>()
        usedWords.forEachIndexed { index, word ->
            if (wordCheckBoxes[index].isChecked) {
                if (index < wipIds.size) {
                    filteredWipIds.add(wipIds[index])
                }
            }
        }
        wipViewModel.generateArticle(requireContext(), selectedWords, filteredWipIds, selectedSources)
    }

    private fun cleanMarkdown(text: String): String {
        return text.replace(Regex("[*#_>]+"), "").trim()
    }

    /**
     * Sets the article text into the textView and applies bold spans for any WIP words/phrases.
     * It also detects any URLs in the text and makes them clickable without stripping them.
     */
    private fun setArticleTextWithBoldWips(
        textView: TextView,
        text: String,
        wips: List<String>
    ) {
        if (text.isBlank()) {
            textView.text = ""
            return
        }

        val spannable = SpannableStringBuilder(text)

        // 1. Apply bold spans for each WIP
        for (raw in wips) {
            val w = raw.trim()
            if (w.isEmpty()) continue

            val pattern = try {
                Regex("\\b${Regex.escape(w)}\\b", RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                Regex(Regex.escape(w), RegexOption.IGNORE_CASE)
            }

            val matches = pattern.findAll(text)
            for (m in matches) {
                val start = m.range.first
                val end = m.range.last + 1
                if (start >= 0 && end <= spannable.length) {
                    spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }

        // 2. Detect URLs and make them clickable in place
        val matcher = Patterns.WEB_URL.matcher(text)
        while (matcher.find()) {
            val url = matcher.group()
            val start = matcher.start()
            val end = matcher.end()

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    try {
                        val uriString = if (url.contains("://")) url else "http://$url"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                        widget.context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(widget.context, "Could not open link", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = true
                    ds.color = Color.BLUE
                }
            }
            spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        textView.text = spannable
    }

    private fun initTTS(ivSpeaker: ImageView) {
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
                textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        activity?.runOnUiThread {
                            isSpeaking = true
                            ivSpeaker.setImageResource(R.drawable.ic_speaker_off)
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        activity?.runOnUiThread {
                            isSpeaking = false
                            ivSpeaker.setImageResource(R.drawable.ic_speaker_on)
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        activity?.runOnUiThread {
                            isSpeaking = false
                            ivSpeaker.setImageResource(R.drawable.ic_speaker_on)
                        }
                    }
                })
            } else {
                Log.e("TTS", "Initialization Failed")
            }
        }
    }

    private fun startTTS(ivSpeaker: ImageView) {
        val fullText = content ?: return
        if (fullText.isBlank()) {
            Toast.makeText(requireContext(), "No content to read", Toast.LENGTH_SHORT).show()
            return
        }

        // Read the cleaned article only
        val spokenText = cleanMarkdown(fullText).trim()

        textToSpeech.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "ArticleID")
    }

    private fun stopTTS(ivSpeaker: ImageView) {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            isSpeaking = false
            ivSpeaker.setImageResource(R.drawable.ic_speaker_on)
        }
    }

    override fun onDestroyView() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroyView()
    }

    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialogTheme
    }

    companion object {
        private const val ARG_CONTENT = "arg_content"
        private const val ARG_TIMESTAMP = "arg_timestamp"
        private const val ARG_WIP_IDS = "arg_wip_ids"
        private const val ARG_USED_WORDS = "arg_used_words"

        fun newInstance(
            content: String,
            timestamp: Long,
            wipIds: List<Int>,
            usedWords: List<String>
        ): ArticleBottomSheetFragment {
            val fragment = ArticleBottomSheetFragment()
            val args = Bundle()
            args.putString(ARG_CONTENT, content)
            args.putLong(ARG_TIMESTAMP, timestamp)
            args.putIntegerArrayList(ARG_WIP_IDS, ArrayList(wipIds))
            args.putStringArrayList(ARG_USED_WORDS, ArrayList(usedWords))
            fragment.arguments = args
            return fragment
        }
    }
}
