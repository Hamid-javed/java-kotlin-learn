package com.rameez.hel.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rameez.hel.databinding.CheckboxItemLayoutBinding

class DeleteTagsAdapter : ListAdapter<String, RecyclerView.ViewHolder>(WIPDiffUtil()) {

    var onCheckBoxClicked: ((String, Boolean, Int) -> Unit)? = null

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
                checkbox.text = item

                checkbox.setOnCheckedChangeListener { compoundButton, isChecked ->
                    onCheckBoxClicked?.invoke(checkbox.text.toString(), isChecked, position)
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