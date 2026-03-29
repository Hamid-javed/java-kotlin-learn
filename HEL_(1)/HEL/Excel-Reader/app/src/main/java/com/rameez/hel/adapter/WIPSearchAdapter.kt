package com.rameez.hel.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rameez.hel.data.model.WIPModel
import com.rameez.hel.databinding.WipSearchItemLayoutBinding

class WIPSearchAdapter : ListAdapter<WIPModel, RecyclerView.ViewHolder>(WIPDiffUtil()) {

    var onWipItemClicked: ((Int) -> Unit)? = null
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

    inner class WIPListViewHolder(private val binding: WipSearchItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(wipItem: WIPModel) {
            binding.apply {

                txtWord.text = wipItem.wip
                txtMeaning.text = wipItem.meaning

                cbSelect.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
                cbSelect.isChecked = selectedItems.any { it.id == wipItem.id }

                cbSelect.setOnClickListener {
                    toggleSelection(wipItem)
                }

                wipCv.setOnClickListener {
                    if (isSelectionMode) {
                        toggleSelection(wipItem)
                    } else {
                        wipItem.id?.let { it1 -> onWipItemClicked?.invoke(it1) }
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
            if (selectedItems.any { it.id == wipItem.id }) {
                selectedItems.removeAll { it.id == wipItem.id }
            } else {
                selectedItems.add(wipItem)
            }
            val pos = adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                notifyItemChanged(pos)
            }
            onSelectionChanged?.invoke()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return WIPListViewHolder(
            WipSearchItemLayoutBinding.inflate(
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