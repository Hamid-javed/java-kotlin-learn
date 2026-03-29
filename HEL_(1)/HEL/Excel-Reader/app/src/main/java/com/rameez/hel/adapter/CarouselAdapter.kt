package com.rameez.hel.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.Selection
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rameez.hel.data.model.WIPModel
import com.rameez.hel.databinding.CarouselItemBinding
import com.rameez.hel.viewmodel.WIPViewModel


class CarouselAdapter :
    ListAdapter<WIPModel, RecyclerView.ViewHolder>(CarouselDiffUtil()) {

    var onItemClick: ((Int, Int) -> Unit)? = null
    var isGenerating: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    val selectedIds = mutableSetOf<Int>()
    var onSelectionChanged: (() -> Unit)? = null

    class CarouselDiffUtil : androidx.recyclerview.widget.DiffUtil.ItemCallback<WIPModel>() {
        override fun areItemsTheSame(oldItem: WIPModel, newItem: WIPModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WIPModel, newItem: WIPModel): Boolean {
            return oldItem == newItem
        }
    }

    fun selectAll(isSelected: Boolean) {
        if (isSelected) {
            currentList.forEach { it.id?.let(selectedIds::add) }
        } else {
            selectedIds.clear()
        }
        notifyDataSetChanged()
        onSelectionChanged?.invoke()
    }

    fun clearSelection() {
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged?.invoke()
    }

    inner class CarouselViewHolder(private val binding: CarouselItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

            @SuppressLint("SetTextI18n")
            fun bind(item: WIPModel) {
                binding.apply {
                    val navigateToDetail = {
                        val pos = adapterPosition
                        if (pos != RecyclerView.NO_POSITION) {
                            item.id?.let { id -> onItemClick?.invoke(id, pos) }
                        }
                    }

                    // Set click listener on major containers to ensure navigation works
                    wipCv.setOnClickListener { navigateToDetail() }
                    innerLayout.setOnClickListener { navigateToDetail() }
                    contentLayout.setOnClickListener { navigateToDetail() }

                    // Custom LinkMovementMethod for txtSampleSentence
                    txtSampleSentence.movementMethod = object : LinkMovementMethod() {
                        override fun onTouchEvent(widget: TextView, buffer: android.text.Spannable, event: MotionEvent): Boolean {
                            val action = event.action
                            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                                var x = event.x.toInt()
                                var y = event.y.toInt()

                                x -= widget.totalPaddingLeft
                                y -= widget.totalPaddingTop
                                x += widget.scrollX
                                y += widget.scrollY

                                val layout = widget.layout
                                val line = layout.getLineForVertical(y)
                                val off = layout.getOffsetForHorizontal(line, x.toFloat())
                                val link = buffer.getSpans(off, off, ClickableSpan::class.java)

                                if (link.isNotEmpty()) {
                                    if (action == MotionEvent.ACTION_UP) {
                                        link[0].onClick(widget)
                                    } else {
                                        Selection.setSelection(buffer, buffer.getSpanStart(link[0]), buffer.getSpanEnd(link[0]))
                                    }
                                    return true
                                } else {
                                    Selection.removeSelection(buffer)
                                }
                            }
                            return false // Bubble up to navigateToDetail
                        }
                    }
                    txtSampleSentence.setOnClickListener { navigateToDetail() }

                    txtWordType.text = item.category
                    txtWord.text = item.wip
                    txtWordMeaning.text = item.meaning
                    txtViewCount.text = "Viewed " + item.displayCount?.toInt().toString() + " times"
                    txtReadCount.text = "Encountered " + item.readCount?.toInt().toString() + " times"
                    setSentenceWithCitation(txtSampleSentence, item.sampleSentence)

                    txtTags.text = item.customTag?.joinToString(", ")

                    tvGenerateSentence.apply {
                        isEnabled = !isGenerating
                        text = if (isGenerating) "Generating…" else "Generate AI sentence"
                    }

                    tvGenerateSentence.setOnClickListener {
                        val wipWord = item.wip
                        val wipId = item.id
                        if (!wipWord.isNullOrBlank() && wipId != null) {
                            (binding.root.context as? FragmentActivity)?.let { activity ->
                                val wipViewModel: WIPViewModel by activity.viewModels()
                                wipViewModel.generateAndSaveSentence(activity, wipId, wipWord)
                            }
                        } else {
                            Toast.makeText(binding.root.context, "Word cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }

                    cbSelect.isChecked = item.id?.let { selectedIds.contains(it) } == true
                    cbSelect.setOnClickListener {
                        item.id?.let { id ->
                            if (selectedIds.contains(id)) {
                                selectedIds.remove(id)
                            } else {
                                selectedIds.add(id)
                            }
                        }
                        onSelectionChanged?.invoke()
                    }
                }
            }
        }


    private fun setSentenceWithCitation(textView: TextView, sentence: String?) {
        if (sentence.isNullOrBlank()) {
            textView.text = ""
            return
        }

        val regex = Regex("Source:\\s*(https?://\\S+)", RegexOption.IGNORE_CASE)
        val match = regex.find(sentence)

        if (match == null) {
            textView.text = sentence
            return
        }

        val url = match.groupValues[1]
        val cleanSentence = sentence.replace(match.value, "").trim()
        val displayText = "Go To Source"

        val fullText = if (cleanSentence.isNotEmpty()) {
            "$cleanSentence\n\n$displayText"
        } else {
            displayText
        }

        val spannable = SpannableString(fullText)

        val start = fullText.indexOf(displayText)
        val end = start + displayText.length

        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                widget.context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                )
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = Color.RED
                ds.isUnderlineText = false
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textView.text = spannable
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        (holder as CarouselViewHolder).bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return CarouselViewHolder(
            CarouselItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    fun getWIPItem(position: Int): WIPModel {
        return currentList[position]
    }
}