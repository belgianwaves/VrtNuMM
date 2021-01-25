package com.bw.vrtnumm.shared

import com.bw.vrtnumm.shared.utils.DebugLog
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.jsonx.JSONObject
import kotlin.time.ExperimentalTime

class TokenResolver(private val client: HttpClient, private val user: String, private val pass: String) {
    private val settings: Settings = Settings()

    companion object {
        const val API_KEY = "3_qhEcPa5JGFROVwu5SWKqJ4mVOIkwlFNMSKwzPDAh8QZOtHqu6L4nD5Q7lk0eXOOG"
        const val TOKEN_TIME_OUT = 1000*60*30
    }

    @ExperimentalTime
    suspend fun getPlayerToken(tokenUrl: String, tokenVariant: String? = null): String? {
        var vrtTokenVariant: String? = null

        val token = getCachedToken("vrtPlayerToken", tokenVariant)
        if (token != null) return token

        val headers =
            if ("ondemand" == tokenVariant) {
                val vrtToken = getVrtToken(vrtTokenVariant) ?: return null
                mapOf(
                    "Content-Type" to "application/json",
                    "Cookie" to "X-VRT-Token=$vrtToken"
                )
            } else {
                mapOf(
                    "Content-Type" to "application/json"
                )
            }
        return getNewPlayerToken(tokenUrl, headers, tokenVariant)
    }

    @ExperimentalTime
    suspend fun login(): String {
        val loginJson = getLoginJson()
        return getNewVrtToken(loginJson)
    }

    private suspend fun getLoginJson(): JSONObject {
        val params = listOf(
            "loginID" to user,
            "password" to pass,
            "sessionExpiration" to "-1",
            "APIKey" to API_KEY,
            "targetEnv" to "jssdk"
        )

        val payload: String = client.get("https://accounts.vrt.be/accounts.login") {
            params.forEach { kv ->
                parameter(kv.first, kv.second)
            }
        }
        return JSONObject(payload)
    }

    @ExperimentalTime
    private suspend fun getVrtToken(tokenVariant: String? = null): String? {
        val token = getCachedToken("X-VRT-Token", tokenVariant)
        if (token != null) return token

        return login()
    }

    @ExperimentalTime
    private suspend fun getNewVrtToken(loginJson: JSONObject): String {
        val loginToken = loginJson.getJSONObject("sessionInfo")["login_token"]

        val loginCookie = "glt_{$API_KEY}={$loginToken}"
        val headers = mapOf(
            "Content-Type" to "application/json",
            "Cookie" to loginCookie
        )

        val payload = LoginCreds(
            loginJson.getString("UID"),
            loginJson.getString("UIDSignature"),
            loginJson.getString("signatureTimestamp"),
            user
        )

        val response: HttpResponse = client.post("https://token.vrt.be") {
            headers.forEach { kv ->
                header(kv.key, kv.value)
            }
            body = payload
        }

        val cookieHeader = response.headers["Set-Cookie"]!!
            DebugLog.d("cookieHeader: $cookieHeader")
        val cookieData = createJSONToken(cookieHeader)
        return cookieData.getString("X-VRT-Token")
    }

    private suspend fun getNewPlayerToken(tokenUrl: String, headers: Map<String, String>, tokenVariant: String?): String? {
        val payload: String  = client.post(tokenUrl) {
            headers.forEach { kv ->
                header(kv.key, kv.value)
            }
        }

        val playerToken = JSONObject(payload)
            setCachedToken(playerToken, tokenVariant)
        return playerToken.getString("vrtPlayerToken")
    }

    private fun getTokenPath(tokenName: String, tokenVariant: String?): String {
        val prefix = if (tokenVariant != null) tokenVariant + "_" else ""
        return prefix + tokenName.replace("-", "")
    }

    private fun getCachedToken(tokenName: String, tokenVariant: String? = null): String? {
        val value = settings.getString(getTokenPath(tokenName, tokenVariant), "")
        if (value.isNullOrEmpty()) return null

        val json = JSONObject(value)
        val exp = try {
            json.getLong("expirationDate")
        } catch (e: Exception) {
            DebugLog.d("expirationDate: ${json.getString("expirationDate")}")
            Instant.parse(json.getString("expirationDate")).toEpochMilliseconds()
        }
        val now = Clock.System.now()
        if ((now.toEpochMilliseconds() - exp) > TOKEN_TIME_OUT) {
            settings.remove(getTokenPath(tokenName, tokenVariant))
            DebugLog.d("token expired: ${(now.toEpochMilliseconds() - exp)/(1000*60)} mins")
            return null
        }
        return null
    }

    private fun setCachedToken(token: JSONObject, tokenVariant: String? = null) {
        val tokenName = token.keys().next() as String
        settings.putString(getTokenPath(tokenName, tokenVariant), token.toString())
    }

    @ExperimentalTime
    private fun createJSONToken(cookieData: String, cookieName: String = "X-VRT-Token"): JSONObject {
        val cookie = HttpCookie.parse(cookieData)[0]
        return JSONObject().apply {
            put(cookieName, cookie.value)
            put("expirationDate", Clock.System.now().toEpochMilliseconds() + cookie.getMaxAge())
        }
        return JSONObject()
    }

    @Serializable
    private data class LoginCreds(
        val uid: String,
        val uidsig: String,
        val ts: String,
        val email: String
    )
}