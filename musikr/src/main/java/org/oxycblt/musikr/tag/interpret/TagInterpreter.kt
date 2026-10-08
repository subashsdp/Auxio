/*
 * Copyright (c) 2024 Auxio Project
 * TagInterpreter.kt is part of Auxio.
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
 
package org.oxycblt.musikr.tag.interpret

import org.oxycblt.musikr.Interpretation
import org.oxycblt.musikr.Music
import org.oxycblt.musikr.fs.File
import org.oxycblt.musikr.fs.Format
import org.oxycblt.musikr.pipeline.RawSong
import org.oxycblt.musikr.tag.Disc
import org.oxycblt.musikr.tag.Name
import org.oxycblt.musikr.tag.Placeholder
import org.oxycblt.musikr.tag.ReleaseType
import org.oxycblt.musikr.tag.ReplayGainAdjustment
import org.oxycblt.musikr.tag.format.parseId3GenreNames
import org.oxycblt.musikr.tag.parse.ParsedTags
import org.oxycblt.musikr.util.toUuidOrNull
import org.oxycblt.musikr.util.update

internal interface TagInterpreter {
    fun interpret(song: RawSong): PreSong

    companion object {
        fun new(interpretation: Interpretation): TagInterpreter = TagInterpreterImpl(interpretation)
    }
}

private class TagInterpreterImpl(private val interpretation: Interpretation) : TagInterpreter {
    override fun interpret(song: RawSong): PreSong {
        val individualPreArtists =
            makePreArtists(
                song.tags.artistMusicBrainzIds,
                song.tags.artistNames,
                song.tags.artistSortNames,
                interpretation,
            )
        val albumPreArtists =
            makePreArtists(
                song.tags.albumArtistMusicBrainzIds,
                song.tags.albumArtistNames,
                song.tags.albumArtistSortNames,
                interpretation,
            )
        val preAlbum =
            makePreAlbum(
                song.tags,
                song.file,
                individualPreArtists,
                albumPreArtists,
                interpretation,
            )
        val rawArtists =
            individualPreArtists.ifEmpty { albumPreArtists }.ifEmpty { listOf(unknownPreArtist()) }
        val rawGenres =
            makePreGenres(song.tags, interpretation).ifEmpty { listOf(unknownPreGenre()) }
        val uri = song.file.uri

        val rawSongName = song.tags.name ?: requireNotNull(song.file.path.name).substringBeforeLast(".")
        val songNameOrFileWithoutExtCorrect = MetadataSanitizer.cleanTitle(rawSongName)
        val rawAlbumName = song.tags.albumName ?: song.file.path.directory.name
        val albumNameOrDir = MetadataSanitizer.cleanAlbum(rawAlbumName)

        val musicBrainzId = song.tags.musicBrainzId?.toUuidOrNull()
        val v363uid =
            musicBrainzId?.let { Music.UID.musicBrainz(Music.UID.Item.SONG, it) }
                ?: Music.UID.auxio(Music.UID.Item.SONG) {
                    update(songNameOrFileWithoutExtCorrect)
                    update(albumNameOrDir)
                    update(song.tags.date)

                    update(song.tags.track)
                    update(song.tags.disc)

                    update(song.tags.artistNames)
                    update(song.tags.albumArtistNames)
                }

        val v400uid =
            musicBrainzId?.let { Music.UID.musicBrainz(Music.UID.Item.SONG, it) }
                ?: Music.UID.auxio(Music.UID.Item.SONG) {
                    update(songNameOrFileWithoutExtCorrect)
                    update(albumNameOrDir)
                    update(song.tags.date)

                    update(song.tags.track)
                    update(song.tags.disc)

                    val artistNames = interpretation.separators.split(song.tags.artistNames)
                    update(artistNames.ifEmpty { listOf(null) })
                    val albumArtistNames =
                        interpretation.separators.split(song.tags.albumArtistNames)
                    update(albumArtistNames.ifEmpty { artistNames }.ifEmpty { listOf(null) })
                }

        val v401uid =
            musicBrainzId?.let { Music.UID.musicBrainz(Music.UID.Item.SONG, it) }
                ?: Music.UID.auxio(Music.UID.Item.SONG) {
                    update(songNameOrFileWithoutExtCorrect)
                    update(albumNameOrDir)
                    update(song.tags.date)

                    update(song.tags.track)
                    update(song.tags.disc)

                    update(song.tags.artistNames)
                    update(song.tags.albumArtistNames)
                }

        return PreSong(
            v363Uid = v363uid,
            v400Uid = v400uid,
            v401Uid = v401uid,
            uri = uri,
            path = song.file.path,
            size = song.file.size,
            format = Format.infer(song.file.mimeType, song.properties.mimeType),
            modifiedMs = song.file.modifiedMs,
            addedMs = song.addedMs,
            musicBrainzId = musicBrainzId,
            name = interpretation.naming.name(songNameOrFileWithoutExtCorrect, song.tags.sortName),
            rawName = songNameOrFileWithoutExtCorrect,
            track = song.tags.track,
            disc = song.tags.disc?.let { Disc(it, song.tags.subtitle) },
            date = song.tags.date,
            durationMs = song.tags.durationMs,
            bitrateKbps = song.properties.bitrateKbps,
            sampleRateHz = song.properties.sampleRateHz,
            replayGainAdjustment =
                ReplayGainAdjustment(
                    song.tags.replayGainTrackAdjustment,
                    song.tags.replayGainAlbumAdjustment,
                ),
            preAlbum = preAlbum,
            preArtists = rawArtists,
            preGenres = rawGenres,
            cover = song.cover,
        )
    }

    private fun makePreAlbum(
        parsedTags: ParsedTags,
        deviceFile: File,
        individualPreArtists: List<PreArtist>,
        albumPreArtists: List<PreArtist>,
        interpretation: Interpretation,
    ): PreAlbum {
        val rawName = parsedTags.albumName ?: deviceFile.path.directory.name
        val name = MetadataSanitizer.cleanAlbum(rawName)
        return PreAlbum(
            musicBrainzId = parsedTags.albumMusicBrainzId?.toUuidOrNull(),
            name = interpretation.naming.name(name, parsedTags.albumSortName, Placeholder.ALBUM),
            rawName = name,
            releaseType =
                ReleaseType.parse(interpretation.separators.split(parsedTags.releaseTypes))
                    ?: ReleaseType.Album(null),
            preArtists =
                PreArtistsFrom.Album(albumPreArtists).takeIf { it.preArtists.isNotEmpty() }
                    ?: PreArtistsFrom.Individual(
                        individualPreArtists.ifEmpty { listOf(unknownPreArtist()) }
                    ),
        )
    }

    private fun makePreArtists(
        rawMusicBrainzIds: List<String>,
        rawNames: List<String>,
        rawSortNames: List<String>,
        interpretation: Interpretation,
    ): List<PreArtist> {
        val musicBrainzIds = interpretation.separators.split(rawMusicBrainzIds)
        val splitNames = interpretation.separators.split(rawNames)
        val names = MetadataSanitizer.splitAndSanitizeArtists(splitNames)
        val sortNames = interpretation.separators.split(rawSortNames)
        return names.mapIndexed { i, name ->
            makePreArtist(musicBrainzIds.getOrNull(i), name, sortNames.getOrNull(i), interpretation)
        }
    }

    private fun makePreArtist(
        musicBrainzId: String?,
        rawName: String?,
        sortName: String?,
        interpretation: Interpretation,
    ): PreArtist {
        val cleanName = rawName?.let { MetadataSanitizer.normalizeArtist(it) } ?: rawName
        val name = interpretation.naming.name(cleanName, sortName, Placeholder.ARTIST)
        val musicBrainzId = musicBrainzId?.toUuidOrNull()
        return PreArtist(musicBrainzId, name, cleanName)
    }

    private fun unknownPreArtist() = PreArtist(null, Name.Unknown(Placeholder.ARTIST), null)

    private fun makePreGenres(
        parsedTags: ParsedTags,
        interpretation: Interpretation,
    ): List<PreGenre> {
        val genreNames =
            parsedTags.genreNames.parseId3GenreNames()
                ?: interpretation.separators.split(parsedTags.genreNames)
        return genreNames.map { makePreGenre(it, interpretation) }
    }

    private fun makePreGenre(rawName: String?, interpretation: Interpretation) =
        PreGenre(interpretation.naming.name(rawName, null, Placeholder.GENRE), rawName)

    private fun unknownPreGenre() = PreGenre(Name.Unknown(Placeholder.GENRE), null)
}
