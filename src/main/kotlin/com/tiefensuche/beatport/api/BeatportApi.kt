/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.tiefensuche.beatport.api

import com.tiefensuche.beatport.api.Constants.ACCESS_TOKEN
import com.tiefensuche.beatport.api.Constants.LOCATION
import com.tiefensuche.beatport.api.Constants.REFRESH_TOKEN
import com.tiefensuche.beatport.api.Constants.SUBSCRIPTION
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.collections.HashMap

class BeatportApi(val session: Session) {

    class Session(val clientId: String, val redirectUri: String, val callback: ((session: Session) -> Unit)?) {
        var username: String? = null
        var password: String? = null
        var accessToken: String? = null
        var refreshToken: String? = null
        val nextQueryUrls: HashMap<String, String> = HashMap()
        private var hasSubscription: Boolean? = null

        fun getAccessToken() {
            if (refreshToken != null) {
                try {
                    val res = WebRequests.post(
                        Endpoints.TOKEN_BASE,
                        "grant_type=refresh_token&refresh_token=${refreshToken}&client_id=${clientId}"
                    )
                    if (res.status == 200) {
                        val json = JSONObject(res.value)
                        accessToken = json.getString(ACCESS_TOKEN)
                        refreshToken = json.getString(REFRESH_TOKEN)
                        hasSubscription = hasSubscription()
                        callback?.let { it(this) }
                        return
                    }
                } catch (e: WebRequests.HttpException) {
                    // refresh token expired or invalid, continue with standard login
                }
            }

            if (username == null || password == null)
                throw InvalidCredentialsException("Username and/or password missing!")

            var con = WebRequests.createConnection(Endpoints.LOGIN)
            con.setRequestProperty("content-type", "application/json")
            con.requestMethod = "POST"
            con.doOutput = true
            con.outputStream.write("{\"username\":\"$username\",\"password\":\"$password\"}".toByteArray())
            if (con.responseCode != 200)
                throw InvalidCredentialsException("Could not login!")

            val cookie = con.getHeaderField("Set-Cookie")
            con = WebRequests.createConnection(Endpoints.AUTH.format(clientId, redirectUri))
            con.setRequestProperty("Cookie", cookie)
            if (con.responseCode != 302)
                throw Exception("Could not get access token!")

            val code = con.getHeaderField(LOCATION).substringAfter("=").substringBefore("&")
            val json = JSONObject(
                WebRequests.post(
                    Endpoints.TOKEN.format(code, clientId, redirectUri),
                    ""
                ).value
            )
            accessToken = json.getString(ACCESS_TOKEN)
            refreshToken = json.getString(REFRESH_TOKEN)
            hasSubscription = hasSubscription()
            callback?.let { it(this) }
        }

        fun hasSubscription(): Boolean {
            return hasSubscription ?: run {
                checkSubscription().let {
                    hasSubscription = it
                    it
                }
            }
        }

        private fun checkSubscription(): Boolean {
            val result = Requests.ActionRequest(this, Endpoints.INTROSPECT).execute()
            if (result.status == 200) {
                return !JSONObject(result.value).isNull(SUBSCRIPTION)
            }
            return false
        }
    }

    fun getGenres(reset: Boolean): List<Genre> {
        return parseGenresFromJSONArray(
            Requests.CollectionRequest(session, Endpoints.GENRES, reset).execute()
        )
    }

    fun getTracks(genre: String, reset: Boolean): List<Track> {
        return getTracks(Endpoints.TRACKS, genre, reset)
    }

    fun getTop100(genre: String, reset: Boolean): List<Track> {
        return getTracks(Endpoints.TOP100, genre, reset)
    }

    fun getTracks(
        type: Requests.CollectionEndpoint,
        arg: String,
        reset: Boolean
    ): List<Track> {
        return parseTracksFromJSONArray(
            Requests.CollectionRequest(
                session,
                type, reset, arg
            ).execute()
        )
    }

    fun query(query: String, reset: Boolean): List<Track> {
        val trackList = JSONArray()
        val tracks = Requests.CollectionRequest(
            session,
            Endpoints.QUERY_URL,
            reset,
            URLEncoder.encode(query, "UTF-8")
        ).execute()
        for (j in 0 until tracks.length()) {
            trackList.put(tracks.getJSONObject(j))
        }
        return parseTracksFromJSONArray(trackList)
    }

    fun getMyPlaylists(reset: Boolean): List<Playlist> {
        return getPlaylists(Endpoints.PLAYLISTS, "", reset)
    }

    fun getPlaylist(id: String, reset: Boolean): List<Track> {
        return getPlaylist(id, Endpoints.PLAYLIST_TRACKS, reset)
    }

    fun getCuratedPlaylists(path: String, reset: Boolean): List<Playlist> {
        return getPlaylists(Endpoints.CURATED, path, reset)
    }

    fun getCuratedPlaylist(path: String, reset: Boolean): List<Track> {
        return getPlaylist(
            path,
            Endpoints.CURATED_TRACKS,
            reset
        )
    }

    fun getPlaylists(
        endpoint: Requests.CollectionEndpoint,
        path: String,
        reset: Boolean
    ): List<Playlist> {
        val playlists = Requests.CollectionRequest(session, endpoint, reset, path).execute()
        val result = mutableListOf<Playlist>()
        for (i in 0 until playlists.length()) {
            val playlist = playlists.getJSONObject(i)
            result.add(Playlist(playlist.getInt(Constants.ID), playlist.getString(Constants.NAME)))
        }
        return result
    }

    fun getPlaylist(
        id: String,
        endpoint: Requests.CollectionEndpoint,
        reset: Boolean
    ): List<Track> {
        val tracks = Requests.CollectionRequest(session, endpoint, reset, id).execute()
        val result = mutableListOf<Track>()
        for (i in 0 until tracks.length()) {
            val track = buildTrackFromJSON(tracks.getJSONObject(i).getJSONObject(Constants.TRACK))
            result.add(track)
        }
        return result
    }

    private fun parseGenresFromJSONArray(genres: JSONArray): List<Genre> {
        val result = mutableListOf<Genre>()
        for (i in 0 until genres.length()) {
            result.add(buildGenreFromJSON(genres.getJSONObject(i)))
        }
        return result
    }

    private fun parseTracksFromJSONArray(tracks: JSONArray): List<Track> {
        val result = mutableListOf<Track>()
        for (i in 0 until tracks.length()) {
            result.add(buildTrackFromJSON(tracks.getJSONObject(i)))
        }
        return result
    }

    private fun buildTrackFromJSON(json: JSONObject): Track {
        return Track(
            json.getLong(Constants.ID),
            json.getJSONArray(Constants.ARTISTS).getJSONObject(0).getString(Constants.NAME),
            json.getJSONObject(Constants.RELEASE).getString(Constants.NAME),
            if (session.hasSubscription()) json.getLong(Constants.LENGTH_MS) else json.getLong(Constants.LENGTH_MS)
                .coerceAtMost(2 * 60 * 1000),
            json.getJSONObject(Constants.RELEASE).getJSONObject(Constants.IMAGE).getString(Constants.URI),
            json.getString(Constants.SAMPLE_URL)
        )
    }

    private fun buildGenreFromJSON(json: JSONObject): Genre {
        return Genre(json.getInt(Constants.ID), json.getString(Constants.NAME), json.getString(Constants.URL))
    }

    fun getStreamUrl(id: String): String {
        val res = Requests.ActionRequest(session, Endpoints.STREAM_URL, data = null, id).execute()
        if (res.status == 200) {
            return JSONObject(res.value).getString(LOCATION)
        }
        throw NotStreamableException("Can not get stream url")
    }

    // Exception types
    class InvalidCredentialsException(message: String) : Exception(message)
    class NotStreamableException(message: String) : Exception(message)
}