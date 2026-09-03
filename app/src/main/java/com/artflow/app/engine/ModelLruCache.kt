package com.artflow.app.engine

import android.util.Log
import com.artflow.app.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter

/**
 * Encapsulates an active [Interpreter] and its optional native GPU delegate holder.
 */
data class CachedModel(
    val interpreter: Interpreter,
    val optionsHolder: GpuDelegateProvider.InterpreterOptionsHolder? = null
) : AutoCloseable {
    override fun close() {
        try {
            interpreter.close()
        } catch (e: Throwable) {
            Log.e("CachedModel", "Error closing interpreter: ${e.message}")
        }
        try {
            optionsHolder?.close()
        } catch (e: Throwable) {
            Log.e("CachedModel", "Error closing optionsHolder: ${e.message}")
        }
    }
}

/**
 * Thread-safe 2-slot LRU Cache for neural network models.
 * Strictly maintains <= 2 active models in memory to avoid exhausting Adreno 619 OpenCL contexts.
 * Evicts and closes the oldest model strictly on [dispatchers.ml] to maintain OpenCL thread affinity.
 */
class ModelLruCache(
    private val capacity: Int = 2,
    private val dispatchers: DispatcherProvider
) : AutoCloseable {

    companion object {
        private const val TAG = "ModelLruCache"
    }

    private val lock = Any()
    
    // LinkedHashMap with access-order = true for LRU behavior
    private val cache = object : LinkedHashMap<String, CachedModel>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedModel>?): Boolean {
            if (size > capacity && eldest != null) {
                val modelToClose = eldest.value
                val key = eldest.key
                Log.d(TAG, "Evicting model '$key' to maintain capacity $capacity.")
                CoroutineScope(dispatchers.ml).launch {
                    try {
                        modelToClose.close()
                        Log.d(TAG, "Successfully closed evicted model '$key' on ML dispatcher.")
                    } catch (e: Throwable) {
                        Log.e(TAG, "Exception while closing evicted model '$key': ${e.message}")
                    }
                }
                return true
            }
            return false
        }
    }

    /**
     * Retrieves a cached model by key, updating its LRU access order.
     */
    fun get(key: String): CachedModel? = synchronized(lock) {
        cache[key]
    }

    /**
     * Adds or updates a model in the cache.
     */
    fun put(key: String, model: CachedModel) = synchronized(lock) {
        val existing = cache.put(key, model)
        if (existing != null && existing !== model) {
            CoroutineScope(dispatchers.ml).launch {
                try {
                    existing.close()
                } catch (e: Throwable) {
                    Log.e(TAG, "Exception closing replaced model: ${e.message}")
                }
            }
        }
    }

    /**
     * Current number of models held in the cache.
     */
    fun size(): Int = synchronized(lock) {
        cache.size
    }

    /**
     * Checks if the cache contains a model for the given key.
     */
    fun containsKey(key: String): Boolean = synchronized(lock) {
        cache.containsKey(key)
    }

    /**
     * Removes and closes a specific model.
     */
    fun remove(key: String): CachedModel? = synchronized(lock) {
        val removed = cache.remove(key)
        if (removed != null) {
            CoroutineScope(dispatchers.ml).launch {
                try {
                    removed.close()
                } catch (e: Throwable) {
                    Log.e(TAG, "Exception closing removed model: ${e.message}")
                }
            }
        }
        removed
    }

    /**
     * Closes all cached models and clears the cache.
     */
    override fun close() = synchronized(lock) {
        for ((_, model) in cache) {
            try {
                model.close()
            } catch (e: Throwable) {
                Log.e(TAG, "Error closing model during cache shutdown: ${e.message}")
            }
        }
        cache.clear()
    }
}
