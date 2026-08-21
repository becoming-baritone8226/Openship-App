package com.kareemessam.openship.shared.storage

import com.kareemessam.openship.shared.model.InstanceConfig

interface TokenStorage {
    fun saveInstance(config: InstanceConfig)
    fun loadInstances(): List<InstanceConfig>
    fun getActiveInstance(): InstanceConfig?
    fun setActiveInstance(id: String)
    fun deleteInstance(id: String)
    fun clearAll()
}
