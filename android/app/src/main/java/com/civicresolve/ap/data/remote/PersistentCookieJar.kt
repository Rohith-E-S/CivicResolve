package com.civicresolve.ap.data.remote

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Storage is encrypted on Android; injectable for offline JVM tests. */
internal interface CookieStorage {
    fun read(): Set<String>
    fun write(cookies: Set<String>)
}

internal class PersistentCookieJar(
    private val storage: CookieStorage,
    private val backendUrl: HttpUrl,
    private val now: () -> Long = System::currentTimeMillis
) : CookieJar {
    private data class Entry(val origin: HttpUrl, val cookie: Cookie)

    private fun read(): MutableList<Entry> = storage.read().mapNotNull { record ->
        val parts = record.split('\n', limit = 3)
        if (parts.size != 3) return@mapNotNull null
        val origin = parts[0].toHttpUrlOrNull() ?: return@mapNotNull null
        val expiry = parts[1].toLongOrNull() ?: return@mapNotNull null
        val parsed = Cookie.parse(origin, parts[2]) ?: return@mapNotNull null
        // Set-Cookie text retains host-only, Domain, Path, Secure and HttpOnly.
        // Keep millisecond precision separately; don't turn session cookies persistent.
        val builder = Cookie.Builder().name(parsed.name).value(parsed.value).path(parsed.path)
        if (parsed.hostOnly) builder.hostOnlyDomain(parsed.domain) else builder.domain(parsed.domain)
        if (parsed.secure) builder.secure()
        if (parsed.httpOnly) builder.httpOnly()
        if (parsed.persistent) builder.expiresAt(expiry)
        val cookie = builder.build()
        if (expiry <= now()) null else Entry(origin, cookie)
    }.toMutableList()

    private fun write(entries: List<Entry>) {
        storage.write(entries.map { "${it.origin}\n${it.cookie.expiresAt}\n${it.cookie}" }.toSet())
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val entries = read()
        cookies.forEach { cookie ->
            // An origin may only set cookies for itself or an enclosing domain.
            val validDomain = if (cookie.hostOnly) url.host == cookie.domain else
                url.host == cookie.domain || url.host.endsWith(".${cookie.domain}")
            if (!validDomain) return@forEach
            entries.removeAll {
                it.cookie.name == cookie.name && it.cookie.domain == cookie.domain &&
                    it.cookie.path == cookie.path
            }
            if (cookie.expiresAt > now()) entries.add(Entry(url, cookie))
        }
        write(entries)
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val entries = read()
        write(entries) // Prune expired or malformed records.
        return entries.map { it.cookie }.filter { it.matches(url) }.sortedByDescending { it.path.length }
    }

    @Synchronized
    fun setToken(token: String) {
        // JSON token fallback must never inherit the host of a future request.
        // Prefer a server-set cookie, which has authoritative expiry and scope.
        if (loadForRequest(backendUrl).any { it.name == "token" && it.value == token }) return
        val cookie = Cookie.Builder().name("token").value(token)
            .hostOnlyDomain(backendUrl.host).path("/").secure().httpOnly().build()
        saveFromResponse(backendUrl, listOf(cookie))
    }

    fun getToken(): String? = loadForRequest(backendUrl).firstOrNull { it.name == "token" }?.value

    @Synchronized
    fun clearCookies() = storage.write(emptySet())
}
