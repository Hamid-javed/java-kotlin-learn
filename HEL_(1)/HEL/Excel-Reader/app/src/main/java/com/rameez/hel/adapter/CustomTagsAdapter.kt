package com.rameez.hel.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rameez.hel.databinding.CheckboxItemLayoutBinding

class CustomTagsAdapter(
    private val selectedItems: MutableSet<String> // Pass the set from ViewModel
) : ListAdapter<String, RecyclerView.ViewHolder>(WIPDiffUtil()) {

    var onCheckBoxClicked: ((String, Boolean) -> Unit)? = null
    class WIPDiffUtil : androidx.recyclerview.widget.DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: String,
            newItem: String
        ): Boolean {
            return oldItem == newItem
        }
    }


    inner class CheckBoxItemViewHolder(private val binding: CheckboxItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: String) {
            binding.apply {
                // 1. Unbind listener to prevent callback loops during recycling
                checkbox.setOnCheckedChangeListener(null)

                checkbox.text = item

                // 2. Set the state based on our saved Set
                checkbox.isChecked = selectedItems.contains(item)

                // 3. Re-bind listener
                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedItems.add(item) else selectedItems.remove(item)
                    onCheckBoxClicked?.invoke(item, isChecked)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return CheckBoxItemViewHolder(
            CheckboxItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        (holder as CheckBoxItemViewHolder).bind(item)
    }
}