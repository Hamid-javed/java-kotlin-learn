package com.rameez.hel.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.map
import com.rameez.hel.SharedPref
import com.rameez.hel.data.WIPDatabase
import com.rameez.hel.data.model.WIPModel
import com.rameez.hel.data.model.ArticleModel
import com.rameez.hel.data.repository.WIPRepository
import com.rameez.hel.utils.AiSentenceService
import kotlinx.coroutines.launch

class WIPViewModel : ViewModel() {

    private val wipDao = WIPDatabase.getDatabase()?.wipDao()
    private val wipRepository = wipDao?.let { WIPRepository(it) }


    val allTags: LiveData<List<String>>? = getWIPs()?.map { wips ->
        wips
            .flatMap { it.customTag ?: emptyList() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }


    val _getWipsObserver: MutableLiveData<List<WIPModel>> = MutableLiveData()
    val getWipsObserver: LiveData<List<WIPModel>> = _getWipsObserver
    var list = arrayListOf<WIPModel>()


    fun insertWIP(wipItem: WIPModel) = viewModelScope.launch {
        wipRepository?.insertWIP(wipItem)
    }

    fun getWIPs(): LiveData<List<WIPModel>>? {
        return wipRepository?.getWIPs()
    }

     suspend fun getWIPs2(): List<WIPModel>? {
        return wipRepository?.getWIPs2()
    }

    fun dropTable() = viewModelScope.launch {
        wipRepository?.dropTable()
    }

    fun getWIPById(id: Int): LiveData<WIPModel>? {
        return wipRepository?.getWIPById(id)
    }

    fun updateWIP(
        id: Int,
        category: String,
        wip: String,
        meaning: String,
        sampleSentence: String,
        customTag: List<String>,
        readCount: Float,
        viewedCount: Float
    ) = viewModelScope.launch {
        wipRepository?.updateWIP(
            id,
            category,
            wip,
            meaning,
            sampleSentence,
            customTag,
            readCount,
            viewedCount
        )
    }

    fun incrementDisplayCount(id: Int) = viewModelScope.launch {
        wipRepository?.incrementDisplayCount(id)
    }

    fun incrementReadCount(id: Int, inc: Float = 1.0f) = viewModelScope.launch {
        wipRepository?.updateReadCount(id, inc)
    }


    fun updateViewedCount(id: Int, viewCount: Float) = viewModelScope.launch {
        wipRepository?.updateViewedCount(id, viewCount)
    }
    fun updateReadCount(id: Int, readCount: Float) = viewModelScope.launch {
        wipRepository?.updateReadCount(id, readCount)
    }




    fun deleteWIPById(id: Int) = viewModelScope.launch {
        wipRepository?.deleteWIPById(id)
    }

    fun deleteWholeCategory(categories: List<String?>) = viewModelScope.launch {
        wipRepository?.deleteWholeCategory(categories)
    }

    fun resetEncountered(id: Int) = viewModelScope.launch {
        wipRepository?.resetEncountered(id)
    }

    fun resetViewed(id: Int) = viewModelScope.launch {
        wipRepository?.resetViewed(id)
    }

    fun resetEncounteredForCategories(categories: List<String>) = viewModelScope.launch {
        wipRepository?.resetEncounteredForCategories(categories)
    }

    fun resetViewedForCategories(categories: List<String>) = viewModelScope.launch {
        wipRepository?.resetViewedForCategories(categories)
    }

    fun removeTagsFromAllWIPs(tagsToRemove: List<String>) = viewModelScope.launch {
        val wips = wipRepository?.getWIPs2() ?: return@launch

        wips.forEach { wip ->
            val currentTags = wip.customTag ?: return@forEach

            val updatedTags = currentTags.filterNot { it in tagsToRemove }

            if (updatedTags.size != currentTags.size) {
                wipRepository.updateTagsOnly(wip.id, updatedTags)
            }
        }
    }
    suspend fun getWIPByIdSync(id: Int): WIPModel? {
        return wipRepository?.getWIPByIdSync(id)
    }





        // Gen AI

    private val aiService = AiSentenceService()


    private val _isGenerating = MutableLiveData<Boolean>(false)
    val isGenerating: LiveData<Boolean> = _isGenerating

    val apiKey = "xxx"

    private val _lastUpdatedWip = MutableLiveData<WIPModel>()
    val lastUpdatedWip: LiveData<WIPModel> = _lastUpdatedWip

    fun generateAndSaveSentence(context: Context, wipId: Int, word: String) = viewModelScope.launch {
        _isGenerating.value = true
        Log.d("AI_SERVICE", "Start generating sentence for WIP ID: $wipId, word: '$word'")

        val savedSources = SharedPref.getSources(context).filter { it.isChecked }.map { it.name }
        val newsSentence = aiService.generateNewsSentence(context, word, apiKey, sources = savedSources)

        if (!newsSentence.isNullOrBlank()) {
            val currentWip = wipRepository?.getWIP2ById(wipId)
            if (currentWip != null) {

                val updatedWip = currentWip.copy(sampleSentence = newsSentence)
                wipRepository.updateWIP(
                    id = currentWip.id ?: wipId,
                    category = currentWip.category ?: "",
                    wip = currentWip.wip ?: "",
                    meaning = currentWip.meaning ?: "",
                    sampleSentence = newsSentence,
                    customTag = currentWip.customTag,
                    readCount = currentWip.readCount ?: 0f,
                    viewedCount = currentWip.displayCount ?: 0f
                )

                _lastUpdatedWip.postValue(updatedWip)
                Log.d("AI_SERVICE", "Sentence saved for WIP ID $wipId: $newsSentence")
            } else {
                Log.e("AI_SERVICE", "WIP not found for ID $wipId")
            }
        } else {
            Log.e("AI_SERVICE", "Sentence generation failed for WIP ID $wipId")
            Toast.makeText(context, "Please! Check your internet connection.", Toast.LENGTH_SHORT).show()
        }

        _isGenerating.value = false
        Log.d("AI_SERVICE", "Finished generating sentence for WIP ID $wipId")
    }


    private val _generatedArticle = MutableLiveData<ArticleModel?>()
    val generatedArticle: LiveData<ArticleModel?> = _generatedArticle

    fun generateArticle(context: Context, words: List<String>, wipIds: List<Int>, selectedSources: List<String> = emptyList()) = viewModelScope.launch {
        Log.d("WIPViewModel", "Generating article with sources: $selectedSources")
        _isGenerating.value = true

        val articleContent = aiService.generateNewsArticle(
            context,
            words,
            apiKey,
            selectedSources
        )


        if (!articleContent.isNullOrBlank()) {
            val now = System.currentTimeMillis()
            val articleModel = ArticleModel(content = articleContent, wipIds = wipIds, createdAt = now)
            
            insertArticle(articleModel)
            
            // Update involved WIPs with the new Para Created At timestamp
            wipIds.forEach { id ->
                wipRepository?.updateLastParaCreatedAt(id, now)
            }
            
            _generatedArticle.postValue(articleModel)
        }

        _isGenerating.value = false
    }

    fun insertArticle(article: ArticleModel) = viewModelScope.launch {
        wipRepository?.insertArticle(article)
    }

    fun getLatestArticle(): LiveData<ArticleModel?>? {
        return wipRepository?.getLatestArticle()
    }

    suspend fun getAllArticles(): List<ArticleModel>? {
        return wipRepository?.getAllArticles()
    }

    fun resetParaCreatedAt(id: Int) = viewModelScope.launch {
        wipRepository?.resetParaCreatedAt(id)
    }

    fun setParaCreatedAt(id: Int, ts: Long) = viewModelScope.launch {
        wipRepository?.updateLastParaCreatedAt(id, ts)
    }

    fun clearGeneratedArticle() {
        _generatedArticle.value = null
    }



}
