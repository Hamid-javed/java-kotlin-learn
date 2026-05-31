package com.rameez.hel.fragments

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rameez.hel.R
import com.rameez.hel.SharedPref
import com.rameez.hel.adapter.WIPListAdapter
import com.rameez.hel.data.model.WIPModel
import com.rameez.hel.data.model.ArticleModel
import com.rameez.hel.databinding.FragmentWIPListBinding
import com.rameez.hel.utils.PermissionUtils
import com.rameez.hel.viewmodel.SharedViewModel
import com.rameez.hel.viewmodel.WIPViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class WIPListFragment : Fragment() {

    private lateinit var mBinding: FragmentWIPListBinding
    private val wipListAdapter = WIPListAdapter()
    private val wipViewModel: WIPViewModel by activityViewModels()
    private val STORAGE_PERMISSION_CODE = 100
    private val OPEN_FILE_REQUEST_CODE = 200
    private lateinit var permissionUtils: PermissionUtils
    private val wipList = arrayListOf<WIPModel>()
    private var isFirstTime = false
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var shuffledList = arrayListOf<WIPModel>()
    val importTime = System.currentTimeMillis()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private var latestArticle: ArticleModel? = null
    private var isAscending = true

    override fun onStart() {
        super.onStart()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        SharedPref.appLaunched(requireContext(), false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        if (::mBinding.isInitialized.not()) {
            isFirstTime = true
            mBinding = FragmentWIPListBinding.inflate(layoutInflater, container, false)
        }
        askForPermission()

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (wipViewModel.isGenerating.value == true) {
                    Toast.makeText(requireContext(), "Wait for processing", Toast.LENGTH_SHORT).show()
                    return
                }

                if (wipListAdapter.isSelectionMode) {
                    wipListAdapter.isSelectionMode = false
                    wipListAdapter.selectedItems.clear()
                    wipListAdapter.notifyDataSetChanged()
                    mBinding.btnGeneratePara.visibility = View.GONE
                    mBinding.cbSelectAll.visibility = View.GONE
                    mBinding.tvSelectionCount.visibility = View.GONE
                } else if (sharedViewModel.isShowingFilteredResults) {
                    sharedViewModel.isShowingFilteredResults = false
                    findNavController().navigate(R.id.WIPFilterFragment)
                } else {
                    requireActivity().finish()
                }
            }

        })

        permissionUtils = PermissionUtils(this)
        setUpRecyclerView()

        // #17: Show filtered results as list when coming from filter "Show List" button
        if (sharedViewModel.isFilterApplied) {
            val idsList = sharedViewModel.filteredWipsList.mapNotNull { it.id }
            
            lifecycleScope.launch {
                mBinding.rvList.visibility = View.GONE
                mBinding.progressbar.visibility = View.VISIBLE
                
                val freshList = wipViewModel.getWIPs2() ?: emptyList()
                val finalList = idsList.mapNotNull { id -> freshList.find { it.id == id } }
                
                shuffledList = ArrayList(finalList)
                wipListAdapter.submitList(shuffledList.toList()) {
                    mBinding.rvList.scrollToPosition(0)
                }
                
                mBinding.rvList.visibility = View.VISIBLE
                mBinding.progressbar.visibility = View.GONE
                updateResultCount(shuffledList.size)
            }
            
            sharedViewModel.isFilterApplied = false
            isFirstTime = false
        }

        wipViewModel.getWIPs()?.observe(viewLifecycleOwner) {
            if (isFirstTime) {
                mBinding.rvList.visibility = View.GONE
                mBinding.progressbar.visibility = View.VISIBLE
                shuffledList = it.shuffled() as ArrayList<WIPModel>
                wipListAdapter.submitList(shuffledList)
                lifecycleScope.launch {
                    delay(500)
                    mBinding.rvList.scrollToPosition(0)
                    mBinding.rvList.visibility = View.VISIBLE
                    mBinding.progressbar.visibility = View.GONE
                    updateResultCount(shuffledList.size)

                    isFirstTime = false
                }
            }
        }

        if (sharedViewModel.itemIdFromHome != null) {
            if (sharedViewModel.isWIPDeleted.not()) {
                wipViewModel.getWIPById(sharedViewModel.itemIdFromHome ?: 0)
                    ?.observe(viewLifecycleOwner) {

                        sharedViewModel.itemPosFromHome?.let { pos ->
                            if (pos in shuffledList.indices && it != null) {
                                shuffledList[pos].apply {
                                    sr = it.sr
                                    category = it.category
                                    wip = it.wip
                                    meaning = it.meaning
                                    sampleSentence = it.sampleSentence
                                    customTag = it.customTag
                                    readCount = it.readCount
                                    displayCount = it.displayCount
                                    lastParaCreatedAt = it.lastParaCreatedAt
                                }
                                wipListAdapter.notifyItemChanged(pos)
                            } else {
                                Log.w("WIPListFragment", "Skipped updating item: pos=$pos, shuffledList size=${shuffledList.size}")
                            }
                        }


                    }
            } else {
                isFirstTime = true
                sharedViewModel.isWIPDeleted = false
            }

        }

        if(sharedViewModel.isWipAdded) {
            isFirstTime = true
            sharedViewModel.isWipAdded = false
        }


//        if (SharedPref.isAppLaunched(requireContext())) {
//            lifecycleScope.launch {
//                wipViewModel.getWIPs2()?.forEach {
//                    val incCount = it.displayCount?.toInt()?.plus(1)?.toFloat()
//                    it.id?.let { it1 ->
//                        if (incCount != null) {
//                            wipViewModel.updateViewedCount(it1, incCount)
//                        }
//                    }
//                }
//                SharedPref.appLaunched(requireContext(), false)
//            }
//        }


        mBinding.apply {

            listOrientation.setOnClickListener {
                toggleSort()
            }

            imgImportExport.setOnClickListener {
                showCustomDialog().show()
            }

            llSearch.setOnClickListener {
                sharedViewModel.itemIdFromHome = null
                sharedViewModel.itemPosFromHome = null
                findNavController().navigate(R.id.WIPSearchFragment)
            }

            imgFilter.setOnClickListener {
                sharedViewModel.itemIdFromHome = null
                sharedViewModel.itemPosFromHome = null
                findNavController().navigate(R.id.WIPFilterFragment)
            }

            cbSelectAll.setOnClickListener {
                wipListAdapter.selectAll(cbSelectAll.isChecked)
            }

            btnGeneratePara.setOnClickListener {
                val selectedWords = wipListAdapter.selectedItems.mapNotNull { it.wip }
                val selectedIds = wipListAdapter.selectedItems.mapNotNull { it.id }

                Log.d("WIPListFragment", "Selected WIPs for Para: $selectedWords")
                if (selectedWords.size < 2) {
                    Toast.makeText(requireContext(),
                        "Select at least 2 WIPs",
                        Toast.LENGTH_SHORT
                    ).show()
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

            imgSavedArticle.setOnClickListener {
                latestArticle?.let { article ->
                    lifecycleScope.launch {
                        val words = fetchWordsByIds(article.wipIds)
                        showArticleBottomSheet(article.content, article.createdAt, article.wipIds, words)
                    }
                }
            }
        }

        wipViewModel.isGenerating.observe(viewLifecycleOwner) { isGenerating ->
            mBinding.generationBarrier.visibility = if (isGenerating) View.VISIBLE else View.GONE
            mBinding.btnGeneratePara.text = if (isGenerating) "Generating article..." else "Generate Para"
            mBinding.btnGeneratePara.isEnabled = !isGenerating
        }

        wipViewModel.generatedArticle.observe(viewLifecycleOwner) { article ->
            article ?: return@observe

            // Clear selection mode when article is generated
            wipListAdapter.clearSelection()
            mBinding.btnGeneratePara.visibility = View.GONE
            mBinding.cbSelectAll.visibility = View.GONE
            mBinding.cbSelectAll.isChecked = false
            mBinding.tvSelectionCount.visibility = View.GONE

            lifecycleScope.launch {
                val words = fetchWordsByIds(article.wipIds)
                showArticleBottomSheet(article.content, article.createdAt, article.wipIds, words)
                wipViewModel.clearGeneratedArticle()
            }
        }

        wipViewModel.getLatestArticle()?.observe(viewLifecycleOwner) { article ->
            if (article != null) {
                latestArticle = article
                mBinding.imgSavedArticle.visibility = View.VISIBLE
            } else {
                mBinding.imgSavedArticle.visibility = View.GONE
            }
        }

        wipListAdapter.onWipItemClicked = { id, viewCount, pos ->
            sharedViewModel.itemIdFromHome = id
            sharedViewModel.itemPosFromHome = pos
            val bundle = Bundle()
            bundle.putInt("wip_id", id)
            bundle.putFloat("view_count", viewCount)
            findNavController().navigate(R.id.WIPDetailFragment, bundle)
        }

        wipListAdapter.onLongPress = {
            mBinding.btnGeneratePara.visibility = View.VISIBLE
            mBinding.cbSelectAll.visibility = View.VISIBLE
            mBinding.tvSelectionCount.visibility = View.VISIBLE
        }

        wipListAdapter.onSelectionChanged = {
            if (wipListAdapter.selectedItems.isEmpty()) {
                mBinding.btnGeneratePara.visibility = View.GONE
                mBinding.cbSelectAll.visibility = View.GONE
                mBinding.tvSelectionCount.visibility = View.GONE
                wipListAdapter.isSelectionMode = false
                wipListAdapter.notifyDataSetChanged()
                mBinding.cbSelectAll.isChecked = false
            } else {
                mBinding.btnGeneratePara.visibility = View.VISIBLE
                mBinding.cbSelectAll.visibility = View.VISIBLE
                mBinding.tvSelectionCount.visibility = View.VISIBLE
                mBinding.cbSelectAll.isChecked = wipListAdapter.selectedItems.size == wipListAdapter.currentList.size
                mBinding.tvSelectionCount.text = "${wipListAdapter.selectedItems.size}/${wipListAdapter.currentList.size} selected"
            }
        }

        mBinding.rvList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val firstVisibleItemPosition =
                        layoutManager.findFirstCompletelyVisibleItemPosition()
                    val lastVisibleItemPosition =
                        layoutManager.findLastCompletelyVisibleItemPosition()
                    Log.d("TAG", "First visible item position: $firstVisibleItemPosition")
                    Log.d("TAG", "Last visible item position: $lastVisibleItemPosition")
                }
            }
        })
//        wipListAdapter.onIncViewedCount = { id, viewCount ->
//            wipViewModel.updateViewedCount(id, viewCount)
//        }
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

    private fun updateResultCount(count: Int) {
        mBinding.tvResultCount.text = "Showing $count cards"
        mBinding.tvResultCount.visibility =
            if (count == 0) View.GONE else View.VISIBLE
    }

    private fun setUpRecyclerView() {
        mBinding.apply {
            rvList.layoutManager = LinearLayoutManager(requireContext())
            rvList.adapter = wipListAdapter
        }
    }

    private fun toggleSort() {
        isAscending = !isAscending
        val sorted = if (isAscending) {
            shuffledList.sortedBy { it.wip?.lowercase() }
        } else {
            shuffledList.sortedByDescending { it.wip?.lowercase() }
        }
        wipListAdapter.submitList(sorted)
        mBinding.rvList.scrollToPosition(0)
    }

    fun <T> LiveData<T>.observeOnce(owner: LifecycleOwner, observer: (T) -> Unit) {
        observe(owner) {
            observer(it)
            removeObservers(owner)
        }
    }

    private fun showCustomDialog(): AlertDialog {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.custom_dialog_layout, null)

        val importWIP = dialogView.findViewById<TextView>(R.id.rWord)
        val exportWIP = dialogView.findViewById<TextView>(R.id.rPhrase)
        val addWIP = dialogView.findViewById<TextView>(R.id.rIdiom)
        val rEncountered = dialogView.findViewById<TextView>(R.id.rEncountered)
        val rViewed = dialogView.findViewById<TextView>(R.id.rViewed)
        val deleteWIP = dialogView.findViewById<TextView>(R.id.rAllWips)
        val rResetTags = dialogView.findViewById<TextView>(R.id.rResetTags)
        val rDeleteAllTags = dialogView.findViewById<TextView>(R.id.rDeleteAllTags)
        val rSourceSettings = dialogView.findViewById<TextView>(R.id.rSourceSettings)


        val alertDialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)

        val alertDialog = alertDialogBuilder.create()

        importWIP.setOnClickListener {
            alertDialog.dismiss()
            openFileUsingSAF()
        }
        rResetTags.setOnClickListener {
            alertDialog.dismiss()
            showResetTagsDialog().show()
        }

        // #32: Delete ALL tags from all WPIs at once
        rDeleteAllTags.setOnClickListener {
            alertDialog.dismiss()
            AlertDialog.Builder(requireContext())
                .setTitle("Delete All Tags")
                .setMessage("This will remove ALL tags from ALL WPIs. This cannot be undone.")
                .setPositiveButton("Delete All") { _, _ ->
                    wipViewModel.allTags?.observe(viewLifecycleOwner) { allTags ->
                        if (allTags.isNotEmpty()) {
                            wipViewModel.removeTagsFromAllWIPs(allTags)
                            isFirstTime = true
                            Toast.makeText(requireContext(), "All tags deleted", Toast.LENGTH_SHORT).show()
                        }
                        wipViewModel.allTags?.removeObservers(viewLifecycleOwner)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }


        exportWIP.setOnClickListener {
            alertDialog.dismiss()
            wipViewModel.getWIPs()?.observeOnce(viewLifecycleOwner) { data ->
                val sdf = SimpleDateFormat("dd_MM_yyyy_HH_mm_ss", Locale.getDefault())
                val currentDateTime = sdf.format(Date())
                val fileName = "WPI_Export_$currentDateTime"
                exportToExcel(data, fileName)
            }
        }


        addWIP.setOnClickListener {
            alertDialog.dismiss()
            findNavController().navigate(R.id.WIPEditFragment)
        }

        deleteWIP.setOnClickListener {
            alertDialog.dismiss()
            showDeleteWIPDialog().show()
        }

        rEncountered.setOnClickListener {
            alertDialog.dismiss()
            showResetEncounteredDialog().show()
        }

        rViewed.setOnClickListener {
            alertDialog.dismiss()
            showResetViewedDialog().show()
        }

        rSourceSettings.setOnClickListener {
            alertDialog.dismiss()
            val sourceSelectionBS = SourceSelectionBottomSheetFragment.newInstance(isSettings = true)
            sourceSelectionBS.show(childFragmentManager, "SourceSelectionBS")
        }

        return alertDialog
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFileUsingSAF()

            } else {
                Toast.makeText(
                    requireContext(),
                    "Please allow permission to import or export files",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun openFileUsingSAF() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // Or "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        startActivityForResult(intent, OPEN_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OPEN_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                val uri = data.data

                if (uri != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        wipViewModel.dropTable() // clear old data first
                        readExcelFile(uri)
                    }
                }

            }
        }
    }

    private fun readExcelFile(uri: Uri) {

        val contentResolver: ContentResolver = requireContext().contentResolver
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)

            // Import Sources from second sheet if present
            if (workbook.numberOfSheets > 1) {
                val sourcesSheet = workbook.getSheetAt(1)
                val importedSources = mutableListOf<com.rameez.hel.data.model.SourceModel>()
                for (r in 1..sourcesSheet.lastRowNum) {
                    val row = sourcesSheet.getRow(r) ?: continue
                    val name = row.getCell(0)?.toString() ?: continue
                    val checked = row.getCell(1)?.toString()?.trim()?.lowercase() == "true"
                    importedSources.add(com.rameez.hel.data.model.SourceModel(name, checked))
                }
                if (importedSources.isNotEmpty()) {
                    SharedPref.saveSources(requireContext(), importedSources)
                }
            }
            Log.d("TAG", "filledRowCount: ${getFilledRowCount(sheet)}")
            val totalRows = getFilledRowCount(sheet)
            val totalColumns = getTotalNoColumns(sheet)
            Log.d("TAG", "total values $totalRows $totalColumns")

            for (i in 1 until totalRows) {
                var j = 0
                val wipModelBuilder = WIPModel.Builder()
                Log.d("TAG", "Row $i \n")

                while (j < totalColumns) {
                    val cellValue = sheet.getRow(i).getCell(j)
                    Log.d("TAG", "cell Values $cellValue")

                    when (j) {
                        0 -> {
                            val value = cellValue?.toString()
                            wipModelBuilder.sr(value?.toFloatOrNull() ?: 0.0f)
                        }
                        1 -> wipModelBuilder.category(cellValue?.toString())
                        2 -> wipModelBuilder.wip(cellValue?.toString())
                        3 -> wipModelBuilder.meaning(cellValue?.toString())
                        4 -> wipModelBuilder.sampleSentence(cellValue?.toString())
                        5 -> wipModelBuilder.customTag(
                            cellValue?.toString()?.split(",")?.map { it.trim() } ?: emptyList()
                        )
                        6 -> {
                            val value = cellValue?.toString()
                            wipModelBuilder.readCount(value?.toFloatOrNull() ?: 0.0f)
                        }
                        7 -> {
                            val value = cellValue?.toString()
                            wipModelBuilder.displayCount(value?.toFloatOrNull() ?: 0.0f)
                        }

                        // ---- FIXED DATE HANDLING BELOW ----

                        8 -> { // Created At
                            val date = getDateFromCell(cellValue)
                            if (date != null) wipModelBuilder.createdAt(date.time)
                        }
                        9 -> { // Modified At
                            val date = getDateFromCell(cellValue)
                            if (date != null) wipModelBuilder.modifiedAt(date.time)
                        }
                        10 -> { // Last Viewed At
                            val date = getDateFromCell(cellValue)
                            if (date != null) wipModelBuilder.displayCountUpdatedAt(date.time)
                        }
                        11 -> { // Last Encountered At
                            val date = getDateFromCell(cellValue)
                            if (date != null) wipModelBuilder.readCountUpdatedAt(date.time)
                        }
                        12 -> { // First Viewed At
                            val date = getDateFromCell(cellValue)
                            if (date != null) wipModelBuilder.firstViewedAt(date.time)
                        }
                        13 -> { // First Encountered At
                            val date = getDateFromCell(cellValue)
                            if (date != null) wipModelBuilder.firstEncounteredAt(date.time)
                        }
                        14 -> { // Para Created At
                            val date = getDateFromCell(cellValue)
                            if (date != null) wipModelBuilder.lastParaCreatedAt(date.time)
                        }
                    }
                    j++
                }

                val wipModel = wipModelBuilder.build()
                wipViewModel.insertWIP(wipModel)
            }
        }
        isFirstTime = true
    }

    private fun getDateFromCell(cell: Cell?): Date? {
        return when (cell?.cellType) {
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    cell.dateCellValue
                } else null
            }
            CellType.STRING -> parseDateOrNull(cell.stringCellValue)
            else -> null
        }
    }


    private fun parseDateOrNull(value: String?): Date? {
        if (value.isNullOrBlank() || value == "-") return null
        return try {
            dateFormat.parse(value)
        } catch (e: Exception) {
            null
        }
    }


    private fun getFilledRowCount(sheet: Sheet): Int {
        var rowCount = 0
        val iterator = sheet.iterator()
        while (iterator.hasNext()) {
            val row = iterator.next()
            if (isEmptyRow(row)) {
                break
            }
            rowCount++
        }
        return rowCount
    }

    private fun isEmptyRow(row: Row): Boolean {
        val lastCellNum = row.lastCellNum.toInt()
        for (i in 0 until lastCellNum) {
            val cell = row.getCell(i)
            if (cell != null && cell.cellType != CellType.BLANK) {
                return false
            }
        }
        return true
    }

    private fun getTotalNoColumns(sheet: Sheet): Int {
        val firstRow = sheet.getRow(0)
        var i = 0
        var totalNoColumns = 0
        while (i < firstRow.physicalNumberOfCells && firstRow.getCell(i).toString() != "") {
            val cellValue = firstRow.getCell(i).toString()
            totalNoColumns += 1
            i++
        }
        return totalNoColumns
    }

    private fun exportToExcel(data: List<WIPModel>, fileName: String) {

        val progressDialog = ProgressDialog(context)
        progressDialog.setMessage("Saving file...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Sheet1")

        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Sr")
        headerRow.createCell(1).setCellValue("Category")
        headerRow.createCell(2).setCellValue("WIP")
        headerRow.createCell(3).setCellValue("Meaning")
        headerRow.createCell(4).setCellValue("Sample Sentence")
        headerRow.createCell(5).setCellValue("Custom Tag")
        headerRow.createCell(6).setCellValue("Number of times read in general reading")
        headerRow.createCell(7).setCellValue("Number of times displayed in app")
        headerRow.createCell(8).setCellValue("Created At")
        headerRow.createCell(9).setCellValue("Modified At")
        headerRow.createCell(10).setCellValue("Last Viewed At")
        headerRow.createCell(11).setCellValue("Last Encountered At")

        headerRow.createCell(12).setCellValue("First Viewed At")
        headerRow.createCell(13).setCellValue("First Encountered At")
        headerRow.createCell(14).setCellValue("Para Created At")
        var rowIndex = 1
        for (model in data) {
            val row = sheet.createRow(rowIndex++)
            model.sr?.toDouble()?.let { row.createCell(0).setCellValue(it) }
            row.createCell(1).setCellValue(model.category ?: "")
            row.createCell(2).setCellValue(model.wip ?: "")
            row.createCell(3).setCellValue(model.meaning ?: "")
            row.createCell(4).setCellValue(model.sampleSentence ?: "")
            row.createCell(5).setCellValue(model.customTag?.joinToString(", "))
            model.readCount?.toDouble()?.let { row.createCell(6).setCellValue(it) }
            model.displayCount?.toDouble()?.let { row.createCell(7).setCellValue(it) }
            row.createCell(8).setCellValue(
                if (model.createdAt != 0L)
                    dateFormat.format(Date(model.createdAt))
                else
                    "-"
            )

            row.createCell(9).setCellValue(
                if (model.modifiedAt != 0L)
                    dateFormat.format(Date(model.modifiedAt))
                else
                    "-"
            )

            row.createCell(10).setCellValue(
                if (model.displayCountUpdatedAt != 0L)
                    dateFormat.format(Date(model.displayCountUpdatedAt))
                else
                    "-"
            )

            row.createCell(11).setCellValue(
                if (model.readCountUpdatedAt != 0L)
                    dateFormat.format(Date(model.readCountUpdatedAt))
                else
                    "-"
            )

            row.createCell(12).setCellValue(if (model.firstViewedAt != 0L) dateFormat.format(Date(model.firstViewedAt)) else "-")
            row.createCell(13).setCellValue(if (model.firstEncounteredAt != 0L) dateFormat.format(Date(model.firstEncounteredAt)) else "-")
            row.createCell(14).setCellValue(if (model.lastParaCreatedAt != 0L) dateFormat.format(Date(model.lastParaCreatedAt)) else "-")

        }

        // Export Sources as a separate sheet
        val sourcesSheet = workbook.createSheet("Sources")
        val sourcesHeader = sourcesSheet.createRow(0)
        sourcesHeader.createCell(0).setCellValue("Name")
        sourcesHeader.createCell(1).setCellValue("IsChecked")
        val sources = SharedPref.getSources(requireContext())
        sources.forEachIndexed { index, source ->
            val srcRow = sourcesSheet.createRow(index + 1)
            srcRow.createCell(0).setCellValue(source.name)
            srcRow.createCell(1).setCellValue(if (source.isChecked) "true" else "false")
        }

        try {
            val documentsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(documentsDir, "$fileName.xlsx")
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }
            lifecycleScope.launch {
                delay(3000)
                progressDialog.dismiss()
                Toast.makeText(context, "File successfully exported", Toast.LENGTH_SHORT).show()
            }

        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                workbook.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    private fun askForPermission() {

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        PermissionUtils(this).requestPermissions(
            permission
        ).onPermissionResult = object : PermissionUtils.OnPermissionResult {
            override fun onPermissionGranted() {
//                Toast.makeText(requireContext(), "Permission Granted", Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionDenied(neverAskAgain: Boolean) {
                Toast.makeText(
                    requireContext(),
                    "Please grant permission to import or export .xlsx files",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        updateResultCount(wipListAdapter.currentList.size)
    }
    private fun showDeleteWIPDialog(): AlertDialog {
        val dialogView =
            LayoutInflater.from(requireContext())
                .inflate(R.layout.custom_delete_wip_dialog_kayout, null)

        val dWord = dialogView.findViewById<TextView>(R.id.rWord)
        val dPhrase = dialogView.findViewById<TextView>(R.id.rPhrase)
        val dIdiom = dialogView.findViewById<TextView>(R.id.rIdiom)
        val dAllWips = dialogView.findViewById<TextView>(R.id.rAllWips)

        val alertDialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)

        val alertDialog = alertDialogBuilder.create()


        dWord.setOnClickListener {
            alertDialog.dismiss()
            val categories = shuffledList.filter { it.category?.lowercase() == "word".lowercase() }.map { it.category }
            wipViewModel.deleteWholeCategory(categories)
            isFirstTime = true
        }

        dPhrase.setOnClickListener {
            alertDialog.dismiss()
            val categories = shuffledList.filter { it.category?.lowercase() == "phrase".lowercase() }.map { it.category }
            wipViewModel.deleteWholeCategory(categories)
            isFirstTime = true
        }

        dIdiom.setOnClickListener {
            alertDialog.dismiss()
            val categories = shuffledList.filter { it.category?.lowercase() == "idiom".lowercase() }.map { it.category }
            wipViewModel.deleteWholeCategory(categories)
            isFirstTime = true
        }

        dAllWips.setOnClickListener {
            alertDialog.dismiss()
            wipViewModel.dropTable()
            isFirstTime = true
        }


        return alertDialog
    }



    private fun showResetTagsDialog(): AlertDialog {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_reset_tags, null)

        val container = dialogView.findViewById<LinearLayout>(R.id.tagContainer)
        val cbSelectAll = dialogView.findViewById<CheckBox>(R.id.cbSelectAll)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)

        val checkBoxMap = mutableMapOf<String, CheckBox>()

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Prevent recursion when syncing states
        var internalChange = false

        wipViewModel.allTags?.observe(viewLifecycleOwner) { tags ->
            container.removeAllViews()
            checkBoxMap.clear()
            cbSelectAll.isChecked = false

            tags.forEach { tag ->
                val cb = CheckBox(requireContext())
                cb.text = tag
                cb.setTextColor(
                    ContextCompat.getColor(requireContext(), android.R.color.black)
                )



                cb.setOnCheckedChangeListener { _, _ ->
                    if (internalChange) return@setOnCheckedChangeListener

                    internalChange = true
                    cbSelectAll.isChecked =
                        checkBoxMap.values.all { it.isChecked }
                    internalChange = false
                }

                container.addView(cb)
                checkBoxMap[tag] = cb
            }
        }

        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (internalChange) return@setOnCheckedChangeListener

            internalChange = true
            checkBoxMap.values.forEach { it.isChecked = isChecked }
            internalChange = false
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            val selectedTags = checkBoxMap
                .filter { it.value.isChecked }
                .keys
                .toList()

            if (selectedTags.isNotEmpty()) {
                wipViewModel.removeTagsFromAllWIPs(selectedTags)
                isFirstTime = true
            }
            dialog.dismiss()
        }

        return dialog
    }



    private fun showResetEncounteredDialog(): AlertDialog {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.custom_reset_dialog_layout, null)

        val rWord = dialogView.findViewById<CheckBox>(R.id.rWord)
        val rPhrase = dialogView.findViewById<CheckBox>(R.id.rPhrase)
        val rIdiom = dialogView.findViewById<CheckBox>(R.id.rIdiom)
        val bDone = dialogView.findViewById<Button>(R.id.bDone)

        val alertDialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)

        val alertDialog = alertDialogBuilder.create()

        bDone.setOnClickListener {
            val list = arrayListOf<String>()
            if (rWord.isChecked) {
                list.add("word")
            }
            if (rPhrase.isChecked) {
                list.add("phrase")
            }
            if (rIdiom.isChecked) {
                list.add("idiom")
            }

            wipViewModel.resetEncounteredForCategories(list)
            alertDialog.dismiss()
            isFirstTime = true
        }


        return alertDialog
    }


    private fun showResetViewedDialog(): AlertDialog {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.custom_reset_dialog_layout, null)

        val rWord = dialogView.findViewById<CheckBox>(R.id.rWord)
        val rPhrase = dialogView.findViewById<CheckBox>(R.id.rPhrase)
        val rIdiom = dialogView.findViewById<CheckBox>(R.id.rIdiom)
        val bDone = dialogView.findViewById<Button>(R.id.bDone)

        val alertDialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)

        val alertDialog = alertDialogBuilder.create()

        bDone.setOnClickListener {
            val list = arrayListOf<String>()
            if (rWord.isChecked) {
                list.add("word")
            }
            if (rPhrase.isChecked) {
                list.add("phrase")
            }
            if (rIdiom.isChecked) {
                list.add("idiom")
            }

            wipViewModel.resetViewedForCategories(list)
            alertDialog.dismiss()
            isFirstTime = true
        }


        return alertDialog
    }
}
