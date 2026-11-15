package org.youschool.app.core.update

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
data class GitHubAsset(
    @SerialName("name") val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = ""
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("body") val body: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("assets") val assets: List<GitHubAsset> = emptyList()
)

data class AppRelease(
    val versionName: String,
    val releaseUrl: String,
    val apkDownloadUrl: String?,
    val releaseNotes: String
)

object AppUpdateChecker {

    private const val TAG = "AppUpdateChecker"
    const val GITHUB_REPO_API = "https://api.github.com/repos/f02xygen/YouSchool/releases/latest"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    fun normalizeVersion(versionStr: String): List<Int> {
        val clean = versionStr.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")
        return clean.split(".")
            .mapNotNull { it.toIntOrNull() }
    }

    fun isNewerVersion(currentVersion: String, latestVersion: String): Boolean {
        val currentParts = normalizeVersion(currentVersion)
        val latestParts = normalizeVersion(latestVersion)
        val maxLen = maxOf(currentParts.size, latestParts.size)

        for (i in 0 until maxLen) {
            val c = currentParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "0.0.0"
        } catch (e: Exception) {
            "0.0.0"
        }
    }

    suspend fun checkForUpdate(
        context: Context,
        customCurrentVersion: String? = null
    ): AppRelease? = withContext(Dispatchers.IO) {
        try {
            val currentVersion = customCurrentVersion ?: getAppVersion(context)

            val request = Request.Builder()
                .url(GITHUB_REPO_API)
                .header("User-Agent", "YouSchool-Android-App")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "GitHub release check failed with code: ${response.code}")
                    return@withContext null
                }

                val bodyStr = response.body?.string() ?: return@withContext null
                val release = json.decodeFromString<GitHubRelease>(bodyStr)

                val latestTag = release.tagName.ifBlank { release.name }
                if (latestTag.isBlank()) return@withContext null

                if (isNewerVersion(currentVersion, latestTag)) {
                    val apkAsset = release.assets.firstOrNull {
                        it.name.endsWith(".apk", ignoreCase = true)
                    }
                    val targetUrl = release.htmlUrl.ifBlank {
                        "https://github.com/f02xygen/YouSchool/releases"
                    }

                    Log.d(TAG, "Found new version: $latestTag (current: $currentVersion)")
                    return@withContext AppRelease(
                        versionName = latestTag.removePrefix("v").removePrefix("V"),
                        releaseUrl = targetUrl,
                        apkDownloadUrl = apkAsset?.browserDownloadUrl,
                        releaseNotes = release.body
                    )
                } else {
                    Log.d(TAG, "App is up to date: $currentVersion >= $latestTag")
                    return@withContext null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates: ${e.message}")
            return@withContext null
        }
    }
}
