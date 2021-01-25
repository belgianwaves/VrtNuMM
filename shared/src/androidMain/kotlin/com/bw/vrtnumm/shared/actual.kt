package com.bw.vrtnumm.shared

import android.content.Context
import co.touchlab.kermit.LogcatLogger
import co.touchlab.kermit.Logger
import com.bw.vrtnumm.shared.db.VrtNuDatabase
import com.squareup.sqldelight.android.AndroidSqliteDriver
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

lateinit var appContext: Context

actual fun createDb(): VrtNuDatabase {
    val driver = AndroidSqliteDriver(VrtNuDatabase.Schema, appContext, "vrtnu.db")
    return VrtNuDatabase(driver)
}

actual fun createHttpClient(): HttpClient {
    return HttpClient(CIO) {
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
    val builderFactory = DocumentBuilderFactory.newInstance()
    val docBuilder = builderFactory.newDocumentBuilder()
    val doc = docBuilder.parse(InputSource(StringReader(payload)))
    val list = doc.getElementsByTagName("ContentProtection")
    for (j in 0 until list.length) {
        val element = list.item(j) as Element
        val k = element.getAttribute("cenc:default_KID")
        if (!k.isNullOrEmpty()) {
            return k
        }
    }
    return null
}

actual fun getLogger(): Logger = LogcatLogger()