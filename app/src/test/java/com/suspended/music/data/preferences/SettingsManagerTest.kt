package com.suspended.music.data.preferences

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsManagerTest {

    private lateinit var settingsManager: SettingsManager

    class FakeSharedPreferences : SharedPreferences {
        val map = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = map.toMutableMap()
        override fun getString(key: String, defValue: String?): String? = if (map.containsKey(key)) map[key] as? String else defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = null
        override fun getInt(key: String, defValue: Int): Int = map[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = map[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = map[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = map.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(this)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        fun put(key: String, value: Any?) {
            map[key] = value
        }
        fun remove(key: String) {
            map.remove(key)
        }
    }

    class FakeEditor(private val prefs: FakeSharedPreferences) : SharedPreferences.Editor {
        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            if (value == null) prefs.remove(key) else prefs.put(key, value)
            return this
        }
        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = this
        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            prefs.put(key, value)
            return this
        }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            prefs.put(key, value)
            return this
        }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            prefs.put(key, value)
            return this
        }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            prefs.put(key, value)
            return this
        }
        override fun remove(key: String): SharedPreferences.Editor {
            prefs.remove(key)
            return this
        }
        override fun clear(): SharedPreferences.Editor = this
        override fun commit(): Boolean = true
        override fun apply() {}
    }

    class FakeContext(private val prefs: SharedPreferences) : android.content.ContextWrapper(null) {
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
    }

    private lateinit var fakePrefs: FakeSharedPreferences

    @Before
    fun setup() {
        fakePrefs = FakeSharedPreferences()
        val fakeContext = FakeContext(fakePrefs)
        settingsManager = SettingsManager(fakeContext)
    }

    // ── Primary Color Tests ──────────────────────────────────────────────
    @Test
    fun getPrimaryColor_returnsDefaultValueWhenNotSet() {
        assertEquals("9D00FF", settingsManager.getPrimaryColor())
    }

    @Test
    fun getPrimaryColor_returnsStoredValue() {
        settingsManager.setPrimaryColor("FF0000")
        assertEquals("FF0000", settingsManager.getPrimaryColor())
    }

    @Test
    fun getPrimaryColor_returnsDefaultWhenNullStored() {
        // Simulates the scenario where the underlying SharedPreferences returns null
        // instead of the default value, testing the `?: "9D00FF"` fallback.
        fakePrefs.map["primary_color"] = null
        assertEquals("9D00FF", settingsManager.getPrimaryColor())
    }

    // ── Secondary Color Tests ────────────────────────────────────────────
    @Test
    fun getSecondaryColor_returnsDefaultValueWhenNotSet() {
        assertEquals("00D9FF", settingsManager.getSecondaryColor())
    }

    @Test
    fun getSecondaryColor_returnsStoredValue() {
        settingsManager.setSecondaryColor("00FF00")
        assertEquals("00FF00", settingsManager.getSecondaryColor())
    }

    @Test
    fun getSecondaryColor_returnsDefaultWhenNullStored() {
        fakePrefs.map["secondary_color"] = null
        assertEquals("00D9FF", settingsManager.getSecondaryColor())
    }

    // ── Tertiary Color Tests ─────────────────────────────────────────────
    @Test
    fun getTertiaryColor_returnsDefaultValueWhenNotSet() {
        assertEquals("FF6B3D", settingsManager.getTertiaryColor())
    }

    @Test
    fun getTertiaryColor_returnsStoredValue() {
        settingsManager.setTertiaryColor("0000FF")
        assertEquals("0000FF", settingsManager.getTertiaryColor())
    }

    @Test
    fun getTertiaryColor_returnsDefaultWhenNullStored() {
        fakePrefs.map["tertiary_color"] = null
        assertEquals("FF6B3D", settingsManager.getTertiaryColor())
    }
}
