package com.rameez.hel.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.rameez.hel.adapter.DeleteTagsAdapter
import com.rameez.hel.databinding.FragmentDeleteTagsBinding
import com.rameez.hel.viewmodel.SharedViewModel
import com.rameez.hel.viewmodel.WIPViewModel

class DeleteTagsFragment : Fragment() {

    private lateinit var mBinding: FragmentDeleteTagsBinding
    private val wipViewModel: WIPViewModel by activityViewModels()
    private val deleteTagsAdapter = DeleteTagsAdapter()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var notDeletedTagsList = arrayListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = FragmentDeleteTagsBinding.inflate(layoutInflater, container, false)

        setUpRecyclerview()

        val intent = arguments
        val id = intent?.getInt("wip_id")

        if (id != null) {
            wipViewModel.getWIPById(id)?.observe(viewLifecycleOwner) {
                val customTags = it.customTag
                notDeletedTagsList = customTags as ArrayList<String>
                deleteTagsAdapter.submitList(customTags)
            }
        }

        deleteTagsAdapter.onCheckBoxClicked = { tagValue, isChecked, position ->
            if (isChecked) {
                notDeletedTagsList.remove(tagValue)
            } else {
                notDeletedTagsList.add(tagValue)
            }
        }

        mBinding.btnSave.setOnClickListener {
            sharedViewModel.notDeletedTags = notDeletedTagsList
            findNavController().popBackStack()
        }

        return mBinding.root
    }

    private fun setUpRecyclerview() {
        mBinding.apply {
            rvList.adapter = deleteTagsAdapter
            rvList.layoutManager = GridLayoutManager(requireContext(), 3)
        }
    }

}