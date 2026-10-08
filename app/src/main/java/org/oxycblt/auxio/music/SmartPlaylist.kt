/*
 * Copyright (c) 2024 Auxio Project
 * SmartPlaylist.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.oxycblt.auxio.music

import android.content.Context
import org.oxycblt.auxio.R
import org.oxycblt.musikr.Library
import org.oxycblt.musikr.Music
import org.oxycblt.musikr.Playlist
import org.oxycblt.musikr.Song
import org.oxycblt.musikr.covers.CoverCollection
import org.oxycblt.musikr.tag.Name
import org.oxycblt.musikr.tag.interpret.Naming

/**
 * A virtual, dynamic playlist generated from library metadata (e.g. Most Played, Recently Added).
 */
class SmartPlaylist(
    override val uid: Music.UID,
    override val name: Name.Known,
    override val songs: List<Song>,
) : Playlist {
    override val durationMs: Long = songs.sumOf { it.durationMs }
    override val covers: CoverCollection = CoverCollection.from(songs.mapNotNull { it.cover })

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Playlist) return false
        return uid == other.uid
    }

    override fun hashCode(): Int = uid.hashCode()

    override fun toString(): String = "SmartPlaylist(name=${name.raw}, count=${songs.size})"
}

object SmartPlaylistManager {
    val RECENTLY_ADDED_UID: Music.UID =
        requireNotNull(Music.UID.fromString("uap00000000-0000-0000-0000-000000000101")) {
            "Failed to parse RECENTLY_ADDED_UID"
        }

    val MOST_PLAYED_UID: Music.UID =
        requireNotNull(Music.UID.fromString("uap00000000-0000-0000-0000-000000000102")) {
            "Failed to parse MOST_PLAYED_UID"
        }

    fun isSmartPlaylist(uid: Music.UID): Boolean {
        return uid == RECENTLY_ADDED_UID || uid == MOST_PLAYED_UID
    }

    fun isSmartPlaylist(playlist: Playlist): Boolean {
        return isSmartPlaylist(playlist.uid)
    }

    fun getSmartPlaylists(context: Context, library: Library): List<Playlist> {
        if (library.empty()) return emptyList()

        val playlists = mutableListOf<Playlist>()
        val naming = Naming.simple()

        // 1. Recently Added
        val recentlyAddedSongs = PlayCountTracker.getRecentlyAddedSongs(library, 50)
        if (recentlyAddedSongs.isNotEmpty()) {
            playlists.add(
                SmartPlaylist(
                    uid = RECENTLY_ADDED_UID,
                    name = naming.name(context.getString(R.string.lbl_recently_added), null),
                    songs = recentlyAddedSongs,
                )
            )
        }

        // 2. Most Played
        val mostPlayedSongs = PlayCountTracker.getTopPlayedSongs(context, library, 50)
        if (mostPlayedSongs.isNotEmpty()) {
            playlists.add(
                SmartPlaylist(
                    uid = MOST_PLAYED_UID,
                    name = naming.name(context.getString(R.string.lbl_most_played), null),
                    songs = mostPlayedSongs,
                )
            )
        }

        return playlists
    }

    fun findSmartPlaylist(context: Context, library: Library?, uid: Music.UID): Playlist? {
        if (library == null || library.empty()) return null
        val naming = Naming.simple()
        return when (uid) {
            RECENTLY_ADDED_UID -> {
                val songs = PlayCountTracker.getRecentlyAddedSongs(library, 50)
                SmartPlaylist(
                    uid = RECENTLY_ADDED_UID,
                    name = naming.name(context.getString(R.string.lbl_recently_added), null),
                    songs = songs,
                )
            }
            MOST_PLAYED_UID -> {
                val songs = PlayCountTracker.getTopPlayedSongs(context, library, 50)
                SmartPlaylist(
                    uid = MOST_PLAYED_UID,
                    name = naming.name(context.getString(R.string.lbl_most_played), null),
                    songs = songs,
                )
            }
            else -> null
        }
    }
}
