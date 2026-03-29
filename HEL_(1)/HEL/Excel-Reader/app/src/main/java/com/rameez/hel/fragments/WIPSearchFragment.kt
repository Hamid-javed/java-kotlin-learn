package com.rameez.hel.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rameez.hel.R
import com.rameez.hel.SharedPref
import com.rameez.hel.adapter.WIPSearchAdapter
import com.rameez.hel.data.model.WIPModel
import com.rameez.hel.databinding.FragmentWIPSearchBinding
import com.rameez.hel.viewmodel.WIPViewModel
import kotlinx.coroutines.launch


class WIPSearchFragment : Fragment() {

    private lateinit var mBinding: FragmentWIPSearchBinding
    private val wipViewModel: WIPViewModel by activityViewModels()
    private val wipSearchAdapter = WIPSearchAdapter()
    private var wipList = listOf<WIPModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = FragmentWIPSearchBinding.inflate(layoutInflater, container, false)
        return mBinding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (wipViewModel.isGenerating.value == true) {
                    Toast.makeText(requireContext(), "Wait for processing", Toast.LENGTH_SHORT).show()
                    return
                }

                if (wipSearchAdapter.isSelectionMode) {
                    wipSearchAdapter.isSelectionMode = false
                    wipSearchAdapter.selectedItems.clear()
                    wipSearchAdapter.notifyDataSetChanged()
                    mBinding.llCountSelectAll.visibility = View.GONE
                } else {
                    findNavController().navigateUp()
                }
            }
        })

        setUpRecyclerView()
        wipViewModel.getWIPs()?.observe(viewLifecycleOwner) {
            wipList = it
            // Re-apply filter if data updates while searching
            applyFilter(mBinding.etSearch.text.toString())
        }

        mBinding.etSearch.requestFocus()
        showSoftKeyboard()

        mBinding.etSearch.doAfterTextChanged {
            applyFilter(it?.toString() ?: "")
        }

        wipSearchAdapter.onWipItemClicked = {
            val bundle = Bundle()
            bundle.putInt("wip_id", it)
            findNavController().navigate(R.id.WIPDetailFragment, bundle)
        }

        wipSearchAdapter.onLongPress = {
            mBinding.llCountSelectAll.visibility = View.VISIBLE
        }

        wipSearchAdapter.onSelectionChanged = {
            if (wipSearchAdapter.selectedItems.isEmpty()) {
                mBinding.llCountSelectAll.visibility = View.GONE
                wipSearchAdapter.isSelectionMode = false
                wipSearchAdapter.notifyDataSetChanged()
            } else {
                mBinding.llCountSelectAll.visibility = View.VISIBLE
                mBinding.tvSelectionCount.text = "${wipSearchAdapter.selectedItems.size} selected"
            }
        }

        mBinding.btnGeneratePara.setOnClickListener {
            val selectedWords = wipSearchAdapter.selectedItems.mapNotNull { it.wip }
            val selectedIds = wipSearchAdapter.selectedItems.mapNotNull { it.id }

            if (selectedWords.size < 2) {
                Toast.makeText(requireContext(), "Please select at least 2 WIPs", Toast.LENGTH_SHORT).show()
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

        wipViewModel.isGenerating.observe(viewLifecycleOwner) { isGenerating ->
            mBinding.generationBarrier.visibility = if (isGenerating) View.VISIBLE else View.GONE
            mBinding.btnGeneratePara.isEnabled = !isGenerating
            mBinding.btnGeneratePara.text = if (isGenerating) "Generating..." else "Generate Para"
        }

        wipViewModel.generatedArticle.observe(viewLifecycleOwner) { article ->
            article ?: return@observe
            wipSearchAdapter.isSelectionMode = false
            wipSearchAdapter.selectedItems.clear()
            wipSearchAdapter.notifyDataSetChanged()
            mBinding.llCountSelectAll.visibility = View.GONE

            lifecycleScope.launch {
                val words = fetchWordsByIds(article.wipIds)
                showArticleBottomSheet(article.content, article.createdAt, article.wipIds, words)
                wipViewModel.clearGeneratedArticle()
            }
        }
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

    override fun onResume() {
        super.onResume()
        // Ensure soft keyboard is shown when coming back if search is empty
        if (mBinding.etSearch.text.isEmpty()) {
            mBinding.etSearch.requestFocus()
            showSoftKeyboard()
        }
    }

    private fun applyFilter(searchQuery: String) {
        if (searchQuery.isEmpty()) {
            mBinding.noData.visibility = View.VISIBLE
            wipSearchAdapter.submitList(emptyList())
            return
        }

        val query = searchQuery.lowercase()
        val filtered = wipList.filter { wipItem ->
            wipItem.wip?.lowercase()?.contains(query) == true ||
            wipItem.meaning?.lowercase()?.contains(query) == true ||
            wipItem.sampleSentence?.lowercase()?.contains(query) == true
        }.distinctBy { it.id }

        if (filtered.isEmpty()) {
            mBinding.noData.visibility = View.VISIBLE
        } else {
            mBinding.noData.visibility = View.GONE
        }

        wipSearchAdapter.submitList(filtered)
    }

    private fun setUpRecyclerView() {
        mBinding.apply {
            rvList.layoutManager = LinearLayoutManager(requireContext())
            rvList.adapter = wipSearchAdapter
        }
    }

    private fun showSoftKeyboard() {
        val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(mBinding.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }
}
