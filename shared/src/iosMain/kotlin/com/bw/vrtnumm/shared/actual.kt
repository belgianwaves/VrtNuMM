package com.bw.vrtnumm.shared

import co.touchlab.kermit.Logger
import co.touchlab.kermit.NSLogLogger
import com.bw.vrtnumm.shared.db.VrtNuDatabase
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*

actual fun createDb(): VrtNuDatabase {
    val driver = NativeSqliteDriver(VrtNuDatabase.Schema, "vrtnu.db")
    return VrtNuDatabase(driver)
}

actual fun createHttpClient(): HttpClient {
    return HttpClient() {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            logger = CustomHttpLogger()
            level = if (true) LogLevel.INFO else LogLevel.NONE
        }
    }
}

actual fun extractKid(payload: String): String? {
    throw IllegalStateException("not implemented")
}

actual fun getLogger(): Logger = NSLogLogger()