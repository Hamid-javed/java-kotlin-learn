package com.rameez.hel.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.rameez.hel.R
import com.rameez.hel.SharedPref
import com.rameez.hel.adapter.CarouselAdapter
import com.rameez.hel.data.model.WIPModel
import com.rameez.hel.databinding.FragmentCarouselBinding
import com.rameez.hel.viewmodel.SharedViewModel
import com.rameez.hel.viewmodel.WIPViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

class CarouselFragment : Fragment() {

    private var _binding: FragmentCarouselBinding? = null
    private val mBinding get() = _binding!!
    private val carouselAdapter = CarouselAdapter()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val wipViewModel: WIPViewModel by activityViewModels()
    private var currentPosition: Int = 0
    private val sessionCountedIds = HashSet<Int>()
    private var previousPosition = 0
    private var isAdded = false
    private val handler = Handler(Looper.getMainLooper())
    private var shuffledList = listOf<WIPModel>()
    private var isFirstTime = true
    private val snapHelper = PagerSnapHelper()
    private lateinit var textToSpeech: TextToSpeech
    private var countDownTimer: CountDownTimer? = null
    private var isTtsReady = false

    private val runnable = object : Runnable {
        override fun run() {
            val binding = _binding ?: return
            val totalItems = carouselAdapter.itemCount

            if (totalItems <= 0) return

            // #11: Stop at last card, don't loop
            if (currentPosition >= totalItems - 1) {
                sharedViewModel.isAutoScrollPaused = true
                updatePauseButtonIcon()
                return
            }

            val nextPosition = currentPosition + 1
            binding.rvList.smoothScrollToPosition(nextPosition)

            val interval = sharedViewModel.autoScrollIntervalSecs * 1000L
            handler.postDelayed(this, interval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
                isTtsReady = true

                handler.post {
                    if (_binding != null) {
                        updateSpeakerIcon()
                    }
                }

                if (sharedViewModel.isReadAloud && shuffledList.isNotEmpty()) {
                    handler.post {
                        if (_binding != null) {
                            val layoutManager =
                                mBinding.rvList.layoutManager as? LinearLayoutManager
                            val pos = layoutManager?.findFirstVisibleItemPosition() ?: 0
                            if (pos != -1 && pos < shuffledList.size) {
                                speakWIP(shuffledList[pos])
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCarouselBinding.inflate(layoutInflater, container, false)
        return mBinding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.categoryList.clear()
        sharedViewModel.tagsList.clear()
        sharedViewModel.readCount = null
        sharedViewModel.viewedCount = null
        sharedViewModel.readCount = null
        sharedViewModel.viewedOperator = null
        sharedViewModel.filteredWord = null
        sharedViewModel.filteredMeaning = null
        sharedViewModel.filteredSampleSen = null
        sharedViewModel.isMuted = false

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (wipViewModel.isGenerating.value == true) {
                        Toast.makeText(
                            requireContext(),
                            "Wait for processing",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    sharedViewModel.remainingTimeInMillis = null
                    sharedViewModel.isTimerRunning = false
                    sharedViewModel.isAutoScrollPaused = false
                    sharedViewModel.leftSwipedItemList.clear()
                    sharedViewModel.itemPos = null
                    sharedViewModel.itemId = null
                    findNavController().navigateUp()
                }
            }
        )

        if (!sharedViewModel.isTimerRunning) {
            startTimer()
        }
        updateSpeakerIcon()

        mBinding.apply {

            ivSpeaker.setOnClickListener {
                if (!isTtsReady) return@setOnClickListener
                sharedViewModel.isMuted = !sharedViewModel.isMuted
                updateSpeakerIcon()

                if (!sharedViewModel.isMuted) {
                    val lm = rvList.layoutManager as? LinearLayoutManager
                    val pos = lm?.findFirstVisibleItemPosition() ?: 0
                    if (pos in shuffledList.indices) {
                        speakWIP(shuffledList[pos])
                    }
                } else {
                    textToSpeech.stop()
                }
            }

            ivCancel.setOnClickListener {
                sharedViewModel.isTimerRunning = false
                sharedViewModel.remainingTimeInMillis = null
                sharedViewModel.isAutoScrollPaused = false
                sharedViewModel.leftSwipedItemList.clear()
                sharedViewModel.itemPos = null
                sharedViewModel.itemId = null
                findNavController().navigateUp()
            }

            rvList.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

            (rvList.itemAnimator as? DefaultItemAnimator)?.supportsChangeAnimations = false

            snapHelper.attachToRecyclerView(null)
            snapHelper.attachToRecyclerView(rvList)

            rvList.adapter = carouselAdapter

            cbSelectAll.setOnClickListener {
                carouselAdapter.selectAll(cbSelectAll.isChecked)
            }

            btnGeneratePara.setOnClickListener {
                val selectedIds = carouselAdapter.selectedIds.toList()
                val selectedWords = shuffledList.filter { it.id in selectedIds }.mapNotNull { it.wip }

                if (selectedWords.size < 2) {
                    Toast.makeText(requireContext(), "Select at least 2 WIPs", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val savedSources = SharedPref.getSources(requireContext()).filter { it.isChecked }.map { it.name }

                if (savedSources.isEmpty()) {
                    val sourceSelectionBS = SourceSelectionBottomSheetFragment.newInstance()
                    sourceSelectionBS.onConfirmListener = { selectedSources ->
                        wipViewModel.generateArticle(requireContext(), selectedWords, selectedIds, selectedSources)
                    }
                    sourceSelectionBS.show(childFragmentManager, "SourceSelectionBS")
                } else {
                    wipViewModel.generateArticle(requireContext(), selectedWords, selectedIds, savedSources)
                }
            }

            // #6: Shuffle button
            btnShuffle.setOnClickListener {
                if (shuffledList.isEmpty()) return@setOnClickListener
                shuffledList = shuffledList.shuffled()
                carouselAdapter.submitList(shuffledList) {
                    rvList.scrollToPosition(0)
                    currentPosition = 0
                    previousPosition = 0
                    updateCardPosition(0)
                }
                Toast.makeText(requireContext(), "Cards shuffled", Toast.LENGTH_SHORT).show()
            }

            // #9: Navigation buttons
            btnFirst.setOnClickListener { navigateToCard(0) }
            btnLast.setOnClickListener { navigateToCard(shuffledList.size - 1) }
            btnPrev.setOnClickListener {
                if (currentPosition > 0) navigateToCard(currentPosition - 1)
            }
            btnNext.setOnClickListener {
                if (currentPosition < shuffledList.size - 1) navigateToCard(currentPosition + 1)
            }

            // #8: Direct navigation to card number
            btnGoToCard.setOnClickListener { goToTypedCard() }
            etGoToCard.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    goToTypedCard()
                    true
                } else false
            }

            // #10: Pause button for auto scroll
            btnPauseAutoScroll.setOnClickListener {
                sharedViewModel.isAutoScrollPaused = !sharedViewModel.isAutoScrollPaused
                if (sharedViewModel.isAutoScrollPaused) {
                    handler.removeCallbacks(runnable)
                } else {
                    handler.postDelayed(runnable, sharedViewModel.autoScrollIntervalSecs * 1000L)
                }
                updatePauseButtonIcon()
            }
        }

        carouselAdapter.onSelectionChanged = {
            val selectedCount = carouselAdapter.selectedIds.size
            val totalCount = carouselAdapter.itemCount
            mBinding.tvSelectionCount.text = "$selectedCount/$totalCount selected"
            mBinding.cbSelectAll.isChecked = selectedCount == totalCount && totalCount > 0
            mBinding.btnGeneratePara.visibility = if (selectedCount >= 2) View.VISIBLE else View.GONE
        }

        val idsList = arrayListOf<Int>()
        sharedViewModel.filteredWipsList.forEach {
            it.id?.let(idsList::add)
        }

        wipViewModel.lastUpdatedWip.observe(viewLifecycleOwner) { updatedWIP ->
            updatedWIP ?: return@observe

            val newList = carouselAdapter.currentList.map {
                if (it.id == updatedWIP.id) updatedWIP else it
            }

            shuffledList = newList
            carouselAdapter.submitList(newList)

            mBinding.tvResultCount.text = "total: ${newList.size}"
            mBinding.tvResultCount.visibility =
                if (newList.isEmpty()) View.GONE else View.VISIBLE
        }

        lifecycleScope.launch {
            val list = wipViewModel.getWIPs2() ?: emptyList()

            val finalList = if (sharedViewModel.isFilterApplied) {
                list.filter { it.id in idsList }
            } else {
                list
            }

            shuffledList = finalList.toList()
            carouselAdapter.submitList(shuffledList) {
                val posToScroll = sharedViewModel.itemPos ?: 0
                if (posToScroll in shuffledList.indices) {
                    mBinding.rvList.scrollToPosition(posToScroll)
                    currentPosition = posToScroll
                    previousPosition = posToScroll
                    updateCardPosition(posToScroll)

                    // Count initial card if enabled
                    if (sharedViewModel.updateViewCountDuringFlashcard) {
                        shuffledList.getOrNull(posToScroll)?.let { item ->
                            val id = item.id
                            if (id != null && !sessionCountedIds.contains(id)) {
                                sessionCountedIds.add(id)

                                val isParaFilterActive = sharedViewModel.articleCreatedOperator != null
                                if (!isParaFilterActive) {
                                    wipViewModel.incrementDisplayCount(id)

                                    val currentViews = item.displayCount ?: 0f
                                    val updatedItem = item.copy(displayCount = currentViews + 1f)

                                    val newList = shuffledList.map {
                                        if (it.id == updatedItem.id) updatedItem else it
                                    }
                                    shuffledList = newList
                                    carouselAdapter.submitList(newList)
                                }
                            }
                        }
                    }

                    mBinding.rvList.post {
                        if (sharedViewModel.isReadAloud && !sharedViewModel.isMuted) {
                            speakWIP(shuffledList[posToScroll])
                        }
                    }
                }

                mBinding.tvSelectionCount.text = "0/${carouselAdapter.itemCount} selected"
            }

            if (shuffledList.isEmpty()) {
                mBinding.rvList.visibility = View.GONE
                mBinding.tvNoResults.visibility = View.VISIBLE
                mBinding.tvNoResults.text = "No matching results found"
                mBinding.llNavigation.visibility = View.GONE
            } else {
                mBinding.rvList.visibility = View.VISIBLE
                mBinding.tvNoResults.visibility = View.GONE
                mBinding.llNavigation.visibility = View.VISIBLE
            }

            // Show pause button if auto-scroll is enabled
            if (sharedViewModel.isAutoScrollEnabled) {
                mBinding.btnPauseAutoScroll.visibility = View.VISIBLE
                updatePauseButtonIcon()
            }

            updateSpeakerIcon()
            restoreResultCount()
        }

        wipViewModel.isGenerating.observe(viewLifecycleOwner) { generating ->
            carouselAdapter.isGenerating = generating

            if (generating) {
                textToSpeech.stop()
                mBinding.generationBarrier.visibility = View.VISIBLE
                mBinding.btnGeneratePara.text = "Generating article..."
                mBinding.btnGeneratePara.isEnabled = false
                handler.removeCallbacks(runnable)
                countDownTimer?.cancel()
            } else {
                mBinding.generationBarrier.visibility = View.GONE
                mBinding.btnGeneratePara.text = "Generate Para"
                mBinding.btnGeneratePara.isEnabled = true

                if (wipViewModel.generatedArticle.value != null) {
                    carouselAdapter.clearSelection()
                    mBinding.cbSelectAll.isChecked = false
                    wipViewModel.clearGeneratedArticle()
                    Toast.makeText(requireContext(), "Article generated and saved. You can also view it from the main list.", Toast.LENGTH_LONG).show()
                }

                if (sharedViewModel.isAutoScrollEnabled && !sharedViewModel.isAutoScrollPaused) {
                    handler.postDelayed(
                        runnable,
                        sharedViewModel.autoScrollIntervalSecs * 1000L
                    )
                }
                sharedViewModel.remainingTimeInMillis?.let {
                    if (it > 0) createTimer(it)
                }
            }
        }

        wipViewModel.generatedArticle.observe(viewLifecycleOwner) { article ->
            article ?: return@observe
            lifecycleScope.launch {
                val words = fetchWordsByIds(article.wipIds)
                showArticleBottomSheet(article.content, article.createdAt, article.wipIds, words)
            }
        }

        carouselAdapter.onItemClick = { id, pos ->
            sharedViewModel.itemPos = pos
            sharedViewModel.itemId = id
            findNavController().navigate(
                R.id.WIPDetailFragment,
                Bundle().apply { putInt("wip_id", id) }
            )
        }

        mBinding.rvList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val lm = recyclerView.layoutManager as LinearLayoutManager

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val snapView = snapHelper.findSnapView(lm)
                    val snappedPos = snapView?.let { lm.getPosition(it) } ?: RecyclerView.NO_POSITION

                    if (snappedPos != RecyclerView.NO_POSITION) {
                        currentPosition = snappedPos
                        updateCardPosition(snappedPos)
                        val item = shuffledList.getOrNull(snappedPos)
                        val id = item?.id

                        // TTS Logic
                        if (sharedViewModel.isReadAloud && !sharedViewModel.isMuted &&
                            snappedPos != previousPosition && snappedPos < shuffledList.size) {
                            speakWIP(shuffledList[snappedPos])
                        }

                        // #13: Only update counts if checkbox is enabled
                        if (sharedViewModel.updateViewCountDuringFlashcard) {
                            if (id != null && !sessionCountedIds.contains(id)) {
                                sessionCountedIds.add(id)

                                val isParaFilterActive = sharedViewModel.articleCreatedOperator != null
                                if (!isParaFilterActive) {
                                    wipViewModel.incrementDisplayCount(id)

                                    val currentViews = item.displayCount ?: 0f
                                    val updatedItem = item.copy(displayCount = currentViews + 1f)

                                    val newList = shuffledList.map {
                                        if (it.id == updatedItem.id) updatedItem else it
                                    }
                                    shuffledList = newList
                                    carouselAdapter.submitList(newList)
                                }
                            }
                        }

                        previousPosition = snappedPos
                    }

                    if (sharedViewModel.isAutoScrollEnabled && !sharedViewModel.isAutoScrollPaused) {
                        handler.removeCallbacks(runnable)
                        handler.postDelayed(runnable, sharedViewModel.autoScrollIntervalSecs * 1000L)
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    handler.removeCallbacks(runnable)
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dx != 0) {
                    val lm = recyclerView.layoutManager as LinearLayoutManager
                    val lastPos = lm.findLastVisibleItemPosition()
                    if (lastPos != RecyclerView.NO_POSITION && lastPos < carouselAdapter.itemCount) {
                        val item = carouselAdapter.getWIPItem(lastPos)
                        if (item !in sharedViewModel.leftSwipedItemList) {
                            sharedViewModel.leftSwipedItemList.add(item)
                        }
                    }
                }
            }
        })

        mBinding.rvList.post {
            if (sharedViewModel.isReadAloud &&
                !sharedViewModel.isMuted &&
                shuffledList.isNotEmpty()
            ) {
                val lm = mBinding.rvList.layoutManager as? LinearLayoutManager
                val pos = lm?.findFirstVisibleItemPosition().let {
                    if (it == null || it == -1) 0 else it
                }
                if (pos < shuffledList.size) {
                    speakWIP(shuffledList[pos])
                }
            }
        }
    }

    // #7: Update card position display
    @SuppressLint("SetTextI18n")
    private fun updateCardPosition(position: Int) {
        val total = shuffledList.size
        if (total > 0) {
            mBinding.tvCardPosition.text = "Card ${position + 1} of $total"
            mBinding.tvCardPosition.visibility = View.VISIBLE
            mBinding.etGoToCard.hint = "${position + 1}"
        } else {
            mBinding.tvCardPosition.visibility = View.GONE
        }
    }

    // #8: Navigate to typed card number
    private fun goToTypedCard() {
        val text = mBinding.etGoToCard.text?.toString()?.trim() ?: return
        val cardNum = text.toIntOrNull() ?: return

        if (cardNum < 1 || cardNum > shuffledList.size) {
            Toast.makeText(requireContext(), "Enter 1-${shuffledList.size}", Toast.LENGTH_SHORT).show()
            return
        }
        navigateToCard(cardNum - 1)
        mBinding.etGoToCard.text?.clear()
    }

    // Navigate to specific card position
    private fun navigateToCard(position: Int) {
        if (position !in shuffledList.indices) return
        mBinding.rvList.smoothScrollToPosition(position)
    }

    // #10: Update pause button icon
    private fun updatePauseButtonIcon() {
        if (sharedViewModel.isAutoScrollPaused) {
            mBinding.btnPauseAutoScroll.setImageResource(android.R.drawable.ic_media_play)
            mBinding.btnPauseAutoScroll.contentDescription = "Resume auto-scroll"
        } else {
            mBinding.btnPauseAutoScroll.setImageResource(android.R.drawable.ic_media_pause)
            mBinding.btnPauseAutoScroll.contentDescription = "Pause auto-scroll"
        }
    }

    private fun speakWIP(wip: WIPModel) {
        if (sharedViewModel.isMuted ||
            !sharedViewModel.isReadAloud ||
            wipViewModel.isGenerating.value == true
        ) {
            textToSpeech.stop()
            return
        }

        val options = sharedViewModel.ttsOptions
        var first = true
        var hasSomethingToSay = false

        options.forEach { option ->
            val textToRead = when (option) {
                "Word" -> wip.wip
                "Meaning" -> wip.meaning
                "Sentence" -> stripSourceForTts(wip.sampleSentence)
                else -> null
            }

            if (!textToRead.isNullOrBlank()) {
                hasSomethingToSay = true
                val queueMode = if (first) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                textToSpeech.speak(textToRead, queueMode, null, option)
                first = false
            }
        }

        if (!hasSomethingToSay) {
            textToSpeech.stop()
        }
    }

    private fun stripSourceForTts(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return text
            .replace(Regex("Source:\\s*https?://\\S+", RegexOption.IGNORE_CASE), "")
            .replace("Go To Source", "", ignoreCase = true)
            .trim()
    }

    private fun restoreResultCount() {
        val count = carouselAdapter.currentList.size
        mBinding.tvResultCount.text = "total: $count"
        mBinding.tvResultCount.visibility =
            if (count == 0) View.GONE else View.VISIBLE
    }

    private fun startTimer() {
        if (sharedViewModel.isAutoScrollEnabled && !sharedViewModel.isAutoScrollPaused) {
            handler.post(runnable)
        }

        val timeToStart = sharedViewModel.remainingTimeInMillis ?: run {
            val hours = sharedViewModel.selectedHours?.toLong() ?: 0L
            val mins = sharedViewModel.selectedMins?.toLong() ?: 0L
            val secs = sharedViewModel.selectedSecs?.toLong() ?: 0L

            if (hours == 0L && mins == 0L && secs == 0L) return@run null

            TimeUnit.HOURS.toMillis(hours) +
                    TimeUnit.MINUTES.toMillis(mins) +
                    TimeUnit.SECONDS.toMillis(secs)
        }

        if (timeToStart != null && timeToStart > 0) {
            mBinding.tvTimer.visibility = View.VISIBLE
            createTimer(timeToStart)
        } else {
            mBinding.tvTimer.visibility = View.INVISIBLE
        }
    }

    private fun createTimer(millis: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(millis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                sharedViewModel.remainingTimeInMillis = millisUntilFinished

                val binding = _binding ?: return
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                val mins = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val secs = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                binding.tvTimer.text = String.format("%02d:%02d:%02d", hours, mins, secs)
            }

            override fun onFinish() {
                val binding = _binding ?: return
                sharedViewModel.remainingTimeInMillis = null
                sharedViewModel.isTimerRunning = false
                binding.tvTimer.text = "00:00:00"
                findNavController().navigateUp()
            }
        }.start()
        sharedViewModel.isTimerRunning = true
    }

    private fun updateSpeakerIcon() {
        val isReadAloudMaster = sharedViewModel.isReadAloud
        val isMutedLocal = sharedViewModel.isMuted
        val listSize = shuffledList.size

        if (!isTtsReady && isReadAloudMaster && listSize > 0) {
            mBinding.ivSpeaker.visibility = View.GONE
            mBinding.pbSpeakerLoading.visibility = View.VISIBLE
            return
        } else {
            mBinding.pbSpeakerLoading.visibility = View.GONE
            mBinding.ivSpeaker.visibility = if (isReadAloudMaster && listSize > 0) View.VISIBLE else View.GONE
        }

        if (!isReadAloudMaster || listSize == 0) {
            mBinding.ivSpeaker.visibility = View.GONE
            textToSpeech.stop()
            return
        }

        mBinding.ivSpeaker.isEnabled = true

        if (!isMutedLocal) {
            mBinding.ivSpeaker.setImageResource(R.drawable.ic_speaker_on)
        } else {
            mBinding.ivSpeaker.setImageResource(R.drawable.ic_speaker_off)
            textToSpeech.stop()
        }

        mBinding.ivSpeaker.setColorFilter(
            ContextCompat.getColor(requireContext(), android.R.color.black),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
    }

    private suspend fun fetchWordsByIds(ids: List<Int>): List<String> {
        val words = mutableListOf<String>()
        ids.forEach { id ->
            wipViewModel.getWIPByIdSync(id)?.wip?.let { words.add(it) }
        }
        return words
    }

    private fun showArticleBottomSheet(content: String, timestamp: Long, wipIds: List<Int>, usedWords: List<String>) {
        val bottomSheet = ArticleBottomSheetFragment.newInstance(content, timestamp, wipIds, usedWords)
        bottomSheet.show(childFragmentManager, "ArticleBottomSheet")
    }

    override fun onDestroyView() {
        handler.removeCallbacks(runnable)
        countDownTimer?.cancel()
        countDownTimer = null
        super.onDestroyView()
        textToSpeech.stop()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.shutdown()
    }

    override fun onResume() {
        super.onResume()
        restoreResultCount()
    }
}
