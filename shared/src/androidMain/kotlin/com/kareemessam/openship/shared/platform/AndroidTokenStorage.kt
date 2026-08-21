package com.kareemessam.openship.shared.platform

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.kareemessam.openship.shared.model.InstanceConfig
import com.kareemessam.openship.shared.storage.TokenStorage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AndroidTokenStorage(context: Context) : TokenStorage {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "openship_secure_instances",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val activeInstanceKey = "__active_instance_id__"

    override fun saveInstance(config: InstanceConfig) {
        val jsonStr = json.encodeToString(config)
        prefs.edit().putString(config.id, jsonStr).apply()

        if (config.isDefault || getActiveInstance() == null) {
            setActiveInstance(config.id)
        }
    }

    override fun loadInstances(): List<InstanceConfig> {
        val instances = mutableListOf<InstanceConfig>()
        val allEntries = prefs.all
        for ((key, value) in allEntries) {
            if (key == activeInstanceKey) continue
            if (value is String) {
                try {
                    val config = json.decodeFromString<InstanceConfig>(value)
                    instances.add(config)
                } catch (_: Exception) {
                    // Ignore corrupted or legacy entries
                }
            }
        }
        return instances.sortedByDescending { it.createdAt }
    }

    override fun getActiveInstance(): InstanceConfig? {
        val activeId = prefs.getString(activeInstanceKey, null)
        if (activeId != null) {
            val jsonStr = prefs.getString(activeId, null)
            if (jsonStr != null) {
                try {
                    return json.decodeFromString<InstanceConfig>(jsonStr)
                } catch (_: Exception) {}
            }
        }
        return loadInstances().firstOrNull()
    }

    override fun setActiveInstance(id: String) {
        prefs.edit().putString(activeInstanceKey, id).apply()
    }

    override fun deleteInstance(id: String) {
        prefs.edit().remove(id).apply()
        if (prefs.getString(activeInstanceKey, null) == id) {
            val remaining = loadInstances().firstOrNull { it.id != id }
            if (remaining != null) {
                setActiveInstance(remaining.id)
            } else {
                prefs.edit().remove(activeInstanceKey).apply()
            }
        }
    }

    override fun clearAll() {
        prefs.edit().clear().apply()
    }
}
