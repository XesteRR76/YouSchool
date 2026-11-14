package org.youschool.app.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.youschool.app.core.auth.AuthManager
import org.youschool.app.core.network.dto.DiaryResponse
import org.youschool.app.core.network.dto.GradesResponse
import org.youschool.app.core.network.dto.PeriodContainerDto
import org.youschool.app.core.network.dto.StateResponse
import java.io.IOException
import java.util.concurrent.TimeUnit

class EschoolApiClient(private val authManager: AuthManager) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val baseUrl = "https://app.eschool.center/ec-server"

    suspend fun login(username: String, passwordHash: String): Result<StateResponse> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("username", username)
                .add("password", passwordHash)
                .build()

            val request = Request.Builder()
                .url("$baseUrl/login")
                .post(formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Ошибка авторизации: код ${response.code}"))
            }

            var sessionId: String? = null
            var route: String? = null

            val cookies = response.headers("Set-Cookie")
            for (cookie in cookies) {
                if (cookie.contains("JSESSIONID=")) {
                    sessionId = cookie.substringAfter("JSESSIONID=").substringBefore(";")
                }
                if (cookie.contains("route=")) {
                    route = cookie.substringAfter("route=").substringBefore(";")
                }
            }

            if (sessionId == null) {
                return@withContext Result.failure(IOException("Не удалось получить сессионный токен"))
            }

            authManager.saveSession(sessionId, route)

            // Запрашиваем состояние пользователя
            val stateResult = getStateInternal(sessionId, route)
            stateResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildCookieHeader(sessionId: String, route: String?): String {
        return buildString {
            append("JSESSIONID=$sessionId; site_ver=app")
            if (!route.isNullOrBlank()) {
                append("; route=$route")
            }
        }
    }

    private suspend fun getStateInternal(sessionId: String, route: String?): Result<StateResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/state?menu=false")
                .get()
                .addHeader("Cookie", buildCookieHeader(sessionId, route))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext Result.failure(IOException("Пустой ответ профиля"))
            val state = json.decodeFromString<StateResponse>(body)
            Result.success(state)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun <T> executeWithAutoAuth(block: suspend (sessionId: String, route: String?) -> Result<T>): Result<T> {
        var sessionId = authManager.getSessionId()
        var route = authManager.getRoute()

        if (sessionId.isNullOrBlank()) {
            val login = authManager.getLogin()
            val hash = authManager.getPasswordHash()
            if (!login.isNullOrBlank() && !hash.isNullOrBlank()) {
                val loginResult = login(login, hash)
                if (loginResult.isFailure) {
                    return Result.failure(loginResult.exceptionOrNull() ?: IOException("Ошибка входа"))
                }
                sessionId = authManager.getSessionId()
                route = authManager.getRoute()
            } else {
                return Result.failure(IOException("Пользователь не авторизован"))
            }
        }

        val firstAttempt = block(sessionId!!, route)
        if (firstAttempt.isFailure && firstAttempt.exceptionOrNull()?.message?.contains("401") == true) {
            // Re-login on 401
            val login = authManager.getLogin()
            val hash = authManager.getPasswordHash()
            if (!login.isNullOrBlank() && !hash.isNullOrBlank()) {
                val loginResult = login(login, hash)
                if (loginResult.isSuccess) {
                    return block(authManager.getSessionId()!!, authManager.getRoute())
                }
            }
        }
        return firstAttempt
    }

    suspend fun getDiary(userId: Long, startMs: Long, endMs: Long): Result<DiaryResponse> {
        return executeWithAutoAuth { sessionId, route ->
            try {
                val request = Request.Builder()
                    .url("$baseUrl/student/diary?userId=$userId&d1=$startMs&d2=$endMs")
                    .get()
                    .addHeader("Cookie", buildCookieHeader(sessionId, route))
                    .build()

                val response = client.newCall(request).execute()
                if (response.code == 401) return@executeWithAutoAuth Result.failure(IOException("401 Unauthorized"))
                val body = response.body?.string() ?: ""
                val diary = json.decodeFromString<DiaryResponse>(body)
                Result.success(diary)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getGradesForPeriod(userId: Long, periodId: Long): Result<GradesResponse> {
        return executeWithAutoAuth { sessionId, route ->
            try {
                val request = Request.Builder()
                    .url("$baseUrl/student/getDiaryPeriod?userId=$userId&eiId=$periodId")
                    .get()
                    .addHeader("Cookie", buildCookieHeader(sessionId, route))
                    .build()

                val response = client.newCall(request).execute()
                if (response.code == 401) return@executeWithAutoAuth Result.failure(IOException("401 Unauthorized"))
                val body = response.body?.string() ?: ""
                val grades = json.decodeFromString<GradesResponse>(body)
                Result.success(grades)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getPeriods(year: Int): Result<List<PeriodContainerDto>> {
        return executeWithAutoAuth { sessionId, route ->
            try {
                val request = Request.Builder()
                    .url("$baseUrl/dict/periods2?year=$year")
                    .get()
                    .addHeader("Cookie", buildCookieHeader(sessionId, route))
                    .build()

                val response = client.newCall(request).execute()
                if (response.code == 401) return@executeWithAutoAuth Result.failure(IOException("401 Unauthorized"))
                val body = response.body?.string() ?: ""
                val periods = json.decodeFromString<List<PeriodContainerDto>>(body)
                Result.success(periods)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
