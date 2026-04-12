package io.github.jwyoon1220.stellastep.io

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.github.jwyoon1220.stellastep.util.ErrorHandler
import java.io.File
import kotlin.system.exitProcess

/** Loads the global song pack index (assets/sheets.json). */
object Parser {
    fun loadSongPack(jsonFile: File): SongPack {
        return try {
            val gson = Gson()
            val json = jsonFile.readText(Charsets.UTF_8)
            gson.fromJson(json, SongPack::class.java)
        } catch (e: JsonSyntaxException) {
            ErrorHandler.handleError(e, true, 1, "Song pack parse error")
            throw e
        }
    }
}

data class SongPack(
    val version: Int,
    val songs: List<Song>
) {
    fun validateDuplicates(): List<String> {
        val errors = mutableListOf<String>()
        val duplicatedSongIds = songs.groupBy { it.id }.filter { it.value.size > 1 }.keys
        duplicatedSongIds.forEach { errors.add("$it: duplicate song ID") }
        songs.forEach { song ->
            song.charts.groupBy { it.difficulty }.filter { it.value.size > 1 }.keys
                .forEach { errors.add("'${song.id}': duplicate difficulty: $it") }
            song.charts.groupBy { it.file }.filter { it.value.size > 1 }.keys
                .forEach { errors.add("'${song.id}': duplicate chart file: $it") }
        }
        return errors
    }
}

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val bpm: Int,
    val offset: Int,
    val video: String = "",
    val jacket: String = "",
    val preview: Preview = Preview(0, 0),
    val charts: List<SongChart> = emptyList()
)

data class Preview(val start: Int, val length: Int)

data class SongChart(
    val difficulty: String,
    val level: Int,
    val file: String
)