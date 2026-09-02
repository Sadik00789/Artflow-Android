package com.artflow.app

import com.artflow.app.engine.CachedModel
import com.artflow.app.engine.ModelLruCache
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tensorflow.lite.Interpreter
import java.util.concurrent.Executor

/**
 * Unit tests verifying 2-slot ModelLruCache capacity constraints and asynchronous background close execution.
 */
class ModelLruCacheTest {

    @Test
    fun testEvictsOldestInterpreterOnThirdLoad() {
        val synchronousExecutor = Executor { it.run() }
        val cache = ModelLruCache(capacity = 2, backgroundExecutor = synchronousExecutor)

        val interp1 = mockk<Interpreter>(relaxed = true)
        val interp2 = mockk<Interpreter>(relaxed = true)
        val interp3 = mockk<Interpreter>(relaxed = true)

        val model1 = CachedModel(interp1)
        val model2 = CachedModel(interp2)
        val model3 = CachedModel(interp3)

        cache.put("model1", model1)
        cache.put("model2", model2)

        assertEquals(2, cache.size())
        assertTrue(cache.containsKey("model1"))
        assertTrue(cache.containsKey("model2"))

        // Add 3rd model - should evict model1
        cache.put("model3", model3)

        assertEquals(2, cache.size())
        assertFalse(cache.containsKey("model1"))
        assertTrue(cache.containsKey("model2"))
        assertTrue(cache.containsKey("model3"))

        // Verify model1 interpreter was closed
        verify(exactly = 1) { interp1.close() }
        verify(exactly = 0) { interp2.close() }
        verify(exactly = 0) { interp3.close() }
    }

    @Test
    fun testAccessOrderUpdatesLru() {
        val synchronousExecutor = Executor { it.run() }
        val cache = ModelLruCache(capacity = 2, backgroundExecutor = synchronousExecutor)

        val interp1 = mockk<Interpreter>(relaxed = true)
        val interp2 = mockk<Interpreter>(relaxed = true)
        val interp3 = mockk<Interpreter>(relaxed = true)

        cache.put("model1", CachedModel(interp1))
        cache.put("model2", CachedModel(interp2))

        // Access model1 to make model2 the least recently used
        cache.get("model1")

        // Add model3 - should evict model2 instead of model1
        cache.put("model3", CachedModel(interp3))

        assertTrue(cache.containsKey("model1"))
        assertFalse(cache.containsKey("model2"))
        assertTrue(cache.containsKey("model3"))

        verify(exactly = 0) { interp1.close() }
        verify(exactly = 1) { interp2.close() }
    }
}
