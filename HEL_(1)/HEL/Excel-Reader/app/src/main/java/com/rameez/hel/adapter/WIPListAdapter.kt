package com.rameez.hel.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rameez.hel.data.model.WIPModel
import com.rameez.hel.databinding.WipListItemLayoutBinding

class WIPListAdapter : ListAdapter<WIPModel, RecyclerView.ViewHolder>(WIPDiffUtil()) {

    var onWipItemClicked: ((Int, Float, Int) -> Unit)? = null
    var isSelectionMode = false
    val selectedItems = mutableSetOf<WIPModel>()
    var onSelectionChanged: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null

    class WIPDiffUtil : androidx.recyclerview.widget.DiffUtil.ItemCallback<WIPModel>() {
        override fun areItemsTheSame(oldItem: WIPModel, newItem: WIPModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WIPModel, newItem: WIPModel): Boolean {
            return oldItem == newItem
        }
    }

    fun selectAll(isSelected: Boolean) {
        if (isSelected) {
            selectedItems.addAll(currentList)
        } else {
            selectedItems.clear()
        }
        notifyDataSetChanged()
        onSelectionChanged?.invoke()
    }

    fun clearSelection() {
        isSelectionMode = false
        selectedItems.clear()
        notifyDataSetChanged()
        onSelectionChanged?.invoke()
    }

    inner class WIPListViewHolder(private val binding: WipListItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(wipItem: WIPModel) {
            binding.apply {
                txtWordType.text = wipItem.category
                txtWord.text = wipItem.wip
                txtWordMeaning.text = wipItem.meaning
                if (wipItem.displayCount == null) {
                    txtCount.text = "Viewed 0 times"
                } else {
                    txtCount.text = "Viewed " + wipItem.displayCount!!.toInt().toString() + " times"
                }

                if (wipItem.readCount == null) {
                    txtEncountered.text = "Encountered 0 times"
                } else {
                    txtEncountered.text =
                        "Encountered " + wipItem.readCount!!.toInt().toString() + " times"
                }

                cbSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
                cbSelect.isChecked = selectedItems.contains(wipItem)

                cbSelect.setOnClickListener {
                    toggleSelection(wipItem)
                }

                wipCv.setOnClickListener {
                    if (isSelectionMode) {
                        toggleSelection(wipItem)
                    } else {
                        wipItem.id?.let { id ->
                            wipItem.displayCount?.let { count ->
                                onWipItemClicked?.invoke(
                                    id,
                                    count,
                                    adapterPosition
                                )
                            }
                        }
                    }
                }

                wipCv.setOnLongClickListener {
                    if (!isSelectionMode) {
                        isSelectionMode = true
                        selectedItems.add(wipItem)
                        onLongPress?.invoke()
                        notifyDataSetChanged()
                        onSelectionChanged?.invoke()
                    }
                    true
                }
            }
        }

        private fun toggleSelection(wipItem: WIPModel) {
            if (selectedItems.contains(wipItem)) {
                selectedItems.remove(wipItem)
            } else {
                selectedItems.add(wipItem)
            }
            notifyItemChanged(adapterPosition)
            onSelectionChanged?.invoke()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return WIPListViewHolder(
            WipListItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        (holder as WIPListViewHolder).bind(item)
    }
}