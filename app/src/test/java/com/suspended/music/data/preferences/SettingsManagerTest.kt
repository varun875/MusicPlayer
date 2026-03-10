package com.suspended.music.data.preferences

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsManagerTest {

    @Test
    fun getPrimaryColor_returnsDefaultFallback_whenValueMissingOrNull() {
        val prefs = InMemorySharedPreferences()
        val settingsManager = SettingsManager(prefs)

        assertEquals("9D00FF", settingsManager.getPrimaryColor())

        prefs.edit().putString("primary_color", null).apply()
        assertEquals("9D00FF", settingsManager.getPrimaryColor())
    }
}

private class InMemorySharedPreferences(
    private val data: MutableMap<String, Any?> = mutableMapOf()
) : SharedPreferences {

    override fun getAll(): MutableMap<String, *> = data.toMutableMap()

    override fun getString(key: String?, defValue: String?): String? {
        if (key == null) return defValue
        return data[key] as? String ?: defValue
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        if (key == null) return defValues
        return (data[key] as? Set<String>)?.toMutableSet() ?: defValues
    }

    override fun getInt(key: String?, defValue: Int): Int {
        if (key == null) return defValue
        return data[key] as? Int ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        if (key == null) return defValue
        return data[key] as? Long ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        if (key == null) return defValue
        return data[key] as? Float ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        if (key == null) return defValue
        return data[key] as? Boolean ?: defValue
    }

    override fun contains(key: String?): Boolean {
        if (key == null) return false
        return data.containsKey(key)
    }

    override fun edit(): SharedPreferences.Editor = EditorImpl(data)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        // No-op for tests
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        // No-op for tests
    }
}

private class EditorImpl(
    private val data: MutableMap<String, Any?>
) : SharedPreferences.Editor {
    private val updates = mutableMapOf<String, Any?>()
    private var clearRequested = false
    private val removedSentinel = Any()

    override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
        if (key != null) updates[key] = value
    }

    override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply {
        if (key != null) updates[key] = values
    }

    override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
        if (key != null) updates[key] = value
    }

    override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
        if (key != null) updates[key] = value
    }

    override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
        if (key != null) updates[key] = value
    }

    override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
        if (key != null) updates[key] = value
    }

    override fun remove(key: String?): SharedPreferences.Editor = apply {
        if (key != null) updates[key] = removedSentinel
    }

    override fun clear(): SharedPreferences.Editor = apply {
        clearRequested = true
        updates.clear()
    }

    override fun commit(): Boolean {
        apply()
        return true
    }

    override fun apply() {
        if (clearRequested) {
            data.clear()
        }
        updates.forEach { (key, value) ->
            if (value === removedSentinel) {
                data.remove(key)
            } else {
                data[key] = value
            }
        }
        updates.clear()
        clearRequested = false
    }
}
