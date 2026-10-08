/*
 * Copyright (c) 2024 Auxio Project
 * PlayCountTracker.kt is part of Auxio.
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
import androidx.core.content.edit
import org.oxycblt.musikr.Library
import org.oxycblt.musikr.Music
import org.oxycblt.musikr.Song
import timber.log.Timber as L

/**
 * Tracks play counts and play history timestamps for songs to power the "Most Played"
 * smart playlist and listening statistics.
 */
object PlayCountTracker {
    private const val PREFS_NAME = "auxio_play_stats"
    private const val KEY_PLAY_COUNT_PREFIX = "count_"
    private const val KEY_LAST_PLAYED_PREFIX = "last_"

    /**
     * Increment play count for a song and update its last played timestamp.
     */
    fun recordPlay(context: Context, song: Song) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val countKey = KEY_PLAY_COUNT_PREFIX + song.uid.toString()
        val lastKey = KEY_LAST_PLAYED_PREFIX + song.uid.toString()
        val currentCount = prefs.getInt(countKey, 0)
        val newCount = currentCount + 1
        val now = System.currentTimeMillis()

        prefs.edit {
            putInt(countKey, newCount)
            putLong(lastKey, now)
        }
        L.d("Recorded play for ${song.name.raw}: count=$newCount")
    }

    /**
     * Get the play count for a specific song UID.
     */
    fun getPlayCount(context: Context, uid: Music.UID): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_PLAY_COUNT_PREFIX + uid.toString(), 0)
    }

    /**
     * Return the top played songs in the library.
     */
    fun getTopPlayedSongs(context: Context, library: Library, limit: Int = 50): List<Song> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allEntries = prefs.all

        val scoredSongs = mutableListOf<Triple<Song, Int, Long>>()
        for ((key, value) in allEntries) {
            if (key.startsWith(KEY_PLAY_COUNT_PREFIX) && value is Int && value > 0) {
                val uidString = key.removePrefix(KEY_PLAY_COUNT_PREFIX)
                val uid = Music.UID.fromString(uidString) ?: continue
                val song = library.findSong(uid) ?: continue
                val lastPlayed = prefs.getLong(KEY_LAST_PLAYED_PREFIX + uidString, 0L)
                scoredSongs.add(Triple(song, value, lastPlayed))
            }
        }

        // Sort primarily by play count (descending), secondarily by last played time (descending)
        scoredSongs.sortWith(
            compareByDescending<Triple<Song, Int, Long>> { it.second }
                .thenByDescending { it.third }
        )

        return scoredSongs.take(limit).map { it.first }
    }

    /**
     * Return recently added songs in the library.
     */
    fun getRecentlyAddedSongs(library: Library, limit: Int = 50): List<Song> {
        return library.songs
            .sortedByDescending { maxOf(it.addedMs, it.modifiedMs) }
            .take(limit)
    }
}
