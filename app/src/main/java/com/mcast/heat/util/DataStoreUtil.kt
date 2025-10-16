package com.mcast.heat.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// At the top level of your kotlin file:
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "datastore")


suspend fun saveInt(context: Context, key: String, value: Int) {
    val stringKey = intPreferencesKey(key)
    context.dataStore.edit {
        it[stringKey] = value
    }
}


suspend fun getInt(context: Context, key: String): Int? {
    val stringKey = intPreferencesKey(key)
    return context.dataStore.data.first()[stringKey]
}
