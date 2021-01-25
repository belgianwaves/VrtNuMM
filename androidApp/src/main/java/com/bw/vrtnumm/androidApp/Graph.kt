package com.bw.vrtnumm.androidApp

import com.bw.vrtnumm.shared.Api
import com.bw.vrtnumm.shared.repository.Repository

object Graph {
    lateinit var api: Api
        private set
    val firebase: FirebaseSync? = if (BuildConfig.USE_FIREBASE) FirebaseSync() else null
    val repo: Repository = Repository()

    fun init(api: Api) {
        this.api = api
        repo.init(api)
    }
}