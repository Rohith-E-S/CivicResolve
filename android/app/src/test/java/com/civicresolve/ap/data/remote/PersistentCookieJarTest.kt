package com.civicresolve.ap.data.remote

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class PersistentCookieJarTest {
    private class MemoryStorage : CookieStorage {
        var records = emptySet<String>()
        override fun read() = records
        override fun write(cookies: Set<String>) { records = cookies }
    }

    private val backend = "https://api.example.test/api/".toHttpUrl()
    private val storage = MemoryStorage()
    private var time = 1_800_000_000_000L
    private fun jar() = PersistentCookieJar(storage, backend) { time }

    @Test fun restoresAllAttributesAndMatchesOriginalScope() {
        val cookie = Cookie.Builder().name("token").value("test-token")
            .hostOnlyDomain(backend.host).path("/api").secure().httpOnly()
            .expiresAt(time + 123456).build()
        jar().saveFromResponse(backend, listOf(cookie))
        val restored = jar()
        assertEquals(cookie, restored.loadForRequest(backend).single())
        for (url in listOf("https://other.test/api", "https://sub.api.example.test/api",
            "http://api.example.test/api", "https://api.example.test/apix", "https://api.example.test/")) {
            assertTrue(url, restored.loadForRequest(url.toHttpUrl()).isEmpty())
        }
    }

    @Test fun domainCookiesRetainSubdomainMatching() {
        val cookie = Cookie.Builder().name("preferences").value("yes")
            .domain("example.test").path("/").build()
        jar().saveFromResponse(backend, listOf(cookie))
        assertEquals(cookie, jar().loadForRequest("https://sub.example.test/".toHttpUrl()).single())
        assertTrue(jar().loadForRequest("https://notexample.test/".toHttpUrl()).isEmpty())
    }

    @Test fun expiresAndRemovesDeletedCookies() {
        val cookie = Cookie.Builder().name("token").value("test-token")
            .hostOnlyDomain(backend.host).expiresAt(time + 1000).build()
        jar().saveFromResponse(backend, listOf(cookie))
        time += 1000
        assertNull(jar().getToken())
        assertTrue(storage.records.isEmpty())
        jar().setToken("new-token")
        jar().saveFromResponse(backend, listOf(Cookie.parse(backend, "token=; Max-Age=0; Path=/")!!))
        assertNull(jar().getToken())
    }

    @Test fun sameNameDifferentPathsSurviveAndLogoutClearsEverything() {
        val root = Cookie.parse(backend, "token=root; Path=/; Secure")!!
        val api = Cookie.parse(backend, "token=api; Path=/api; Secure")!!
        jar().saveFromResponse(backend, listOf(root, api))
        assertEquals(listOf("api", "root"), jar().loadForRequest(backend).map { it.value })
        jar().clearCookies()
        assertTrue(jar().loadForRequest(backend).isEmpty())
    }

    @Test fun syntheticTokenIsBoundToBackendAndDoesNotOverwriteServerExpiry() {
        jar().setToken("test-token")
        assertEquals("test-token", jar().getToken())
        assertTrue(jar().loadForRequest("https://elsewhere.test/".toHttpUrl()).isEmpty())
        assertTrue(jar().loadForRequest("http://api.example.test/".toHttpUrl()).isEmpty())
        val cookie = Cookie.Builder().name("token").value("test-token")
            .hostOnlyDomain(backend.host).path("/").secure().httpOnly().expiresAt(time + 60000).build()
        jar().saveFromResponse(backend, listOf(cookie))
        jar().setToken("test-token")
        assertEquals(cookie, jar().loadForRequest(backend).single())
    }

    @Test fun ignoresMalformedRecordsAndForeignOriginCookies() {
        storage.records = setOf("garbage")
        val cookie = Cookie.Builder().name("token").value("test-token").domain("foreign.test").build()
        jar().saveFromResponse(backend, listOf(cookie))
        assertTrue(jar().loadForRequest(backend).isEmpty())
    }
}
