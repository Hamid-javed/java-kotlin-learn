package com.rameez.hel

import android.content.Context
import android.content.SharedPreferences
import com.rameez.hel.data.model.SourceModel
import org.json.JSONArray
import org.json.JSONObject

object SharedPref {
    private const val PREFS_NAME = "MyPrefs"
    private const val BOOLEAN_KEY = "my_boolean_key"
    private const val SOURCES_KEY = "sources_key"

    fun appLaunched(context: Context, value: Boolean) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = prefs.edit()
        editor.putBoolean(BOOLEAN_KEY, value)
        editor.apply()
    }

    fun isAppLaunched(context: Context): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(BOOLEAN_KEY, false)
    }

    fun saveSources(context: Context, sources: List<SourceModel>) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val jsonArray = JSONArray()
        sources.forEach { source ->
            val jsonObject = JSONObject()
            jsonObject.put("name", source.name)
            jsonObject.put("isChecked", source.isChecked)
            jsonArray.put(jsonObject)
        }
        editor.putString(SOURCES_KEY, jsonArray.toString())
        editor.apply()
    }

    fun getSources(context: Context): List<SourceModel> {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(SOURCES_KEY, null) ?: return emptyList()
        val sources = mutableListOf<SourceModel>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                sources.add(
                    SourceModel(
                        jsonObject.getString("name"),
                        jsonObject.getBoolean("isChecked")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sources
    }
}
