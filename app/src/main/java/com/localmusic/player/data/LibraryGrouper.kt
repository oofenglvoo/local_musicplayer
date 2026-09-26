package com.localmusic.player.data

import androidx.annotation.StringRes
import com.localmusic.player.R
import com.localmusic.player.data.db.SongEntity

data class AlbumGroup(
    val key: String,
    val album: String,
    val artist: String,
    val year: Int,
    val songs: List<SongEntity>,
) {
    val albumId: Long get() = songs.firstOrNull()?.albumId ?: 0L
    val artworkPath: String? get() = songs.firstOrNull { it.artworkPath != null }?.artworkPath
    val representative: SongEntity? get() = songs.firstOrNull()
}

data class ArtistGroup(
    val artist: String,
    val songs: List<SongEntity>,
) {
    val albumCount: Int get() = songs.map { it.album }.distinct().size
    val artworkPath: String? get() = songs.firstOrNull { it.artworkPath != null }?.artworkPath
    val representative: SongEntity? get() = songs.firstOrNull()
}

data class FolderGroup(
    val path: String,
    val name: String?,
    val songs: List<SongEntity>,
)

enum class SongSort(@StringRes val labelRes: Int) {
    TITLE(R.string.sort_title),
    ARTIST(R.string.sort_artist),
    ALBUM(R.string.sort_album),
    DURATION(R.string.sort_duration),
    DATE_ADDED(R.string.sort_date_added),
    YEAR(R.string.sort_year),
    PLAY_COUNT(R.string.sort_play_count),
}

object LibraryGrouper {

    fun albumKey(song: SongEntity): String =
        "${song.album.lowercase()}|${song.artist.lowercase()}"

    fun albums(songs: List<SongEntity>): List<AlbumGroup> =
        songs.groupBy { albumKey(it) }
            .map { (key, list) ->
                val sorted = list.sortedWith(
                    compareBy({ it.discNumber }, { it.trackNumber }, { it.title.lowercase() })
                )
                AlbumGroup(
                    key = key,
                    album = sorted.first().album,
                    artist = sorted.first().artist,
                    year = sorted.firstOrNull { it.year > 0 }?.year ?: 0,
                    songs = sorted,
                )
            }
            .sortedBy { it.album.lowercase() }

    fun artists(songs: List<SongEntity>): List<ArtistGroup> =
        songs.groupBy { it.artist.lowercase() }
            .map { (_, list) -> ArtistGroup(artist = list.first().artist, songs = list) }
            .sortedBy { it.artist.lowercase() }

    fun folders(songs: List<SongEntity>): List<FolderGroup> =
        folderGroups(songs.map { it.path.substringBeforeLast('/', "") })

    fun folderGroups(paths: List<String>): List<FolderGroup> =
        paths
            .filter { it.isNotEmpty() }
            .distinct()
            .map { path ->
                FolderGroup(
                    path = path,
                    name = path.substringAfterLast('/').ifBlank { null },
                    songs = emptyList(),
                )
            }
            .sortedBy { it.path.lowercase() }

    fun sortSongs(
        songs: List<SongEntity>,
        field: SongSort,
        ascending: Boolean,
    ): List<SongEntity> {
        val comparator: Comparator<SongEntity> = when (field) {
            SongSort.TITLE -> compareBy { it.title.lowercase() }
            SongSort.ARTIST -> compareBy({ it.artist.lowercase() }, { it.title.lowercase() })
            SongSort.ALBUM -> compareBy(
                { it.album.lowercase() },
                { it.discNumber },
                { it.trackNumber },
            )
            SongSort.DURATION -> compareBy { it.duration }
            SongSort.DATE_ADDED -> compareBy { it.dateAdded }
            SongSort.YEAR -> compareBy({ it.year }, { it.title.lowercase() })
            SongSort.PLAY_COUNT -> compareBy({ it.playCount }, { it.title.lowercase() })
        }
        return if (ascending) songs.sortedWith(comparator)
        else songs.sortedWith(comparator.reversed())
    }
}
