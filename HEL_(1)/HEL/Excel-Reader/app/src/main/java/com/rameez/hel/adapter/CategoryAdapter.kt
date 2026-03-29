package com.rameez.hel.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rameez.hel.databinding.CheckboxItemLayoutBinding


class CategoryAdapter(
    private val selectedCategories: MutableSet<String> // Pass the Set from ViewModel
) : ListAdapter<String, RecyclerView.ViewHolder>(CategoryDiffUtil()) {

    var onCheckBoxClicked: ((String, Boolean, Int) -> Unit)? = null

    class CategoryDiffUtil : androidx.recyclerview.widget.DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
    }
    override fun submitList(list: List<String>?) {
        super.submitList(list)
        if (list != null && selectedCategories.isEmpty()) {
            selectedCategories.addAll(list)
        }
    }


    inner class CheckBoxItemViewHolder(private val binding: CheckboxItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun bind(item: String) {
            binding.apply {
                // 1. Clear listener to prevent recursive calls during rebinding
                checkbox.setOnCheckedChangeListener(null)

                checkbox.text = item

                // 2. Set state based on the Set in ViewModel
                checkbox.isChecked = selectedCategories.contains(item)

                // 3. Re-attach listener
                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedCategories.add(item)
                    } else {
                        selectedCategories.remove(item)
                    }
                    onCheckBoxClicked?.invoke(item, isChecked, adapterPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return CheckBoxItemViewHolder(
            CheckboxItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        (holder as CheckBoxItemViewHolder).bind(item)
    }
}