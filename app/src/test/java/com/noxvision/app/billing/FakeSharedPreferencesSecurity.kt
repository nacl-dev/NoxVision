package com.noxvision.app.billing

import android.content.SharedPreferences

class FakeSharedPreferencesSecurity : SharedPreferences {
    private val data = mutableMapOf<String, Any>()

    override fun getAll(): MutableMap<String, *> = data

    override fun getString(key: String?, defValue: String?): String? {
        return data[key] as? String ?: defValue
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        @Suppress("UNCHECKED_CAST")
        return data[key] as? MutableSet<String> ?: defValues
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return data[key] as? Int ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        return data[key] as? Long ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        return data[key] as? Float ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return data[key] as? Boolean ?: defValue
    }

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditorSecurity(this)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    fun put(key: String, value: Any?) {
        if (value == null) {
            data.remove(key)
        } else {
            data[key] = value
        }
    }
}

class FakeEditorSecurity(private val prefs: FakeSharedPreferencesSecurity) : SharedPreferences.Editor {
    private val changes = mutableMapOf<String, Any?>()

    override fun putString(key: String?, value: String?): SharedPreferences.Editor {
        key?.let { changes[it] = value }
        return this
    }

    override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
        key?.let { changes[it] = values }
        return this
    }

    override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
        key?.let { changes[it] = value }
        return this
    }

    override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
        key?.let { changes[it] = value }
        return this
    }

    override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
        key?.let { changes[it] = value }
        return this
    }

    override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
        key?.let { changes[it] = value }
        return this
    }

    override fun remove(key: String?): SharedPreferences.Editor {
        key?.let { changes[it] = null }
        return this
    }

    override fun clear(): SharedPreferences.Editor {
        return this
    }

    override fun commit(): Boolean {
        apply()
        return true
    }

    override fun apply() {
        changes.forEach { (k, v) ->
            prefs.put(k, v)
        }
        changes.clear()
    }
}
