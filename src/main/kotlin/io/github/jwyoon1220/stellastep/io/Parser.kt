package io.github.jwyoon1220.stellastep.io

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.github.jwyoon1220.stellastep.util.ErrorHandler
import java.io.File
import javax.swing.JOptionPane
import kotlin.system.exitProcess

object Parser {
    fun loadSongPack(jsonFile: File): SongPack {
        try {
            val gson = Gson()
            val json = jsonFile.readText(Charsets.UTF_8)
            return gson.fromJson(json, SongPack::class.java)
        } catch (e: JsonSyntaxException) {
            ErrorHandler.handleError(e, true, 1, "곡 정보 파싱 오류")
            throw e // 이 라인은 실제로 도달하지 않지만 컴파일러 경고를 피하기 위해 필요합니다.
        }
    }
}
data class SongPack(
    val version: Int,
    val songs: List<Song>
) {

    fun validateDuplicates(): List<String> {
        val errors = mutableListOf<String>()

        // 1. Song ID 중복 체크
        val duplicatedSongIds = songs
            .groupBy { it.id }
            .filter { it.value.size > 1 }
            .keys

        duplicatedSongIds.forEach {
            errors.add("${it}: 노래 ID가 중복되었습니다.")
        }

        // 2. 각 Song 내부 difficulty 중복 체크
        songs.forEach { song ->
            val duplicatedDifficulties = song.charts
                .groupBy { it.difficulty }
                .filter { it.value.size > 1 }
                .keys

            duplicatedDifficulties.forEach {
                errors.add("'${song.id}'의 중복된 난이도가 있습니다.: $it")
            }

            // 3. 차트 파일명 중복 체크
            val duplicatedChartFiles = song.charts
                .groupBy { it.file }
                .filter { it.value.size > 1 }
                .keys

            duplicatedChartFiles.forEach {
                errors.add("'${song.id}'에 중복된 채보 파일이 있습니다.: $it")
            }
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
    val video: String,
    val jacket: String,
    val preview: Preview,
    val charts: List<Chart>
)

data class Preview(
    val start: Int,
    val length: Int
)

data class Chart(
    val difficulty: String,
    val level: Int,
    val file: String
)