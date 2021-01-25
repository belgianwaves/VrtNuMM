package com.bw.vrtnumm.shared

import co.touchlab.kermit.Logger
import com.bw.vrtnumm.shared.db.VrtNuDatabase
import io.ktor.client.HttpClient

expect fun createDb() : VrtNuDatabase

expect fun createHttpClient() : HttpClient

expect fun extractKid(payload: String): String?

expect fun getLogger(): Logger