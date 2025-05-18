/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.tiefensuche.beatport.api

data class Track(
    val id: Long,
    val artist: String,
    val title: String,
    val duration: Long,
    val artwork: String,
    val url: String
)

data class Playlist(
    val uuid: Int,
    val title: String
)

data class Genre(
    val id: Int,
    val name: String,
    val url: String
)