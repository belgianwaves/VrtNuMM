package com.bw.vrtnumm.androidApp

import com.bw.vrtnumm.shared.db.Position
import com.bw.vrtnumm.shared.db.Program
import com.bw.vrtnumm.shared.repository.Repository
import com.bw.vrtnumm.shared.utils.DebugLog
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirebaseSync() {
    private val db = Firebase.firestore

    private lateinit var repo: Repository
    private var positionChangeListener: ListenerRegistration? = null
    private var favouriteChangeListener: ListenerRegistration? = null

    fun sync(repo: Repository) {
        this.repo = repo

        restorePositions()
        addPositionChangeListener()
        restoreFavourites()
        addFavouriteChangeListener()
    }

    fun storePosition(position: Position) {
        val self = repo.getCurrentUser()
        self?.apply {
            val data = hashMapOf(
                "title" to position.title,
                "position" to position.position,
                "duration" to position.duration,
                "lastSeen" to position.lastSeen
            )
            val id = "${position.programUrl}:${position.publicationId}:${position.videoId}".replace("/", "|")

            db.collection("users").document(email).collection("positions").document(id).set(data)
                .addOnSuccessListener { DebugLog.d("position $id successfully written") }
                .addOnFailureListener { e -> DebugLog.e("failed writing position $id", e) }
        }
    }

    private fun restorePositions() {
        val self = repo.getCurrentUser()
        self?.apply {
            db.collection("users").document(email).collection("positions").get()
                .addOnSuccessListener { documents ->
                    documents.map { d -> storePosition(d) }
                }
                .addOnFailureListener { e -> DebugLog.e("failed getting positions", e) }
        }
    }

    private fun addPositionChangeListener() {
        val self = repo.getCurrentUser()
        self?.apply {
            positionChangeListener?.apply {
                remove()
            }
            positionChangeListener = db.collection("users").document(email).collection("positions").addSnapshotListener { data, e ->
                if (e != null) {
                    DebugLog.e("failed getting snapshot", e)
                    return@addSnapshotListener
                }

                val local = (data != null) && data.metadata.hasPendingWrites()
                if (local) {
                    return@addSnapshotListener
                }

                data!!.documentChanges.map { dc -> storePosition(dc.document) }
            }
        }
    }

    fun storeFavourite(program: Program, favourite: Boolean) {
        val self = repo.getCurrentUser()
        self?.apply {
            val data = hashMapOf(
                "title" to program.title,
                "description" to program.desc,
                "thumbnail" to program.thumbnail,
                "favourite" to favourite
            )

            val id = program.programUrl.replace("/", "|")

            db.collection("users").document(email).collection("favourites").document(id).set(data)
                .addOnSuccessListener { DebugLog.d("favourite $id successfully written") }
                .addOnFailureListener { e -> DebugLog.e("failed writing favourite $id", e) }
        }
    }

    private fun restoreFavourites() {
        val self = repo.getCurrentUser()
        self?.apply {
            db.collection("users").document(email).collection("favourites").get()
                .addOnSuccessListener { documents ->
                    documents.map { d -> storeProgram(d) }
                }
                .addOnFailureListener { e -> DebugLog.e("failed getting favourites", e) }
        }
    }

    private fun addFavouriteChangeListener() {
        val self = repo.getCurrentUser()
        self?.apply {
            favouriteChangeListener?.apply {
                remove()
            }
            favouriteChangeListener = db.collection("users").document(email).collection("favourites").addSnapshotListener { data, e ->
                if (e != null) {
                    DebugLog.e("failed getting snapshot", e)
                    return@addSnapshotListener
                }

                val local = (data != null) && data.metadata.hasPendingWrites()
                if (local) {
                    return@addSnapshotListener
                }

                data!!.documentChanges.map { dc -> storeProgram(dc.document) }
            }
        }
    }

    private fun storePosition(document: QueryDocumentSnapshot) {
        val id = document.id.replace("|", "/")
        val (programUrl, publicationId, videoId) = id.split(":")

        var position = repo.getPosition(programUrl, publicationId, videoId)
        if (position == null) {
            repo.insertPosition(programUrl, document.getString("title")!!, publicationId, videoId)
        }
        repo.updatePosition(programUrl, publicationId, videoId, document.getLong("position")!!, document.getLong("duration")!!, document.getLong("lastSeen")!!)
    }

    private fun storeProgram(document: QueryDocumentSnapshot) {
        val programUrl = document.id.replace("|", "/")

        var program = repo.getProgram(programUrl)
        if (program == null) {
            repo.insertProgram(programUrl, document.getString("title")!!, document.getString("description")!!, document.getString("thumbnail")!!)
        }
        repo.setProgramFavourite(programUrl, document.getBoolean("favourite")!!)
    }
}