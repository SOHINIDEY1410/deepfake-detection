package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.verimedDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "verimed_preferences"
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_SPECIALTY = stringPreferencesKey("default_specialty")
    }

    val themeModeFlow: Flow<ThemeMode> = context.verimedDataStore.data.map { prefs ->
        val stored = prefs[Keys.THEME_MODE]
        runCatching { if (stored != null) ThemeMode.valueOf(stored) else ThemeMode.SYSTEM }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    val defaultSpecialtyFlow: Flow<ClinicalSpecialty> = context.verimedDataStore.data.map { prefs ->
        val stored = prefs[Keys.DEFAULT_SPECIALTY]
        runCatching {
            if (stored != null) ClinicalSpecialty.valueOf(stored) else ClinicalSpecialty.GENERAL
        }.getOrDefault(ClinicalSpecialty.GENERAL)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.verimedDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setDefaultSpecialty(specialty: ClinicalSpecialty) {
        context.verimedDataStore.edit { prefs ->
            prefs[Keys.DEFAULT_SPECIALTY] = specialty.name
        }
    }
}
